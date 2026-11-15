package at.technikum.api.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public org.springframework.amqp.core.Declarables topology() {
        var jobs = new org.springframework.amqp.core.FanoutExchange("document.events");
        var results = new org.springframework.amqp.core.FanoutExchange("ocr.events");
        var jobQueue = new org.springframework.amqp.core.Queue("ocr.document.events", true);
        var resultQueue = new org.springframework.amqp.core.Queue("api.ocr.events", true);
        return new org.springframework.amqp.core.Declarables(jobs, results, jobQueue, resultQueue,
                org.springframework.amqp.core.BindingBuilder.bind(jobQueue).to(jobs),
                org.springframework.amqp.core.BindingBuilder.bind(resultQueue).to(results));
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
