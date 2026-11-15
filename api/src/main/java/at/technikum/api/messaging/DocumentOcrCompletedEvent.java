package at.technikum.api.messaging;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
public class DocumentOcrCompletedEvent {
    private String documentId;
    private String documentName;
    private String content;
}
