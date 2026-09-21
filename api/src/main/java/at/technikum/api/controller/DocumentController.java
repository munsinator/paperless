package at.technikum.api.controller;

import at.technikum.api.dto.request.RequestDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentDTO;
import at.technikum.api.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/document")
//@CrossOrigin(origins = "http://localhost:4200")
public class DocumentController {

    //private static final Logger logger = LoggerFactory.getLogger(GlobalTourLogController.class);
    public final DocumentService documentService;

    DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<ResponseDocumentDTO> create(@Valid @RequestBody RequestDocumentDTO requestDocumentDTO) {
        ResponseDocumentDTO responseDocumentDTO = documentService.save(requestDocumentDTO);
        return new ResponseEntity<>(responseDocumentDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("test");
    }
}
