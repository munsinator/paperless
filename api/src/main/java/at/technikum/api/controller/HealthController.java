package at.technikum.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.ResponseEntity;

@RestController
public class HealthController {
    private final ApplicationAvailability availability;

    public HealthController(ApplicationAvailability availability) {
        this.availability = availability;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC
                ? ResponseEntity.ok("ok")
                : ResponseEntity.status(503).body("starting");
    }
}
