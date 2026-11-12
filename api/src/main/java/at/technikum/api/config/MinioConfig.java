package at.technikum.api.config;

// stellt den Client zur Verfügung, mit dem Java-Anwendungen auf einen MinIO-Server zugreifen können
import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }
}

/**
 * MinioClient ist das Objekt, über das die Anwendung später mit MinIO
 * kommunizieren kann, beispielsweise um PDFs hochzuladen,
 * herunterzuladen oder zu löschen.
 */