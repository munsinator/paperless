package at.technikum.api;

import at.technikum.api.entity.Document;
import at.technikum.api.exceptions.SearchException;
import at.technikum.api.repository.DocumentRepository;
import at.technikum.api.service.SearchReindexRunner;
import at.technikum.api.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchReindexRunnerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchReindexRunner runner;

    @Test
    void run_saves_title_of_every_document() {
        Document first = document("Rechnung");
        Document second = document("Vertrag");
        when(documentRepository.findAll()).thenReturn(List.of(first, second));

        runner.run(new DefaultApplicationArguments());

        verify(searchService).saveTitle(first.getId(), "Rechnung");
        verify(searchService).saveTitle(second.getId(), "Vertrag");
    }

    @Test
    void run_continues_with_next_document_when_one_fails() {
        Document first = document("Rechnung");
        Document second = document("Vertrag");
        when(documentRepository.findAll()).thenReturn(List.of(first, second));
        doThrow(new SearchException("down", new IOException())).when(searchService).saveTitle(first.getId(), "Rechnung");

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments()));

        verify(searchService).saveTitle(second.getId(), "Vertrag");
    }

    @Test
    void run_does_nothing_without_documents() {
        when(documentRepository.findAll()).thenReturn(List.of());

        runner.run(new DefaultApplicationArguments());

        verifyNoInteractions(searchService);
    }

    private Document document(String title) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle(title);
        return document;
    }
}
