package at.technikum.api.service;

import at.technikum.api.exceptions.SearchException;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticSearchService implements SearchService {

    static final String INDEX = "documents"; // Index bei Elasticsearch ca das was eine Tabelle ist in einer relationalen Datenbank
    private static final int MAX_RESULTS = 100;

    // Aufbau eines Eintrags im Index: _id = documentId, Felder = title (schreibt die API) und text (schreibt der Indexing-Worker)
    static final String TITLE_FIELD = "title";
    static final String TEXT_FIELD = "text";
    private static final int TITLE_BOOST = 3; // Treffer im Titel zählen dreimal so viel wie Treffer im Text

    private final ElasticsearchClient client;

    @Override
    public List<UUID> search(String query) {
        log.debug("Searching index '{}' for: {}", INDEX, query);
        try {
            SearchResponse<Void> response = client.search(search -> search
                    .index(INDEX)
                    .ignoreUnavailable(true)
                    .size(MAX_RESULTS)
                    .source(source -> source.fetch(false)) // keine Metadaten
                    .query(q -> q.multiMatch(match -> match // gleichzeitig in mehreren Feldern suchen
                            .query(query)
                            .fields(TITLE_FIELD + "^" + TITLE_BOOST, TEXT_FIELD)
                            .fuzziness("AUTO"))), Void.class); // fehlertolerante Suche

            List<UUID> ids = response.hits().hits().stream()
                    .map(hit -> toDocumentId(hit.id()))
                    .flatMap(Optional::stream) // Optional::stream macht aus: - Optional.of(uuid1) einen Stream mit einer UUID. - Optional.empty() einen leeren Stream.
                    .toList();
            log.info("Search found {} documents", ids.size());
            return ids;
        } catch (IOException | ElasticsearchException exception) {
            throw new SearchException("Search in Elasticsearch failed", exception);
        }
    }

    // Ein Eintrag mit ungültiger ID (z. B. von Hand angelegt) darf nicht die ganze Suche zum Absturz bringen
    private Optional<UUID> toDocumentId(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException exception) {
            log.warn("Skipping search hit with invalid document id '{}'", id);
            return Optional.empty();
        }
    }

    @Override
    public void saveTitle(UUID documentId, String title) {
        log.debug("Saving title of document {} in index '{}'", documentId, INDEX);
        try {
            client.update(update -> update
                    .index(INDEX)
                    .id(documentId.toString())
                    .doc(Map.of(TITLE_FIELD, title)) // "Aktualisiere das Feld title des Elasticsearch-Dokuments"
                    .docAsUpsert(true), Void.class);
            log.info("Saved title of document {} in search index", documentId);
        } catch (IOException | ElasticsearchException exception) {
            throw new SearchException("Saving title in Elasticsearch failed", exception);
        }
    }

    @Override
    public void delete(UUID documentId) {
        log.debug("Deleting document {} from index '{}'", documentId, INDEX);
        try {
            client.delete(delete -> delete
                    .index(INDEX)
                    .id(documentId.toString()));
            log.info("Deleted document {} from search index", documentId);
        } catch (IOException | ElasticsearchException exception) {
            throw new SearchException("Deleting document in Elasticsearch failed", exception);
        }
    }
}
