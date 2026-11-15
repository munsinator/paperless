package at.technikum.ocrworker.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public org.springframework.amqp.core.Declarables topology() {
        var results = new org.springframework.amqp.core.FanoutExchange("ocr.events");
        var resultQueue = new org.springframework.amqp.core.Queue("api.ocr.events", true);
        // The job listener declares its queue and binding; the API owns document.events.
        return new org.springframework.amqp.core.Declarables(results, resultQueue,
                org.springframework.amqp.core.BindingBuilder.bind(resultQueue).to(results));
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
