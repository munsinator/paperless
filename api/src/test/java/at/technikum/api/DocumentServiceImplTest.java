package at.technikum.api;

import at.technikum.api.entity.Document;
import at.technikum.api.entity.DocumentType;
import at.technikum.api.entity.DocumentTypeEnum;
import at.technikum.api.messaging.DocumentSavedEvent;
import at.technikum.api.exceptions.DocumentNotFoundException;
import at.technikum.api.exceptions.InvalidFileException;
import at.technikum.api.exceptions.StorageException;
import at.technikum.api.repository.DocumentRepository;
import at.technikum.api.repository.DocumentTypeRepository;
import at.technikum.api.service.DocumentServiceImpl;
import at.technikum.api.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    void save_sets_creation_date_tags_and_creates_unseeded_type() {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle("Semesterrechnung");
        document.setTags(List.of("Studium", "Rechnung"));
        DocumentType type = new DocumentType(DocumentTypeEnum.RECEIPT);

        when(documentTypeRepository.findById(DocumentTypeEnum.RECEIPT)).thenReturn(Optional.empty());
        when(documentTypeRepository.save(any(DocumentType.class))).thenReturn(type);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Document savedDocument = documentService.save(document, DocumentTypeEnum.RECEIPT);

        assertEquals(LocalDate.now(), savedDocument.getCreatedAt());
        assertEquals(List.of("Studium", "Rechnung"), savedDocument.getTags());
        assertEquals(type, savedDocument.getType());
        verify(documentTypeRepository).save(
                org.mockito.ArgumentMatchers.argThat(savedType ->
                        savedType.getType() == DocumentTypeEnum.RECEIPT));
        verify(documentRepository).save(document);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.argThat(
                (Object event) -> event instanceof DocumentSavedEvent savedEvent
                        && savedEvent.getDocumentId().equals(document.getId().toString())));
    }

    @Test
    void save_does_not_publish_when_repository_save_fails() {
        when(documentRepository.save(any(Document.class)))
                .thenThrow(new IllegalStateException("Database unavailable"));

        assertThrows(IllegalStateException.class,
                () -> documentService.save(new Document(), null));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void update_changes_only_editable_metadata_and_keeps_creation_date() {
        UUID id = UUID.randomUUID();
        LocalDate createdAt = LocalDate.of(2026, 10, 1);
        Document current = new Document();
        current.setId(id);
        current.setTitle("Alte Rechnung");
        current.setTags(List.of("Alt"));
        current.setCreatedAt(createdAt);

        Document update = new Document();
        update.setTitle("Neue Rechnung");
        update.setTags(List.of("Neu"));

        DocumentType type = new DocumentType(DocumentTypeEnum.RECEIPT);
        when(documentRepository.findById(id)).thenReturn(Optional.of(current));
        when(documentTypeRepository.findById(DocumentTypeEnum.RECEIPT)).thenReturn(Optional.of(type));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Document savedDocument = documentService.update(id, update, DocumentTypeEnum.RECEIPT);

        assertEquals("Neue Rechnung", savedDocument.getTitle());
        assertEquals(List.of("Neu"), savedDocument.getTags());
        assertEquals(type, savedDocument.getType());
        assertEquals(createdAt, savedDocument.getCreatedAt());
        assertNotNull(savedDocument.getId());
        verify(documentRepository).save(current);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void save_with_file_saves_metadata_and_stores_pdf_for_valid_pdf() {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle("Rechnung");
        byte[] content = "%PDF-1.7".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "rechnung.pdf", "application/pdf", content);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Document saved = documentService.saveWithFile(document, null, file);

        assertEquals(LocalDate.now(), saved.getCreatedAt());
        verify(documentRepository).save(document);
        verify(storageService).store(document.getId(), content);
    }

    @Test
    void save_with_file_propagates_storage_failure() {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        MockMultipartFile file = new MockMultipartFile(
                "file", "rechnung.pdf", "application/pdf", "%PDF-1.7".getBytes());
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new StorageException("Storage down", new RuntimeException()))
                .when(storageService).store(any(UUID.class), any(byte[].class));

        assertThrows(StorageException.class, () -> documentService.saveWithFile(document, null, file));
    }

    @Test
    void delete_removes_document_and_stored_file() {
        UUID id = UUID.randomUUID();
        when(documentRepository.existsById(id)).thenReturn(true);

        documentService.delete(id);

        verify(documentRepository).deleteById(id);
        verify(storageService).delete(id);
    }

    @Test
    void delete_throws_not_found_and_skips_storage_for_unknown_document() {
        UUID id = UUID.randomUUID();
        when(documentRepository.existsById(id)).thenReturn(false);

        assertThrows(DocumentNotFoundException.class, () -> documentService.delete(id));
        verifyNoInteractions(storageService);
    }

    @Test
    void load_file_returns_stream_from_storage_for_existing_document() {
        UUID id = UUID.randomUUID();
        InputStream content = new ByteArrayInputStream("%PDF-1.7".getBytes());
        when(documentRepository.existsById(id)).thenReturn(true);
        when(storageService.load(id)).thenReturn(content);

        assertSame(content, documentService.loadFile(id));
    }

    @Test
    void load_file_throws_not_found_and_skips_storage_for_unknown_document() {
        UUID id = UUID.randomUUID();
        when(documentRepository.existsById(id)).thenReturn(false);

        assertThrows(DocumentNotFoundException.class, () -> documentService.loadFile(id));
        verifyNoInteractions(storageService);
    }

    @Test
    void save_with_file_rejects_empty_non_pdf_and_fake_pdf_files() {
        Document document = new Document();
        MockMultipartFile empty = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);
        MockMultipartFile wrongName = new MockMultipartFile("file", "a.txt", "text/plain", "%PDF-1.7".getBytes());
        MockMultipartFile fakePdf = new MockMultipartFile("file", "a.pdf", "application/pdf", "hello".getBytes());

        assertThrows(InvalidFileException.class, () -> documentService.saveWithFile(document, null, empty));
        assertThrows(InvalidFileException.class, () -> documentService.saveWithFile(document, null, wrongName));
        assertThrows(InvalidFileException.class, () -> documentService.saveWithFile(document, null, fakePdf));
        verifyNoInteractions(documentRepository, storageService);
    }
}
