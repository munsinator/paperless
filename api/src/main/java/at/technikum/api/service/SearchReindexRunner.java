package at.technikum.api.service;

import at.technikum.api.entity.Document;
import at.technikum.api.exceptions.SearchException;
import at.technikum.api.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Schreibt beim Start alle Titel aus Postgres neu in den Suchindex (Postgres ist die Source of Truth).
 * Nur aktiv mit search.reindex-on-startup=true, z. B. nach einem verlorenen Index oder fehlgeschlagenen Schreibvorgängen.
 * Der OCR-Text bleibt unberührt: Er gehört dem Indexing-Worker und steht nicht in Postgres.
 */
@Slf4j
@Component
@Order(2) // nach dem SearchIndexInitializer, damit der Index sein Mapping schon hat
@ConditionalOnProperty(name = "search.reindex-on-startup", havingValue = "true") // "Erstelle diese Spring-Komponente nur, wenn search.reindex-on-startup true ist in z.B. application.properties"
@RequiredArgsConstructor
public class SearchReindexRunner implements ApplicationRunner {

    private final DocumentRepository documentRepository;
    private final SearchService searchService;

    @Override
    public void run(ApplicationArguments args) {
        List<Document> documents = documentRepository.findAll();
        log.info("Reindexing titles of {} documents", documents.size());

        int failed = 0;
        for (Document document : documents) {
            try {
                searchService.saveTitle(document.getId(), document.getTitle());
            } catch (SearchException exception) {
                failed++; // ein Fehler soll die übrigen Dokumente nicht blockieren
                log.warn("Title of document {} could not be reindexed", document.getId(), exception);
            }
        }
        log.info("Reindex finished: {} indexed, {} failed", documents.size() - failed, failed);
    }
}
