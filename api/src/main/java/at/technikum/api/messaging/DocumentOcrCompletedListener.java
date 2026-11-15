package at.technikum.api.messaging;

import at.technikum.api.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentOcrCompletedListener {
    private final DocumentRepository repository;
    @RabbitListener(queues = "api.ocr.events")
    @Transactional
    public void consume(DocumentOcrCompletedEvent event) {
        repository.findById(UUID.fromString(event.getDocumentId())).ifPresentOrElse(document -> {
            document.setOcrText(event.getContent());
            repository.save(document);
            log.info("Saved OCR text for documentId={}", event.getDocumentId());
        }, () -> log.warn("Ignoring OCR result for deleted documentId={}", event.getDocumentId()));
    }
}
