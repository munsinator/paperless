package at.technikum.paperless.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Duenne Huelle um das bestehende Web-Frontend: zeigt nur die Seite unter
 * {@link BuildConfig#APP_URL}, die ganze Logik (Login, Suche, Upload) bleibt im Frontend.
 */
public class MainActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST = 1;

    // Das Frontend startet den Download mit einem unsichtbaren Link auf eine Blob-URL und
    // gibt die URL sofort wieder frei. Die WebView macht daraus keinen Download. Darum faengt
    // dieses Skript den Klick ab, liest den Blob und gibt ihn an die App weiter.
    private static final String DOWNLOAD_HOOK = """
            (function () {
              if (window.__paperlessDownloadHook) return;
              window.__paperlessDownloadHook = true;
              var originalClick = HTMLAnchorElement.prototype.click;
              HTMLAnchorElement.prototype.click = function () {
                if (this.download && this.href.indexOf('blob:') === 0) {
                  var name = this.download;
                  fetch(this.href).then(function (r) { return r.blob(); }).then(function (blob) {
                    var reader = new FileReader();
                    reader.onload = function () { AndroidDownload.save(reader.result, name); };
                    reader.readAsDataURL(blob);
                  });
                  return;
                }
                return originalClick.apply(this, arguments);
              };
            })();
            """;

    private WebView webView;
    private ValueCallback<Uri[]> pendingFileCallback;

    @SuppressLint("SetJavaScriptEnabled") // das Angular-Frontend braucht JavaScript
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        // localStorage: dort legt das Frontend das JWT ab
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        webView.addJavascriptInterface(new DownloadBridge(), "AndroidDownload");
        webView.setWebViewClient(new AppWebViewClient());
        webView.setWebChromeClient(new FileChooserChromeClient());

        if (savedInstanceState == null) {
            webView.loadUrl(BuildConfig.APP_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    @SuppressWarnings("deprecation") // der Zurueck-Knopf soll im Web-Verlauf zurueckgehen
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    @SuppressWarnings("deprecation") // ohne AndroidX gibt es keine Activity Result API
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || pendingFileCallback == null) {
            return;
        }
        // bei Abbruch liefert parseResult null, der Callback muss trotzdem aufgerufen werden
        pendingFileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        pendingFileCallback = null;
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    private String errorPage() {
        return "<html><body style='font-family:sans-serif;padding:24px;text-align:center'>"
                + "<h2>" + getString(R.string.error_title) + "</h2>"
                + "<p>" + getString(R.string.error_text) + "</p>"
                + "<p><a href='" + BuildConfig.APP_URL + "'>" + getString(R.string.error_retry) + "</a></p>"
                + "</body></html>";
    }

    private class AppWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri target = request.getUrl();
            String appHost = Uri.parse(BuildConfig.APP_URL).getHost();
            if (appHost != null && appHost.equals(target.getHost())) {
                return false; // eigene Seite bleibt in der WebView
            }
            // fremde Links gehoeren in den Browser, nicht in unsere WebView
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, target));
            } catch (ActivityNotFoundException ignored) {
                // keine App kann den Link oeffnen: nichts tun
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.evaluateJavascript(DOWNLOAD_HOOK, null);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                view.loadDataWithBaseURL(null, errorPage(), "text/html", "UTF-8", null);
            }
        }
    }

    /** Wird vom Skript oben aufgerufen (Hintergrund-Thread) und speichert die PDF im Download-Ordner. */
    private class DownloadBridge {

        @JavascriptInterface
        public void save(String dataUrl, String filename) {
            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, filename.replaceAll("[\\\\/:*?\"<>|]", "_"));
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");

            Uri file = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            try (OutputStream out = file == null ? null : resolver.openOutputStream(file)) {
                if (out == null) {
                    throw new IOException("Datei konnte nicht angelegt werden");
                }
                // dataUrl sieht aus wie "data:application/pdf;base64,JVBERi0..."
                out.write(Base64.decode(dataUrl.substring(dataUrl.indexOf(',') + 1), Base64.DEFAULT));
            } catch (IOException | IllegalArgumentException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.download_failed, Toast.LENGTH_LONG).show());
                return;
            }
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, R.string.download_saved, Toast.LENGTH_LONG).show();
                openPdf(file);
            });
        }
    }

    private void openPdf(Uri file) {
        Intent view = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(file, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(view);
        } catch (ActivityNotFoundException ignored) {
            // keine PDF-App installiert: die Datei liegt trotzdem im Download-Ordner
        }
    }

    private class FileChooserChromeClient extends WebChromeClient {

        @Override
        @SuppressWarnings("deprecation")
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (pendingFileCallback != null) {
                pendingFileCallback.onReceiveValue(null); // alten Dialog abschliessen
            }
            pendingFileCallback = callback;
            try {
                startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
            } catch (ActivityNotFoundException e) {
                pendingFileCallback = null;
                return false;
            }
            return true;
        }
    }
}
