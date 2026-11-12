package at.technikum.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

}

/*
ResilientMethods: Anwendung reagiert auf bestimmte Fehler
und z.B. Vorgang wiederholen (wenn MinIO nicht reagiert)
*/