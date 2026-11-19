package at.technikum.api;

import at.technikum.api.messaging.DocumentSummarizedEvent;
import at.technikum.api.messaging.DocumentSummarizedListener;
import at.technikum.api.repository.DocumentRepository;
import at.technikum.api.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentSummarizedListenerTest {

    private final DocumentRepository repository = mock(DocumentRepository.class);
    private final SearchService searchService = mock(SearchService.class);
    private final DocumentSummarizedListener listener = new DocumentSummarizedListener(repository, searchService);
    private final UUID id = UUID.randomUUID();

    private DocumentSummarizedEvent event(String documentId, String summary) {
        var event = new DocumentSummarizedEvent();
        event.setDocumentId(documentId);
        event.setSummary(summary);
        return event;
    }

    @Test
    void saves_summary_in_search_index_for_valid_event() {
        when(repository.existsById(id)).thenReturn(true);

        listener.consume(event(id.toString(), "Kurze Zusammenfassung"));

        verify(searchService).saveSummary(id, "Kurze Zusammenfassung");
    }

    @ParameterizedTest // mit mehreren Parametern selben Test aufrufen
    @NullAndEmptySource // zwei Testwerte, null und ""
    @ValueSource(strings = {"   "}) // noch einer mit nur Leerzeichen
    void ignores_event_without_summary(String summary) {
        listener.consume(event(id.toString(), summary));

        verifyNoInteractions(repository, searchService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-uuid"})
    void ignores_event_with_invalid_document_id(String documentId) {
        listener.consume(event(documentId, "Zusammenfassung"));

        verifyNoInteractions(repository, searchService);
    }

    // Falls z.B. ein Dokument gelöscht wurde, bevor die Zusammenfassung gespeichert werden konnte, darf nichts im Index landen.
    @Test
    void ignores_summary_for_deleted_document() {
        when(repository.existsById(id)).thenReturn(false);

        listener.consume(event(id.toString(), "Zusammenfassung"));

        verifyNoInteractions(searchService);
    }
}
