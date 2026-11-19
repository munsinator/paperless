package at.technikum.api.messaging;

import at.technikum.api.repository.DocumentRepository;
import at.technikum.api.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentSummarizedListener {

    private final DocumentRepository repository;
    private final SearchService searchService;

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
        // sonst Inkonsistenz
        if (!repository.existsById(documentId)) {
            log.warn("Ignoring summary for deleted documentId={}", documentId);
            return;
        }
        searchService.saveSummary(documentId, event.getSummary());
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
