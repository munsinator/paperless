package at.technikum.api.service;

import at.technikum.api.entity.Document;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    Document save(Document document);

    void delete(UUID documentId);

    List<Document> findAllDocuments();

    Document findDocumentById(UUID documentId);
}