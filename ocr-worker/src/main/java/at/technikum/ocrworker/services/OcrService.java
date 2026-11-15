package at.technikum.ocrworker.services;

import at.technikum.ocrworker.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {
    private final DocumentOcrCompletedPublisher publisher;
    private final MinioStorageService storageService;

    public static String extractText(File file) throws IOException {
        Tesseract tesseract = new Tesseract();
        String dataPath = System.getenv("TESSDATA_PREFIX");
        if (dataPath != null) tesseract.setDatapath(dataPath);
        tesseract.setLanguage("eng+deu");
        StringBuilder text = new StringBuilder();
        try (var pdf = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            for (int page = 0; page < pdf.getNumberOfPages(); page++) {
                var image = renderer.renderImageWithDPI(page, 200);
                try { text.append(tesseract.doOCR(image)).append("\n"); }
                finally { image.flush(); }
            }
        } catch (TesseractException exception) {
            throw new IOException("Tess4J could not extract text", exception);
        }
        return text.toString().trim();
    }

    public void processDocument(DocumentSavedEvent event) throws IOException {
        UUID id = UUID.fromString(event.documentId());
        String text;
        var temporaryFile = Files.createTempFile("ocr-" + id + "-", ".pdf");
        try {
            try (var input = storageService.load(id)) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
            text = extractText(temporaryFile.toFile());
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
        publisher.publish(new DocumentOcrCompletedEvent(id.toString(), event.documentName(), text));
        log.info("OCR completed for documentId={}", id);
    }
}
