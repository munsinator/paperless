package at.technikum.api.messaging;

import at.technikum.api.exceptions.MessagePublishException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RabbitPublication(exchange = "document.events")
@Slf4j
public class DocumentSavedPublisher extends EventPublisher<DocumentSavedEvent>{
    protected DocumentSavedPublisher(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(DocumentSavedEvent event) {
        try {
            super.publish(event);
        } catch (MessagePublishException ex) {
            log.error(
                    "OCR job could not be sent for documentId={}",
                    event.getDocumentId(),
                    ex
            );
        }
    }
}
