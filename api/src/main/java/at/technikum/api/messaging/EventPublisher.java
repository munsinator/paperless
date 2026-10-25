package at.technikum.api.messaging;

import at.technikum.api.exceptions.MessagePublishException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.annotation.AnnotationUtils;

@Slf4j
public abstract class EventPublisher<T> {
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    protected EventPublisher (RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;

        RabbitPublication publication = AnnotationUtils.findAnnotation(getClass(), RabbitPublication.class);
        if (publication == null) {
            throw new IllegalStateException(
                    getClass().getName() + " must be annotated with @" + RabbitPublication.class.getSimpleName());
        }
        this.exchange = publication.exchange();
    }

    public void publish(T event) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                rabbitTemplate.convertAndSend(exchange, "", event);
                log.info("Sent message to exchange: {}", exchange);
                return;

            } catch (AmqpException ex) {
                if (attempt == maxAttempts) {
                    throw new MessagePublishException(
                            "Failed to publish after " + maxAttempts + " attempts",
                            ex
                    );
                }

                log.warn("Publish attempt {}/{} to {} failed; retrying: {}",
                        attempt, maxAttempts, exchange, ex.getMessage());
                try {
                    //Short break in between retries
                    Thread.sleep(400);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new MessagePublishException(
                            "Publishing interrupted while waiting for retry", interrupted);
                }
            }
        }

    }
}
