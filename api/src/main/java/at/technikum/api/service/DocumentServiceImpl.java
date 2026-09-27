package at.technikum.api.service;

import at.technikum.api.entity.Document;
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
    // TODO check if mapping has to happen in controller or service
    // private final DocumentMapper mapper;

    @Override
    @Transactional
    public Document save(Document document) {
        return documentRepository.save(document);
    }

    @Override
    @Transactional
    public void delete(UUID documentId) {
        if(!documentRepository.existsById(documentId)) {
            throw new RuntimeException("Document not found with ID: " + documentId);
        }
        documentRepository.deleteById(documentId);
    }

    // TODO here later search as attribute
    @Override
    @Transactional(readOnly = true)
    public List<Document> findAllDocuments() {
        return documentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Document findDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));
    }
}