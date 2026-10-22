package at.technikum.ocrworker.messaging;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DocumentSavedEvent {
    private String documentId;
    private String documentName;
}