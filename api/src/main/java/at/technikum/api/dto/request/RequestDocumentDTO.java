package at.technikum.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestDocumentDTO {

    @NotBlank(message = "Title cannot be empty")
    private String title;
}