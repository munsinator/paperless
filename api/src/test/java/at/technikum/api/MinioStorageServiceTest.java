package at.technikum.api;

import at.technikum.api.config.MinioProperties;
import at.technikum.api.exceptions.StorageException;
import at.technikum.api.exceptions.StoredFileNotFoundException;
import at.technikum.api.service.MinioStorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    // MinioStorageService benötigt Informationen beim Erstellen
    private static final MinioProperties PROPERTIES =
            new MinioProperties("http://localhost:9000", "access", "secret", "documents");

    @Mock
    private MinioClient minioClient;

    @Test
    void load_throws_not_found_when_object_does_not_exist() throws Exception {
        ErrorResponseException noSuchKey = errorResponseException("NoSuchKey");
        when(minioClient.getObject(org.mockito.ArgumentMatchers.any(GetObjectArgs.class))).thenThrow(noSuchKey);
        MinioStorageService storageService = new MinioStorageService(minioClient, PROPERTIES);

        assertThrows(StoredFileNotFoundException.class, () -> storageService.load(UUID.randomUUID()));
    }

    @Test
    void load_throws_storage_exception_for_other_errors() throws Exception {
        when(minioClient.getObject(org.mockito.ArgumentMatchers.any(GetObjectArgs.class)))
                .thenThrow(errorResponseException("AccessDenied"));
        MinioStorageService storageService = new MinioStorageService(minioClient, PROPERTIES);

        assertThrows(StorageException.class, () -> storageService.load(UUID.randomUUID()));
    }

    private ErrorResponseException errorResponseException(String code) {
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn(code); // Wenn jemand die Methode errorResponse.code() aufruft, soll der String zurückgegeben werden, den man der Hilfsmethode übergeben hat
        ErrorResponseException exception = mock(ErrorResponseException.class);
        when(exception.errorResponse()).thenReturn(errorResponse);
        return exception;
    }
}
