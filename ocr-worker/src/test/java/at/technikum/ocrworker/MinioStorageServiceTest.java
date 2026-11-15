package at.technikum.ocrworker;

import at.technikum.ocrworker.config.MinioProperties;
import at.technikum.ocrworker.services.MinioStorageService;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MinioStorageServiceTest {
    private final MinioClient client = mock(MinioClient.class);
    private final MinioStorageService storage = new MinioStorageService(client,
            new MinioProperties("http://localhost:9000", "access", "secret", "documents"));

    @Test
    void loadsFromConfiguredBucketUsingIdWithoutExtension() throws Exception {
        var id = UUID.randomUUID();
        var response = mock(GetObjectResponse.class);
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(response);
        assertSame(response, storage.load(id));
        var args = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(client).getObject(args.capture());
        assertEquals("documents", args.getValue().bucket());
        assertEquals(id.toString(), args.getValue().object());
    }

    @Test
    void propagatesDownloadFailureWithCause() throws Exception {
        var cause = new MinioException("download failed");
        when(client.getObject(any(GetObjectArgs.class))).thenThrow(cause);
        assertSame(cause, assertThrows(IOException.class, () -> storage.load(UUID.randomUUID())).getCause());
    }
}
