package at.technikum.api;

import at.technikum.api.exceptions.SearchException;
import at.technikum.api.service.DocumentDeletedEvent;
import at.technikum.api.service.DocumentTitleChangedEvent;
import at.technikum.api.service.SearchIndexSyncListener;
import at.technikum.api.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchIndexSyncListenerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchIndexSyncListener listener;

    @Test
    void title_changed_saves_title_in_search_index() {
        UUID id = UUID.randomUUID();

        listener.onTitleChanged(new DocumentTitleChangedEvent(id, "Rechnung"));

        verify(searchService).saveTitle(id, "Rechnung");
    }

    @Test
    void title_changed_swallows_search_failure() {
        UUID id = UUID.randomUUID();
        doThrow(new SearchException("down", new IOException())).when(searchService).saveTitle(id, "Rechnung");

        assertDoesNotThrow(() -> listener.onTitleChanged(new DocumentTitleChangedEvent(id, "Rechnung")));
    }

    @Test
    void deleted_removes_document_from_search_index() {
        UUID id = UUID.randomUUID();

        listener.onDeleted(new DocumentDeletedEvent(id));

        verify(searchService).delete(id);
    }

    @Test
    void deleted_swallows_search_failure() {
        UUID id = UUID.randomUUID();
        doThrow(new SearchException("down", new IOException())).when(searchService).delete(id);

        assertDoesNotThrow(() -> listener.onDeleted(new DocumentDeletedEvent(id)));
    }
}

/**
 * @Mock = Ich erstelle eine Simulation einer Abhängigkeit.
 * @InjectMocks = Ich erstelle mein echtes Testobjekt und gebe ihm die simulierten Abhängigkeiten.
 */