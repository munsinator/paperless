package at.technikum.api.service;

import at.technikum.api.exceptions.DocumentNotFoundException;
import at.technikum.api.exceptions.InvalidFileException;
import at.technikum.api.entity.Document;
import at.technikum.api.entity.DocumentType;
import at.technikum.api.entity.DocumentTypeEnum;
import at.technikum.api.repository.DocumentTypeRepository;
import at.technikum.api.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    // TODO check if mapping has to happen in controller or service
    // private final DocumentMapper mapper;

    @Override
    @Transactional
    public Document save(Document document, DocumentTypeEnum requestedType) {
        document.setType(resolveType(requestedType));
        document.setCreatedAt(LocalDate.now());
        if (document.getTags() == null) {
            document.setTags(List.of());
        }
        return documentRepository.save(document);
    }

    @Override
    @Transactional
    public Document saveWithFile(Document document, DocumentTypeEnum requestedType, MultipartFile file) {
        validatePdf(file);
        return save(document, requestedType);
    }

    @Override
    @Transactional
    public Document update(UUID documentId, Document updatedDocument, DocumentTypeEnum requestedType) {
        Document document = findDocumentById(documentId);
        document.setTitle(updatedDocument.getTitle());
        document.setTags(updatedDocument.getTags() == null ? List.of() : updatedDocument.getTags());
        document.setType(resolveType(requestedType));
        return documentRepository.save(document);
    }

    @Override
    @Transactional
    public void delete(UUID documentId) {
        if(!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        documentRepository.deleteById(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findAllDocuments(String query) {
        if (query == null || query.isBlank()) {
            return documentRepository.findAll();
        }
        return documentRepository.findByTitleContainingIgnoreCaseOrOcrTextContainingIgnoreCase(query, query);
    }

    @Override
    @Transactional(readOnly = true)
    public Document findDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    private DocumentType resolveType(DocumentTypeEnum requestedType) {
        if (requestedType == null) {
            return null;
        }
        return documentTypeRepository.findById(requestedType)
                .orElseGet(() -> documentTypeRepository.save(new DocumentType(requestedType)));
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("The uploaded file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidFileException("Only PDF files are supported");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(5);
            if (!new String(header, StandardCharsets.US_ASCII).equals("%PDF-")) {
                throw new InvalidFileException("The file is not a valid PDF");
            }
        } catch (IOException exception) {
            throw new InvalidFileException("The uploaded file could not be read");
        }
    }
}
