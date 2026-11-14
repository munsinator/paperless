package at.technikum.api.config;

// stellt den Client zur Verfügung, mit dem Java-Anwendungen auf einen MinIO-Server zugreifen können
import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    // CONNECT_TIMEOUT = 3 Sekunden: maximale Zeit, die der Client für den Verbindungsaufbau zu MinIO wartet
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    // TRANSFER_TIMEOUT = 30 Sekunden: maximale Zeit für die Übertragung der Daten
    private static final Duration TRANSFER_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
        // verbinden, schreiben, lesen
        client.setTimeout(CONNECT_TIMEOUT.toMillis(), TRANSFER_TIMEOUT.toMillis(), TRANSFER_TIMEOUT.toMillis());
        return client;
    }
}

/**
 * MinioClient ist das Objekt, über das die Anwendung später mit MinIO
 * kommunizieren kann, beispielsweise um PDFs hochzuladen,
 * herunterzuladen oder zu löschen.
 */