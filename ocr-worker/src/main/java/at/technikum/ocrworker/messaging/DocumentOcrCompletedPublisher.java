package at.technikum.ocrworker.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RabbitPublication(exchange = "ocr.events")
public class DocumentOcrCompletedPublisher extends EventPublisher<DocumentOcrCompletedEvent>{
    protected DocumentOcrCompletedPublisher(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    public void publish(DocumentOcrCompletedEvent event) {
        log.info("Publishing document OCR completed event: {}", event.getDocumentId());
        super.publish(event);
    }
}
