package at.technikum.api.controller;

import at.technikum.api.dto.request.RequestDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentData;
import at.technikum.api.mapper.DocumentMapper;
import at.technikum.api.service.DocumentService;
import at.technikum.api.service.DocumentServiceImpl;
import at.technikum.api.entity.Document;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/document")
//@CrossOrigin(origins = "http://localhost:4200")
public class DocumentController {

    //private static final Logger logger = LoggerFactory.getLogger(GlobalTourLogController.class);
    public final DocumentService documentService;
    public final DocumentMapper mapper;

    DocumentController(DocumentService documentService, DocumentMapper mapper) {
        this.documentService = documentService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ResponseDocumentDTO> create(@Valid @RequestBody RequestDocumentDTO requestDocumentDTO) {
        Document entityToSave = mapper.dtoToEntity(requestDocumentDTO);
        Document savedEntity = documentService.save(entityToSave, requestDocumentDTO.getType());
        return new ResponseEntity<>(mapper.entityToDto(savedEntity), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ResponseDocumentDTO>> findAll(@RequestParam(required = false) String query) {
        List<ResponseDocumentDTO> documents = documentService.findAllDocuments(query).stream()
                .map(mapper::entityToDto)
                .toList();

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ResponseDocumentDTO> findById(@PathVariable UUID documentId) {
        Document document = documentService.findDocumentById(documentId);
        return ResponseEntity.ok(mapper.entityToDto(document));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId) {
        documentService.delete(documentId);
        return ResponseEntity.noContent().build(); // TODO check if HTTP 204 No Content needed, or data from the document is displayed after deleting
    }

    @GetMapping("/{documentId}/data")
    public ResponseEntity<ResponseDocumentData> findDataById(@PathVariable UUID documentId) {
        //TODO: Create minIO service to fetch data and don't forget to map the response from service to ResponseDocumentData
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @DeleteMapping("/{documentId}/data")
    public ResponseEntity<String> deleteData(@PathVariable UUID documentId) {
        //TODO: Create minIO service to delete data
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

}
