package at.technikum.api.service;

import at.technikum.api.dto.request.RequestDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentDTO;
import at.technikum.api.entity.Document;
import at.technikum.api.mapper.DocumentMapper;
import at.technikum.api.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper mapper;

    public ResponseDocumentDTO save(RequestDocumentDTO dto) {
        Document entity = mapper.dtoToEntity(dto);
        Document savedEntity = documentRepository.save(entity);
        return mapper.entityToDto(savedEntity);
    }
}