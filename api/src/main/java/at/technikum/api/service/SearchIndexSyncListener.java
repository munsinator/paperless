package at.technikum.api.service;

import at.technikum.api.exceptions.SearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Hält den Suchindex nach dem DB-Commit aktuell. Ein Fehler in Elasticsearch
 * darf die bereits committete Änderung in Postgres nicht beeinträchtigen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexSyncListener {

    private final SearchService searchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // methode reagiert auf bestimmtes Event: DB Transaktion erfolgreich
    public void onTitleChanged(DocumentTitleChangedEvent event) {
        try {
            searchService.saveTitle(event.documentId(), event.title());
        } catch (SearchException exception) {
            log.warn("Title of document {} could not be saved in the search index", event.documentId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(DocumentDeletedEvent event) {
        try {
            searchService.delete(event.documentId());
        } catch (SearchException exception) {
            log.warn("Document {} could not be deleted from the search index", event.documentId(), exception);
        }
    }
}
