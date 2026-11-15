package at.technikum.ocrworker.services;

import at.technikum.ocrworker.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioStorageService {
    private final MinioClient minioClient;
    private final MinioProperties properties;

    // The caller owns and must close the response stream.
    public InputStream load(UUID documentId) throws IOException {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(documentId.toString())
                    .build());
        } catch (MinioException | IllegalStateException exception) {
            throw new IOException("Could not download PDF for document " + documentId, exception);
        }
    }
}
