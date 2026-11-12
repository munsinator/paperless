package at.technikum.api.service;

import java.io.InputStream;
import java.util.UUID;

public interface StorageService {

    void store(UUID documentId, InputStream content, long size);

    InputStream load(UUID documentId);

    void delete(UUID documentId);
}
