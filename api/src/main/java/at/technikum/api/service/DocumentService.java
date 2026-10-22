package at.technikum.api.service;

import at.technikum.api.entity.Document;
import at.technikum.api.entity.DocumentTypeEnum;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    Document save(Document document, DocumentTypeEnum type);

    // validiert das hochgeladene PDF und speichert die Metadaten (die Datei selbst wird noch nicht gespeichert)
    Document saveWithFile(Document document, DocumentTypeEnum type, MultipartFile file);

    Document update(UUID documentId, Document document, DocumentTypeEnum type);

    void delete(UUID documentId);

    List<Document> findAllDocuments(String query);

    Document findDocumentById(UUID documentId);
}
