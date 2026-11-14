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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticSearchService implements SearchService {

    private static final String INDEX = "documents"; // Index bei Elasticsearch ca das was eine Tabelle ist in einer relationalen Datenbank
    private static final int MAX_RESULTS = 100;

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
                            .fields("title^3", "text") // Treffer im Feld title dreimal so stark gewichtet werden wie Treffer im Feld text
                            .fuzziness("AUTO"))), Void.class); // fehlertolerante Suche

            List<UUID> ids = response.hits().hits().stream()
                    .map(hit -> hit.id())
                    .map(id -> UUID.fromString(id))
                    .toList();
            log.info("Search found {} documents", ids.size());
            return ids;
        } catch (IOException | ElasticsearchException exception) {
            throw new SearchException("Search in Elasticsearch failed", exception);
        }
    }

    @Override
    public void saveTitle(UUID documentId, String title) {
        throw new UnsupportedOperationException("Implemented in #76");
    }

    @Override
    public void delete(UUID documentId) {
        throw new UnsupportedOperationException("Implemented in #76");
    }
}
