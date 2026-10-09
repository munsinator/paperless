package at.technikum.api.dto.request;

import at.technikum.api.entity.DocumentTypeEnum;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestDocumentDTO {

    @NotBlank(message = "Title cannot be empty")
    private String title;
    private List<String> tags;
    private DocumentTypeEnum type;
}
