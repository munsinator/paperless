package at.technikum.api.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public org.springframework.amqp.core.FanoutExchange documentEventsExchange() {
        return new org.springframework.amqp.core.FanoutExchange("document.events");
    }

    @Bean
    public org.springframework.boot.ApplicationRunner initializeRabbitTopology(
            org.springframework.amqp.rabbit.core.RabbitAdmin rabbitAdmin) {
        return args -> rabbitAdmin.initialize();
    }

    @Bean
    public org.springframework.amqp.core.Declarables topology(
            org.springframework.amqp.core.FanoutExchange documentEventsExchange) {
        var jobs = documentEventsExchange;
        var results = new org.springframework.amqp.core.FanoutExchange("ocr.events");
        var jobQueue = new org.springframework.amqp.core.Queue("ocr.document.events", true);
        var resultQueue = new org.springframework.amqp.core.Queue("api.ocr.events", true);
        // Provision the durable job queue even without a worker so uploads are retained.
        return new org.springframework.amqp.core.Declarables(results, jobQueue, resultQueue,
                org.springframework.amqp.core.BindingBuilder.bind(jobQueue).to(jobs),
                org.springframework.amqp.core.BindingBuilder.bind(resultQueue).to(results));
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
