package at.technikum.api.service;

import at.technikum.api.entity.Document;
import at.technikum.api.entity.DocumentTypeEnum;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    Document save(Document document, DocumentTypeEnum type);

    void delete(UUID documentId);

    List<Document> findAllDocuments(String query);

    Document findDocumentById(UUID documentId);
}
