package at.technikum.api.service;

import at.technikum.api.entity.Document;
import at.technikum.api.entity.DocumentTypeEnum;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentService {

    Document save(Document document, DocumentTypeEnum type);

    // validiert das hochgeladene PDF, speichert die Metadaten und legt die Datei im Storage ab
    Document saveWithFile(Document document, DocumentTypeEnum type, MultipartFile file);

    // liefert das gespeicherte PDF als Stream (der Aufrufer muss ihn schliessen)
    InputStream loadFile(UUID documentId);

    Document update(UUID documentId, Document document, DocumentTypeEnum type);

    // löscht das Dokument inklusive der gespeicherten Datei
    void delete(UUID documentId);

    List<Document> findAllDocuments(String query);

    Document findDocumentById(UUID documentId);

    // liest die Zusammenfassung aus dem Suchindex; leer, solange sie noch nicht erzeugt wurde;
    // wirft SummaryUnavailableException, wenn der Index nicht erreichbar ist
    Optional<String> findSummary(UUID documentId);

    // liest den OCR-Text aus dem Suchindex; leer, solange die OCR noch nicht fertig ist;
    // wirft TextUnavailableException, wenn der Index nicht erreichbar ist
    Optional<String> findText(UUID documentId);
}
