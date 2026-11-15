package at.technikum.api;

import at.technikum.api.exceptions.SearchException;
import at.technikum.api.service.SearchIndexInitializer;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.util.ObjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchIndexInitializerTest {

    @Mock
    private ElasticsearchClient client;

    @Mock
    private ElasticsearchIndicesClient indices;

    @InjectMocks
    private SearchIndexInitializer initializer;

    @BeforeEach
    void setUp() {
        when(client.indices()).thenReturn(indices);
    }

    @Test
    void run_does_not_create_index_when_it_already_exists() throws IOException {
        when(indices.exists(anyExists())).thenReturn(new BooleanResponse(true));

        initializer.run(new DefaultApplicationArguments());

        verify(indices, never()).create(anyCreate());
    }

    @Test
    void run_creates_index_when_it_is_missing() throws IOException {
        when(indices.exists(anyExists())).thenReturn(new BooleanResponse(false));

        initializer.run(new DefaultApplicationArguments());

        verify(indices).create(anyCreate());
    }

    @Test
    void run_ignores_index_created_concurrently() throws IOException {
        ElasticsearchException alreadyExists = elasticsearchError("resource_already_exists_exception");
        when(indices.exists(anyExists())).thenReturn(new BooleanResponse(false));
        when(indices.create(anyCreate())).thenThrow(alreadyExists);

        assertDoesNotThrow(() -> initializer.run(new DefaultApplicationArguments()));
    }

    @Test
    void run_wraps_other_elasticsearch_errors_in_search_exception() throws IOException {
        ElasticsearchException mappingError = elasticsearchError("mapper_parsing_exception");
        when(indices.exists(anyExists())).thenReturn(new BooleanResponse(false));
        when(indices.create(anyCreate())).thenThrow(mappingError);

        assertThrows(SearchException.class, () -> initializer.run(new DefaultApplicationArguments()));
    }


    @Test
    void run_wraps_connection_failure_in_search_exception() throws IOException {
        when(indices.exists(anyExists())).thenThrow(new IOException("connection refused"));

        assertThrows(SearchException.class, () -> initializer.run(new DefaultApplicationArguments()));
    }

    private ElasticsearchException elasticsearchError(String type) {
        ErrorCause cause = mock(ErrorCause.class);
        when(cause.type()).thenReturn(type);
        ElasticsearchException exception = mock(ElasticsearchException.class);
        when(exception.error()).thenReturn(cause);
        return exception;
    }

    private Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>> anyExists() {
        return ArgumentMatchers.any();
    }

    private Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> anyCreate() {
        return ArgumentMatchers.any();
    }
}
