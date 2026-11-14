package at.technikum.api.service;

import at.technikum.api.config.MinioProperties;
import at.technikum.api.exceptions.StorageException;
import at.technikum.api.exceptions.StoredFileNotFoundException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
// bei StorageException wird erneut versucht
@Retryable(includes = StorageException.class, maxRetries = 2, delay = 500)
public class MinioStorageService implements StorageService {

    private static final String NO_SUCH_KEY = "NoSuchKey";

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public void store(UUID documentId, byte[] content) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(documentId.toString())
                    .data(content, content.length)
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .build());
            log.info("Stored PDF for documentId={} ({} bytes)", documentId, content.length);
        } catch (MinioException | IllegalStateException ex) { // MinIO-Client wirft bei Verbindungsfehlern eine IllegalStateException
            log.warn("Storing PDF failed for documentId={}: {}", documentId, ex.getMessage());
            throw new StorageException("Could not store PDF for document " + documentId, ex);
        }
    }

    @Override
    public InputStream load(UUID documentId) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(documentId.toString())
                    .build());
        } catch (ErrorResponseException ex) {
            if (NO_SUCH_KEY.equals(ex.errorResponse().code())) {
                log.debug("No stored PDF for documentId={}", documentId);
                throw new StoredFileNotFoundException(documentId);
            }
            log.warn("Loading PDF failed for documentId={}: {}", documentId, ex.getMessage());
            throw new StorageException("Could not load PDF for document " + documentId, ex);
        } catch (MinioException | IllegalStateException ex) {
            log.warn("Loading PDF failed for documentId={}: {}", documentId, ex.getMessage());
            throw new StorageException("Could not load PDF for document " + documentId, ex);
        }
    }

    @Override
    public void delete(UUID documentId) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(documentId.toString())
                    .build());
            log.info("Deleted PDF for documentId={}", documentId);
        } catch (MinioException | IllegalStateException ex) {
            log.warn("Deleting PDF failed for documentId={}: {}", documentId, ex.getMessage());
            throw new StorageException("Could not delete PDF for document " + documentId, ex);
        }
    }
}
