package at.technikum.api;

import at.technikum.api.config.RabbitConfig;
import at.technikum.api.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RabbitStartupTest {
    @Test
    void initializesBrokerBeforeStartupCompletes() throws Exception {
        var admin = mock(RabbitAdmin.class);
        new RabbitConfig().initializeRabbitTopology(admin).run(new DefaultApplicationArguments());
        verify(admin).initialize();
    }

    @Test
    void brokerFailurePreventsSuccessfulStartup() {
        var admin = mock(RabbitAdmin.class);
        doThrow(new AmqpConnectException(new java.net.ConnectException("offline")))
                .when(admin).initialize();
        assertThrows(AmqpConnectException.class, () ->
                new RabbitConfig().initializeRabbitTopology(admin).run(new DefaultApplicationArguments()));
    }

    @Test
    void healthWaitsForApplicationRunners() {
        var availability = mock(ApplicationAvailability.class);
        var controller = new HealthController(availability);
        when(availability.getReadinessState()).thenReturn(ReadinessState.REFUSING_TRAFFIC);
        assertEquals(503, controller.health().getStatusCode().value());
        when(availability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
        assertEquals(200, controller.health().getStatusCode().value());
    }
}
