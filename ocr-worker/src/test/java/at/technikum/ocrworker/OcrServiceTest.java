package at.technikum.ocrworker;

import at.technikum.ocrworker.messaging.DocumentOcrCompletedEvent;
import at.technikum.ocrworker.messaging.DocumentOcrCompletedPublisher;
import at.technikum.ocrworker.messaging.DocumentSavedEvent;
import at.technikum.ocrworker.services.MinioStorageService;
import at.technikum.ocrworker.services.OcrService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OcrServiceTest {
    @Test
    void downloadsPdfClosesStreamAndDeletesTemporaryFile() throws Exception {
        runProcessing(false);
    }

    @Test
    void deletesTemporaryFileAndDoesNotPublishWhenOcrFails() throws Exception {
        runProcessing(true);
    }

    private void runProcessing(boolean failOcr) throws Exception {
        var id = UUID.randomUUID();
        var storage = mock(MinioStorageService.class);
        var publisher = mock(DocumentOcrCompletedPublisher.class);
        var input = spy(new ByteArrayInputStream("pdf bytes".getBytes()));
        when(storage.load(id)).thenReturn(input);
        var downloaded = new AtomicReference<File>();
        try (var ocr = mockStatic(OcrService.class)) {
            ocr.when(() -> OcrService.extractText(any(File.class))).thenAnswer(invocation -> {
                File file = invocation.getArgument(0);
                downloaded.set(file);
                assertEquals("pdf bytes", Files.readString(file.toPath()));
                verify(input).close();
                if (failOcr) throw new IOException("OCR failed");
                return "recognized text";
            });
            var service = new OcrService(publisher, storage);
            var event = new DocumentSavedEvent(id.toString(), "document.pdf");
            if (failOcr) {
                assertThrows(IOException.class, () -> service.processDocument(event));
                verifyNoInteractions(publisher);
            } else {
                service.processDocument(event);
                var result = ArgumentCaptor.forClass(DocumentOcrCompletedEvent.class);
                verify(publisher).publish(result.capture());
                assertEquals(id.toString(), result.getValue().getDocumentId());
                assertEquals("document.pdf", result.getValue().getDocumentName());
                assertEquals("recognized text", result.getValue().getContent());
            }
        }
        assertNotNull(downloaded.get());
        assertFalse(Files.exists(downloaded.get().toPath()));
    }
}
