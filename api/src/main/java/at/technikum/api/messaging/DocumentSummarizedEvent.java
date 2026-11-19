package at.technikum.api.messaging;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class DocumentSummarizedEvent {
    private String documentId;
    private String summary;
}
