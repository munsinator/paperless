package at.technikum.api.dto.response;

import at.technikum.api.entity.DocumentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDocumentDTO {

    private UUID id;
    private String title;
    private String content; // for ocr
    private DocumentTypeEnum documentType;
    private LocalDate created;
    private LocalTime added;
    private LocalTime modified;

    // special feature ?
    // private String type;

}
