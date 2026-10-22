package at.technikum.ocrworker.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class DocumentSavedListener extends EventListener<DocumentSavedEvent>{
    public DocumentSavedListener() {}

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue("ocr.document.events"),
                    exchange = @Exchange(
                            value = "document.events",
                            type = ExchangeTypes.FANOUT
                    )
    ))

    @Override
    public void consume(DocumentSavedEvent event) {
        log.info("OCR-Auftrag empfangen: ID={}, Dokument={}",
                event.getDocumentId(), event.getDocumentName());
    }
}
