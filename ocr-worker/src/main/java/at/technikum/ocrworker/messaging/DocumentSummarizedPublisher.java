package at.technikum.ocrworker.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocumentSummarizedPublisher extends EventPublisher<DocumentSummarizedEvent>{
    protected DocumentSummarizedPublisher(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }
}
