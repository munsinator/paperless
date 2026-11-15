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
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    @Size(max = 20, message = "At most 20 tags are allowed")
    private List<@NotBlank @Size(max = 50) @Pattern(regexp = "(?U)\\S+", message = "Tags must not contain whitespace") String> tags;
    private DocumentTypeEnum type;

    @AssertTrue(message = "Tags must be unique (case-insensitive)")
    public boolean isTagsUnique() {
        if (tags == null) return true;
        var seen = new java.util.HashSet<String>();
        return tags.stream().filter(java.util.Objects::nonNull)
                .allMatch(tag -> seen.add(tag.toLowerCase(java.util.Locale.ROOT)));
    }
}
