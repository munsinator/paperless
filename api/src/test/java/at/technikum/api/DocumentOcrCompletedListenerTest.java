package at.technikum.api;

import at.technikum.api.messaging.*;
import at.technikum.api.repository.DocumentRepository;
import at.technikum.api.service.SearchService;
import at.technikum.api.exceptions.SearchException;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DocumentOcrCompletedListenerTest {
    private final DocumentRepository repository = mock(DocumentRepository.class);
    private final SearchService search = mock(SearchService.class);
    private final DocumentOcrCompletedListener listener = new DocumentOcrCompletedListener(repository, search);
    private final UUID id = UUID.randomUUID();

    private DocumentOcrCompletedEvent event() {
        var event = new DocumentOcrCompletedEvent();
        event.setDocumentId(id.toString());
        event.setContent("Hello World");
        return event;
    }

    @Test
    void writes_text_to_search_without_writing_to_database() {
        when(repository.existsById(id)).thenReturn(true);
        listener.consume(event());
        verify(search).saveText(id, "Hello World");
        verify(repository).existsById(id);
        verifyNoMoreInteractions(repository, search);
    }

    @Test
    void ignores_result_for_deleted_document() {
        listener.consume(event());
        verifyNoInteractions(search);
    }

    @Test
    void propagates_search_failure_so_rabbit_can_retry() {
        when(repository.existsById(id)).thenReturn(true);
        doThrow(new SearchException("unavailable", new java.io.IOException())).when(search).saveText(id, "Hello World");
        assertThrows(SearchException.class, () -> listener.consume(event()));
    }
}
