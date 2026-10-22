package at.technikum.api.controller;

import at.technikum.api.dto.request.RequestDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentData;
import at.technikum.api.mapper.DocumentMapper;
import at.technikum.api.service.DocumentService;
import at.technikum.api.service.DocumentServiceImpl;
import at.technikum.api.entity.Document;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/document")
//@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class DocumentController {

    public final DocumentService documentService;
    public final DocumentMapper mapper;

    DocumentController(DocumentService documentService, DocumentMapper mapper) {
        this.documentService = documentService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ResponseDocumentDTO> create(@Valid @RequestBody RequestDocumentDTO requestDocumentDTO) {
        log.debug("Create document request: title={}", requestDocumentDTO.getTitle());
        Document entityToSave = mapper.dtoToEntity(requestDocumentDTO);
        Document savedEntity = documentService.save(entityToSave, requestDocumentDTO.getType());
        return new ResponseEntity<>(mapper.entityToDto(savedEntity), HttpStatus.CREATED);
    }

    // Upload mit der eigentlichen PDF-Datei: multipart/form-data mit file, title, type und tags.
    // Die Datei wird validiert und an das Backend übertragen, aber noch nicht gespeichert.
    // consumes = welches Datenformat die Methode entgegennehmen kann
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDocumentDTO> upload(
        // "nimm den teil im multipart/form-data wo file steht" (append)
            @RequestPart("file") MultipartFile file,
            // @ModelAttribute: "nimm die übermittelten Formulardaten und mappe sie auf das DTO"
            @Valid @ModelAttribute RequestDocumentDTO requestDocumentDTO
    ) {
        log.debug("Upload document request: title={}, file={} ({} bytes)",
                requestDocumentDTO.getTitle(), file.getOriginalFilename(), file.getSize());
        Document entityToSave = mapper.dtoToEntity(requestDocumentDTO);
        Document savedEntity = documentService.saveWithFile(entityToSave, requestDocumentDTO.getType(), file);
        return new ResponseEntity<>(mapper.entityToDto(savedEntity), HttpStatus.CREATED);
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<ResponseDocumentDTO> update(
            @PathVariable UUID documentId,
            @Valid @RequestBody RequestDocumentDTO requestDocumentDTO
    ) {
        log.debug("Update document request: id={}", documentId);
        Document entityToUpdate = mapper.dtoToEntity(requestDocumentDTO);
        Document updatedEntity = documentService.update(
                documentId,
                entityToUpdate,
                requestDocumentDTO.getType()
        );
        return ResponseEntity.ok(mapper.entityToDto(updatedEntity));
    }

    @GetMapping
    public ResponseEntity<List<ResponseDocumentDTO>> findAll(@RequestParam(required = false) String query) {
        log.debug("List documents request (search: {})", query != null && !query.isBlank());
        List<ResponseDocumentDTO> documents = documentService.findAllDocuments(query).stream()
                .map(mapper::entityToDto)
                .toList();

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ResponseDocumentDTO> findById(@PathVariable UUID documentId) {
        log.debug("Get document request: id={}", documentId);
        Document document = documentService.findDocumentById(documentId);
        return ResponseEntity.ok(mapper.entityToDto(document));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId) {
        log.debug("Delete document request: id={}", documentId);
        documentService.delete(documentId);
        return ResponseEntity.noContent().build(); // TODO check if HTTP 204 No Content needed, or data from the document is displayed after deleting
    }

    @GetMapping("/{documentId}/data")
    public ResponseEntity<ResponseDocumentData> findDataById(@PathVariable UUID documentId) {
        log.warn("Get document data request not implemented: id={}", documentId);
        //TODO: Create minIO service to fetch data and don't forget to map the response from service to ResponseDocumentData
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @DeleteMapping("/{documentId}/data")
    public ResponseEntity<String> deleteData(@PathVariable UUID documentId) {
        log.warn("Delete document data request not implemented: id={}", documentId);
        //TODO: Create minIO service to delete data
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
