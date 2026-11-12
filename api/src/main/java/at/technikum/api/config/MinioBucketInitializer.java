package at.technikum.api.config;

import at.technikum.api.exceptions.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        String bucket = properties.bucket();
        try {
            if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                log.info("Storage bucket '{}' is ready", bucket);
                return;
            }
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created storage bucket '{}'", bucket);
        } catch (MinioException ex) {
            throw new StorageException("Could not prepare storage bucket '" + bucket + "'", ex);
        }
    }
}

/**
 * run: Spring Boot ruft diese Methode automatisch auf, sobald der Application Context vorbereitet wurde
 */