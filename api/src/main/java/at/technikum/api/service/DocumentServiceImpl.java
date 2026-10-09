package at.technikum.api.service;

import at.technikum.api.exceptions.DocumentNotFoundException;
import at.technikum.api.entity.Document;
import at.technikum.api.entity.DocumentType;
import at.technikum.api.entity.DocumentTypeEnum;
import at.technikum.api.repository.DocumentTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import at.technikum.api.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        //Null check in case the user adds a document type, otherwise null
        //TODO: Might change this approach in the future
        DocumentType type = null;
        if (requestedType != null) {
            type = documentTypeRepository.findById(requestedType)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Document type is not configured"));
        }
        document.setType(type);
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
        return documentRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(query, query);
    }

    @Override
    @Transactional(readOnly = true)
    public Document findDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }
}
