package at.technikum.api.service;

import at.technikum.api.exceptions.SearchException;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static at.technikum.api.service.ElasticSearchService.INDEX;
import static at.technikum.api.service.ElasticSearchService.SUMMARY_FIELD;
import static at.technikum.api.service.ElasticSearchService.TEXT_FIELD;
import static at.technikum.api.service.ElasticSearchService.TITLE_FIELD;

/**
 * Legt den Suchindex mit explizitem Mapping an, falls er noch nicht existiert.
 * Ohne diesen Schritt würde Elasticsearch das Mapping beim ersten Schreiben selbst erraten.
 */
@Slf4j
@Component
@Order(1) // läuft vor dem Reindex, damit der Index sein Mapping hat, bevor Titel geschrieben werden
@RequiredArgsConstructor
public class SearchIndexInitializer implements ApplicationRunner {

    private static final String ALREADY_EXISTS = "resource_already_exists_exception"; // Fehlername, den Elasticsearch zurückgibt, wenn man versucht, einen Index zu erstellen, der bereits existiert.

    private final ElasticsearchClient client;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // checkt ob Index documents schon existiert
            if (client.indices().exists(exists -> exists.index(INDEX)).value()) {
                log.info("Search index '{}' is ready", INDEX);
                return;
            }
            createIndex();
            log.info("Created search index '{}'", INDEX);
        } catch (IOException | ElasticsearchException exception) {
            throw new SearchException("Could not prepare search index '" + INDEX + "'", exception);
        }
    }

    private void createIndex() throws IOException {
        try {
            client.indices().create(create -> create
                    .index(INDEX)
                    .mappings(mappings -> mappings
                            .dynamic(DynamicMapping.Strict) // nur title, text und summary, keine fremden Felder
                            .properties(TITLE_FIELD, property -> property.text(text -> text
                                    .fields("keyword", field -> field.keyword(keyword -> keyword)))) // text durchsuchung und keyword durchsuchung
                            .properties(TEXT_FIELD, property -> property.text(text -> text))
                            .properties(SUMMARY_FIELD, property -> property.text(text -> text.index(false))))); // nur gespeichert, nicht durchsuchbar
        } catch (ElasticsearchException exception) {
            // der Indexing-Worker kann den Index zwischen Prüfung und Anlegen erzeugt haben
            if (exception.error() == null || !ALREADY_EXISTS.equals(exception.error().type())) {
                throw exception;
            }
            log.info("Search index '{}' was created concurrently", INDEX);
        }
    }
}

/**
 * if (exception.error() == null || !ALREADY_EXISTS.equals(exception.error().type())) {
 * "Wenn keine Fehlerinformation vorhanden ist ODER wenn es nicht der Fehler 'Index existiert bereits' ist, dann wirf die Exception weiter."
 */