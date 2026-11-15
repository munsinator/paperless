package at.technikum.api.service;

import java.util.List;
import java.util.UUID;

public interface SearchService {

    List<UUID> search(String query);

    void saveTitle(UUID documentId, String title);

    void saveText(UUID documentId, String text);

    void delete(UUID documentId);
}
