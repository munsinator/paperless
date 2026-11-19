package at.technikum.api.messaging;

import at.technikum.api.entity.SummaryStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class DocumentSummarizedEvent {
    // String weil keine UUIDs in JSON (über Message Queue)
    private String documentId;
    // nur bei DONE gesetzt; bei FAILED gibt der Worker endgültig auf (nach Retries), dann gibt es keine Summary
    private String summary;
    // fehlt das Feld in der Nachricht, bleibt der Default: ältere Worker ohne Status senden nur erfolgreiche Summaries
    private SummaryStatus status = SummaryStatus.DONE;
}
