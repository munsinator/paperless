package at.technikum.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SearchService {

    List<UUID> search(String query);

    void saveTitle(UUID documentId, String title);

    void saveText(UUID documentId, String text);

    Optional<String> findText(UUID documentId);

    void saveSummary(UUID documentId, String summary);

    Optional<String> findSummary(UUID documentId);

    void delete(UUID documentId);
}
