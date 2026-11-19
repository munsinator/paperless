package at.technikum.api.messaging;

import at.technikum.api.exceptions.DocumentNotFoundException;
import at.technikum.api.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentSummarizedListener {

    private final DocumentService documentService;

    @RabbitListener(queues = "api.genai.events")
    // DocumentSummarizedEvent enthält summary und documentId
    public void consume(DocumentSummarizedEvent event) {
        // ungültige Nachrichten werden nur geloggt und verworfen, ein Retry würde sie nicht reparieren
        if (event.getSummary() == null || event.getSummary().isBlank()) {
            log.warn("Ignoring summary event without summary, documentId={}", event.getDocumentId());
            return;
        }
        UUID documentId = parseDocumentId(event.getDocumentId());
        if (documentId == null) {
            return;
        }
        try {
            documentService.updateSummary(documentId, event.getSummary());
        } catch (DocumentNotFoundException exception) {
            log.warn("Ignoring summary for deleted documentId={}", documentId);
        }
    }

    private UUID parseDocumentId(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            log.warn("Ignoring summary event with invalid documentId={}", rawId);
            return null;
        }
    }
}
