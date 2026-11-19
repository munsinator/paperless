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
            org.springframework.amqp.core.AmqpAdmin rabbitAdmin) {
        return args -> rabbitAdmin.initialize();
    }

    @Bean
    public org.springframework.amqp.core.Declarables topology(
            org.springframework.amqp.core.FanoutExchange documentEventsExchange) {
        var jobs = documentEventsExchange;
        var results = new org.springframework.amqp.core.FanoutExchange("ocr.events");
        var jobQueue = new org.springframework.amqp.core.Queue("ocr.document.events", true);
        var resultQueue = new org.springframework.amqp.core.Queue("api.ocr.events", true);
        var summaries = new org.springframework.amqp.core.FanoutExchange("genai.events"); // verteilt an Queues, muss nicht wissen wer empfangen möchte
        var summaryQueue = new org.springframework.amqp.core.Queue("api.genai.events", true); // Nachrichtenwarteschlange
        // Provision the durable job queue even without a worker so uploads are retained.
        return new org.springframework.amqp.core.Declarables(results, jobQueue, resultQueue, summaries, summaryQueue,
                org.springframework.amqp.core.BindingBuilder.bind(jobQueue).to(jobs),
                org.springframework.amqp.core.BindingBuilder.bind(resultQueue).to(results),
                org.springframework.amqp.core.BindingBuilder.bind(summaryQueue).to(summaries));
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
