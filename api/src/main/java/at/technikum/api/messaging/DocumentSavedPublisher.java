package at.technikum.api.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RabbitPublication(exchange = "document.events")
public class DocumentSavedPublisher extends EventPublisher<DocumentSavedEvent>{
    protected DocumentSavedPublisher(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(DocumentSavedEvent event) {
        super.publish(event);
    }
}
