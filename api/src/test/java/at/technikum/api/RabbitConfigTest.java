package at.technikum.api;

import at.technikum.api.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();
    private final Declarables topology = config.topology(config.documentEventsExchange());

    @Test
    void declares_durable_queues_for_ocr_jobs_ocr_results_and_summaries() {
        List<Queue> queues = topology.getDeclarablesByType(Queue.class);

        assertEquals(List.of("ocr.document.events", "api.ocr.events", "api.genai.events"),
                queues.stream().map(Queue::getName).toList());
        assertTrue(queues.stream().allMatch(Queue::isDurable));
    }

    @Test
    void binds_every_queue_to_its_fanout_exchange() {
        List<String> bindings = topology.getDeclarablesByType(Binding.class).stream()
                .map(binding -> binding.getExchange() + " -> " + binding.getDestination())
                .toList();

        assertEquals(List.of(
                "document.events -> ocr.document.events",
                "ocr.events -> api.ocr.events",
                "genai.events -> api.genai.events"), bindings);
    }

    @Test
    void declares_the_result_and_summary_exchanges_next_to_the_upload_exchange() {
        List<String> exchanges = topology.getDeclarablesByType(FanoutExchange.class).stream()
                .map(FanoutExchange::getName)
                .toList();

        assertEquals(List.of("ocr.events", "genai.events"), exchanges); // document.events ist ein eigenes Bean
        assertEquals("document.events", config.documentEventsExchange().getName());
    }
}
