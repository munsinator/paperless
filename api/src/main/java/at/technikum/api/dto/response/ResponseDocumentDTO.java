package at.technikum.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDocumentDTO {

    private UUID id;
    private String title;
    private String category;
    private LocalDate createdAt;
    private List<String> tags;
    private String summary;
    private String ocrText;
    private String fileUrl;
}