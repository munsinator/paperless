package at.technikum.api;

import at.technikum.api.exceptions.SearchException;
import at.technikum.api.service.ElasticSearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticSearchServiceTest {

    @Mock
    private ElasticsearchClient client;

    @InjectMocks
    private ElasticSearchService searchService;

    @Test
    void search_returns_hit_ids_in_the_order_elasticsearch_ranked_them() throws IOException {
        UUID best = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        mockResponse(response(best.toString(), second.toString()));

        assertEquals(List.of(best, second), searchService.search("hello"));
    }

    @Test
    void search_skips_hits_whose_id_is_not_a_uuid() throws IOException {
        UUID valid = UUID.randomUUID();
        mockResponse(response("not-a-uuid", valid.toString()));

        assertEquals(List.of(valid), searchService.search("hello"));
    }

    @Test
    void search_returns_empty_list_when_nothing_matches() throws IOException {
        mockResponse(response());

        assertTrue(searchService.search("hello").isEmpty());
    }

    @Test
    void search_wraps_connection_failure_in_search_exception() throws IOException {
        when(client.search(anySearch(), eq(Void.class))).thenThrow(new IOException("connection refused"));

        assertThrows(SearchException.class, () -> searchService.search("hello"));
    }

    @Test
    void search_wraps_elasticsearch_error_in_search_exception() throws IOException {
        when(client.search(anySearch(), eq(Void.class))).thenThrow(mock(ElasticsearchException.class));

        assertThrows(SearchException.class, () -> searchService.search("hello"));
    }

    @Test
    @SuppressWarnings("unchecked") // Test verwendet any(Function.class): Java gibt manchmal Warnung bezüglich der nicht überprüften Typinformation
    void save_title_sends_partial_update_to_elasticsearch() throws IOException {
        searchService.saveTitle(UUID.randomUUID(), "Meine Rechnung");

        verify(client).update(any(Function.class), eq(Void.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void save_title_wraps_connection_failure_in_search_exception() throws IOException {
        when(client.update(any(Function.class), eq(Void.class))).thenThrow(new IOException("connection refused"));

        assertThrows(SearchException.class, () -> searchService.saveTitle(UUID.randomUUID(), "Meine Rechnung"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void delete_removes_document_from_elasticsearch() throws IOException {
        searchService.delete(UUID.randomUUID());

        verify(client).delete(any(Function.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void delete_wraps_elasticsearch_error_in_search_exception() throws IOException {
        when(client.delete(any(Function.class))).thenThrow(mock(ElasticsearchException.class));

        assertThrows(SearchException.class, () -> searchService.delete(UUID.randomUUID()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void save_text_updates_only_text_and_supports_ocr_arriving_before_title() throws IOException {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doAnswer(invocation -> {
            Function<co.elastic.clients.elasticsearch.core.UpdateRequest.Builder<Void, java.util.Map<String, String>>,
                    ObjectBuilder<co.elastic.clients.elasticsearch.core.UpdateRequest<Void, java.util.Map<String, String>>>> builder = invocation.getArgument(0);
            var request = builder.apply(new co.elastic.clients.elasticsearch.core.UpdateRequest.Builder<>()).build();
            assertEquals("documents", request.index());
            assertEquals(id.toString(), request.id());
            assertEquals(java.util.Map.of("text", "Hello World"), request.doc());
            assertEquals(Boolean.TRUE, request.docAsUpsert());
            return null;
        }).when(client).update(any(Function.class), eq(Void.class));
        searchService.saveText(id, "Hello World");
        verify(client).update(any(Function.class), eq(Void.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void save_text_wraps_connection_failure() throws IOException {
        when(client.update(any(Function.class), eq(Void.class))).thenThrow(new IOException("unavailable"));
        assertThrows(SearchException.class, () -> searchService.saveText(UUID.randomUUID(), "Hello World"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void find_text_returns_text_field_of_the_index_document() throws IOException {
        GetResponse<Map> response = mock(GetResponse.class);
        when(response.found()).thenReturn(true);
        when(response.source()).thenReturn(Map.of("text", "Hello World"));
        when(client.get(any(Function.class), eq(Map.class))).thenReturn(response);

        assertEquals(Optional.of("Hello World"), searchService.findText(UUID.randomUUID()));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void find_text_returns_empty_when_document_or_field_does_not_exist_yet() throws IOException {
        GetResponse<Map> notFound = mock(GetResponse.class);
        when(notFound.found()).thenReturn(false);
        GetResponse<Map> withoutText = mock(GetResponse.class);
        when(withoutText.found()).thenReturn(true);
        when(withoutText.source()).thenReturn(Map.of());
        when(client.get(any(Function.class), eq(Map.class))).thenReturn(notFound, withoutText);

        assertEquals(Optional.empty(), searchService.findText(UUID.randomUUID()));
        assertEquals(Optional.empty(), searchService.findText(UUID.randomUUID()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void find_text_wraps_connection_failure_in_search_exception() throws IOException {
        when(client.get(any(Function.class), eq(Map.class))).thenThrow(new IOException("unavailable"));

        assertThrows(SearchException.class, () -> searchService.findText(UUID.randomUUID()));
    }

    private void mockResponse(SearchResponse<Void> response) throws IOException {
        when(client.search(anySearch(), eq(Void.class))).thenReturn(response);
    }

    private static Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> anySearch() {
        return any();
    }

    private static SearchResponse<Void> response(String... ids) {
        return SearchResponse.of(response -> response
                .took(1)
                .timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(hits -> {
                    hits.hits(List.of());
                    for (String id : ids) {
                        hits.hits(hit -> hit.index("documents").id(id));
                    }
                    return hits;
                }));
    }
}
