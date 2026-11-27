# Paperless mobile app

This is the Android app for Sprint 5 (requirement 7, "generate a smartphone app using AI coding tools"). It is a WebView around our Angular frontend. The app has no screens of its own.

## Why a WebView

The assignment only asks for an app that is available and working. We already have login, search, upload and the summary view in the web frontend, and they are tested there. Building them a second time in Flutter or Kotlin would give us twice the code and nothing new to show.

A WebView also saves us the API work. The frontend calls `/api` on its own address and nginx forwards it, so the app needs no CORS settings, no HTTP client and no token handling. The JWT stays in the page's `localStorage`, exactly like in the browser.

The app is plain Java with the Android framework classes. We did not add AndroidX or any other library. That keeps the build short and leaves little that can break.

## Spec

We wrote this before generating anything (course slide "Lightweight Spec Template"). The AI tool got it as its input.

**Goal**
Open the Paperless web frontend as an app on an Android phone, so a user can log in, search, upload a PDF and read a document with its summary.

**Inputs and outputs**
The only input is the frontend URL, set at build time with the Gradle property `appUrl`. The default is `http://10.0.2.2/`, which is how the emulator reaches the host computer. The output is one full-screen view of that page.

**Constraints**
- No own UI logic, no own API calls, no second login.
- JavaScript and DOM storage are on, because the frontend keeps its token in `localStorage`.
- The PDF upload has to work, so the WebView must open the phone's file picker.
- The back button walks back through the page history. On the first page it closes the app.
- The "Herunterladen" button must work. The frontend starts the download with a hidden blob link, which a WebView ignores, so the app catches that click, saves the PDF to the Downloads folder and opens it in a PDF app if one is installed.
- Links to other hosts open in the phone's browser.
- If the page can't be loaded, the app shows a short German message with a retry link. A blank screen is not acceptable.
- Rotating the phone must not reload the page.
- Web content gets no access to local files. No secrets in the code. No new libraries.
- Plain HTTP is allowed because our dev stack has no TLS. A real deployment would need HTTPS.

**Known limitation**
The PDF preview on the detail page stays empty in the app. The frontend shows it in an `<iframe>`, and the Android WebView has no PDF viewer. The download button is the way to read the file on the phone. A real preview would need PDF.js in the frontend, which we left out on purpose.

**Acceptance criteria**
- Given the stack is running, when I start the app, then I see the login page of the web frontend.
- Given valid credentials, when I log in, then I see my documents.
- Given a search term, then the list matches the web search, in the same order.
- Given a PDF on the phone, when I upload it, then it shows up in the list.
- Given a document, when I open its AI tab, then I see the summary or the "not available yet" text.
- Given the server is stopped, when I open the app, then I see "Keine Verbindung" and can retry.
- Given a document, when I tap "Herunterladen", then the PDF is saved in the Downloads folder and I see a short message.
- Given a link to another host, when I tap it, then it opens in the browser.

**Boundaries**
- Always: read the URL from the build config.
- Ask first: new permissions, new dependencies.
- Never: rebuild frontend features natively, store credentials in the app, log tokens.

## Prompt and tool

Tool: Claude Code (in VS Code). Model: Claude Sonnet 5.5, model ID `claude-sonnet-5-5`.

This is the prompt, written from the spec above. It was entered in the repository on the branch `feature/mobile-app`, so the tool could read the spec and the frontend code.

```text
Create an Android app in the folder mobile/ from the spec in mobile/README.md.
It is a WebView around our existing Angular frontend, with no UI of its own.
Use Java and plain Android framework classes (no AndroidX, no Kotlin), Gradle
Kotlin DSL, minSdk 29. The URL comes from the Gradle property appUrl (default
http://10.0.2.2/) as BuildConfig.APP_URL.
JavaScript and DOM storage on, file chooser for the PDF upload, back button uses
the web history, other hosts open in the browser, a German error page with a retry
link when the main page fails, the download button saves the PDF to Downloads, no reload on rotation, no file access for web
content, nothing beyond that.
Pick Gradle and AGP versions that fit the installed Android Studio, and make sure
`gradle assembleDebug` passes.
```

## Build and run

You need the stack running (`docker compose up --build` in the repository root, web UI on port 80) and Android Studio with an emulator.

In Android Studio:
1. Open the `mobile/` folder and wait for the Gradle sync.
2. Choose an emulator, for example `Pixel_4_API_33`, and press Run.

The default `appUrl` already points to the host, so nothing needs to be configured for the emulator.

On the command line:

```powershell
cd mobile
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The APK ends up in `app/build/outputs/apk/debug/`. There is no `gradle-wrapper.jar` in the repository. Android Studio downloads Gradle 9.2.1 on the first sync, and on the command line any Gradle from 9.1 upwards works.

For a real phone, build with the LAN address of your computer and make sure the firewall lets port 80 through:

```powershell
gradle assembleDebug -PappUrl=http://192.168.0.10/
```

## Test checklist

`gradle assembleDebug` passes with Gradle 9.2.1, AGP 9.0.1 and `compileSdk 34`. The boxes below are for the manual test in the emulator and are still open.

- [ ] App starts and shows the login page
- [ ] Register and log in
- [ ] Search gives the same results as the web UI
- [ ] Upload `hello-world.pdf` (file picker opens, document appears)
- [ ] "Herunterladen" saves the PDF to Downloads
- [ ] Summary shows up on the detail page once the GenAI worker has run
- [ ] Server stopped: error page and retry work
- [ ] Back button and rotation behave as in the spec

## What we had to fix by hand

- The first version of the app did not handle the download button. The AI had to add a small script that catches the hidden blob link, plus a bridge that saves the file with `MediaStore`. That raised `minSdk` from 26 to 29.
- The PDF preview does not work in a WebView. We looked at three options: accept it, add PDF.js to the frontend, or hide the empty preview in the app. We accepted it, because PDF.js would change the web version for everyone and the preview is not part of the acceptance criteria. It is documented under "Known limitation" above.

Anything else found in the emulator test goes here.
