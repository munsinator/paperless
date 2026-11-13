package at.technikum.api;

import at.technikum.api.config.MinioProperties;
import at.technikum.api.exceptions.StorageException;
import at.technikum.api.exceptions.StoredFileNotFoundException;
import at.technikum.api.service.MinioStorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    // MinioStorageService benötigt Informationen beim Erstellen
    private static final MinioProperties PROPERTIES =
            new MinioProperties("http://localhost:9000", "access", "secret", "documents");

    @Mock
    private MinioClient minioClient;

    @Test
    void store_uploads_pdf_with_document_id_as_key_and_pdf_content_type() throws Exception {
        UUID id = UUID.randomUUID();
        MinioStorageService storageService = new MinioStorageService(minioClient, PROPERTIES);

        storageService.store(id, "%PDF-1.7".getBytes());

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class); // ArgumentCaptor ist eine Funktionalität von Mockito. Damit können wir später anschauen, welche Parameter bei einem Methodenaufruf tatsächlich übergeben wurden.
        verify(minioClient).putObject(captor.capture()); // captor.capture(): Fange die Argumente auf, die bei diesem Aufruf an putObject() übergeben wurden.
        assertEquals("documents", captor.getValue().bucket());
        assertEquals(id.toString(), captor.getValue().object());
        assertEquals("application/pdf", captor.getValue().contentType().toString());
    }

    @Test
    void store_throws_storage_exception_when_upload_fails() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new MinioException("upload failed"));
        MinioStorageService storageService = new MinioStorageService(minioClient, PROPERTIES);

        assertThrows(StorageException.class, () -> storageService.store(UUID.randomUUID(), new byte[]{1}));
    }

    @Test
    void store_throws_storage_exception_when_storage_is_unreachable() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new IllegalStateException(new java.net.ConnectException("connection refused")));
        MinioStorageService storageService = new MinioStorageService(minioClient, PROPERTIES);

        assertThrows(StorageException.class, () -> storageService.store(UUID.randomUUID(), new byte[]{1}));
    }

    @Test
    void delete_removes_object_with_document_id_as_key() throws Exception {
        UUID id = UUID.randomUUID();
        MinioStorageService storageService = new MinioStorageService(minioClient, PROPERTIES);

        storageService.delete(id);

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        assertEquals("documents", captor.getValue().bucket());
        assertEquals(id.toString(), captor.getValue().object());
    }

    @Test
    void delete_throws_storage_exception_when_removal_fails() throws Exception {
        doThrow(new MinioException("removal failed")).when(minioClient).removeObject(any(RemoveObjectArgs.class));
        MinioStorageService storageService = new MinioStorageService(minioClient, PROPERTIES);

        assertThrows(StorageException.class, () -> storageService.delete(UUID.randomUUID()));
    }

    @Test
    void load_throws_not_found_when_object_does_not_exist() throws Exception {
        ErrorResponseException noSuchKey = errorResponseException("NoSuchKey");
        when(minioClient.getObject(org.mockito.ArgumentMatchers.any(GetObjectArgs.class))).thenThrow(noSuchKey);
        MinioStorageService storageService = new MinioStorageService(minioClient, PROPERTIES);

        assertThrows(StoredFileNotFoundException.class, () -> storageService.load(UUID.randomUUID()));
    }

    @Test
    void load_throws_storage_exception_for_other_errors() throws Exception {
        ErrorResponseException accessDenied = errorResponseException("AccessDenied");
        when(minioClient.getObject(org.mockito.ArgumentMatchers.any(GetObjectArgs.class))).thenThrow(accessDenied);
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
