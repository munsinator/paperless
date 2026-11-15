package at.technikum.ocrworker.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DocumentOcrCompletedEvent {
    private String documentId;
    private String documentName;
    private String content;
}
