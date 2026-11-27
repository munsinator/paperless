package at.technikum.api.exceptions;

import java.util.UUID;

public class TextUnavailableException extends RuntimeException {

    public TextUnavailableException(UUID documentId, Throwable cause) {
        super("The OCR text of document with ID " + documentId + " could not be read.", cause);
    }
}
