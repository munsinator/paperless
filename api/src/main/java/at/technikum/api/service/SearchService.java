package at.technikum.api.service;

import java.util.List;
import java.util.UUID;

public interface SearchService {

    List<UUID> search(String query);

    void indexTitle(UUID documentId, String title);

    void delete(UUID documentId);
}
