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
public class DocumentOcrCompletedListener {
    private final DocumentRepository repository;
    private final SearchService searchService;
    @RabbitListener(queues = "api.ocr.events")
    public void consume(DocumentOcrCompletedEvent event) {
        UUID documentId = UUID.fromString(event.getDocumentId());
        if (!repository.existsById(documentId)) {
            log.warn("Ignoring OCR result for deleted documentId={}", documentId);
            return;
        }
        searchService.saveText(documentId, event.getContent());
    }
}
