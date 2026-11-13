package at.technikum.api.service;

import at.technikum.api.entity.Document;
import at.technikum.api.entity.DocumentTypeEnum;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface DocumentService {

    Document save(Document document, DocumentTypeEnum type);

    // validiert das hochgeladene PDF, speichert die Metadaten und legt die Datei im Storage ab
    Document saveWithFile(Document document, DocumentTypeEnum type, MultipartFile file);

    // liefert das gespeicherte PDF als Stream (der Aufrufer muss ihn schliessen)
    InputStream loadFile(UUID documentId);

    Document update(UUID documentId, Document document, DocumentTypeEnum type);

    void delete(UUID documentId);

    List<Document> findAllDocuments(String query);

    Document findDocumentById(UUID documentId);
}
