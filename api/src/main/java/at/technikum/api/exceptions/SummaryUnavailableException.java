package at.technikum.api.exceptions;

import java.util.UUID;

public class SummaryUnavailableException extends RuntimeException {

    public SummaryUnavailableException(UUID documentId, Throwable cause) {
        super("The summary of document with ID " + documentId + " could not be read.", cause);
    }
}
