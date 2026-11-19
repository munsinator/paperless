package at.technikum.api;

import at.technikum.api.exceptions.DocumentNotFoundException;
import at.technikum.api.messaging.DocumentSummarizedEvent;
import at.technikum.api.messaging.DocumentSummarizedListener;
import at.technikum.api.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DocumentSummarizedListenerTest {

    private final DocumentService documentService = mock(DocumentService.class);
    private final DocumentSummarizedListener listener = new DocumentSummarizedListener(documentService);
    private final UUID id = UUID.randomUUID();

    private DocumentSummarizedEvent event(String documentId, String summary) {
        var event = new DocumentSummarizedEvent();
        event.setDocumentId(documentId);
        event.setSummary(summary);
        return event;
    }

    @Test
    void saves_summary_for_valid_event() {
        listener.consume(event(id.toString(), "Kurze Zusammenfassung"));

        verify(documentService).updateSummary(id, "Kurze Zusammenfassung");
    }

    @ParameterizedTest // mit mehreren Parametern selben Test aufrufen
    @NullAndEmptySource // zwei Testwerte, null und ""
    @ValueSource(strings = {"   "}) // noch einer mit nur Leerzeichen
    void ignores_event_without_summary(String summary) {
        listener.consume(event(id.toString(), summary));

        verifyNoInteractions(documentService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-uuid"})
    void ignores_event_with_invalid_document_id(String documentId) {
        listener.consume(event(documentId, "Zusammenfassung"));

        verifyNoInteractions(documentService);
    }

    // Falls z.B. ein Dokument gelöscht wurde, bevor die Zusammenfassung gespeichert werden konnte, darf kein Fehler geworfen werden.
    @Test
    void ignores_summary_for_deleted_document_without_throwing() {
        doThrow(new DocumentNotFoundException(id)).when(documentService).updateSummary(id, "Zusammenfassung");

        assertDoesNotThrow(() -> listener.consume(event(id.toString(), "Zusammenfassung")));
    }
}
