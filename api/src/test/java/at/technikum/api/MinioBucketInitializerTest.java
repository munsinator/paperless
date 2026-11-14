package at.technikum.api;

import at.technikum.api.config.MinioBucketInitializer;
import at.technikum.api.config.MinioProperties;
import at.technikum.api.exceptions.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioBucketInitializerTest {

    private static final MinioProperties PROPERTIES =
            new MinioProperties("http://localhost:9000", "access", "secret", "documents");

    @Mock
    private MinioClient minioClient;

    private MinioBucketInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new MinioBucketInitializer(minioClient, PROPERTIES);
    }

    @Test
    void run_does_not_create_bucket_when_it_already_exists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        initializer.run(new DefaultApplicationArguments());

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void run_creates_bucket_when_it_is_missing() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        initializer.run(new DefaultApplicationArguments());

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void run_wraps_minio_error_in_storage_exception() throws Exception {
        MinioException error = mock(MinioException.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(error);

        assertThrows(StorageException.class, () -> initializer.run(new DefaultApplicationArguments()));
    }

    @Test
    void run_wraps_connection_failure_in_storage_exception() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new IllegalStateException("connection refused"));

        assertThrows(StorageException.class, () -> initializer.run(new DefaultApplicationArguments()));
    }
}
