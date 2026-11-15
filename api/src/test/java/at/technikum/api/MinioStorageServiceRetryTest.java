package at.technikum.api;

import at.technikum.api.config.MinioProperties;
import at.technikum.api.exceptions.StorageException;
import at.technikum.api.exceptions.StoredFileNotFoundException;
import at.technikum.api.service.MinioStorageService;
import at.technikum.api.service.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Das Retry wirkt nur ueber den Spring-Proxy, daher ein minimaler Kontext statt "new MinioStorageService(...)"
@SpringJUnitConfig(MinioStorageServiceRetryTest.TestConfig.class)
class MinioStorageServiceRetryTest {

    @Configuration
    /**
     * @EnableResilientMethods
     * Aktiviert die Resilience-Funktionen von Spring.
     * Damit kann Spring die Retry-Annotationen auf deinen Beans berücksichtigen und die entsprechenden Proxies erstellen.
     */
    @EnableResilientMethods
    static class TestConfig {

        @Bean
        MinioClient minioClient() {
            return mock(MinioClient.class);
        }

        @Bean
        StorageService storageService(MinioClient minioClient) {
            return new MinioStorageService(minioClient,
                    new MinioProperties("http://localhost:9000", "access", "secret", "documents"));
        }
    }

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private StorageService storageService;

    @BeforeEach
    void resetMock() {
        reset(minioClient);
    }

    @Test
    void store_succeeds_when_a_later_attempt_works() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new MinioException("first failure"))
                .thenThrow(new MinioException("second failure"))
                .thenReturn((ObjectWriteResponse) null);

        assertDoesNotThrow(() -> storageService.store(UUID.randomUUID(), new byte[]{1}));

        verify(minioClient, times(3)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void store_gives_up_after_three_attempts() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new MinioException("storage down"));

        assertThrows(StorageException.class, () -> storageService.store(UUID.randomUUID(), new byte[]{1}));

        verify(minioClient, times(3)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void load_does_not_retry_when_file_does_not_exist() throws Exception {
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        ErrorResponseException noSuchKey = mock(ErrorResponseException.class);
        when(noSuchKey.errorResponse()).thenReturn(errorResponse);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(noSuchKey);

        assertThrows(StoredFileNotFoundException.class, () -> storageService.load(UUID.randomUUID()));

        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }
}
