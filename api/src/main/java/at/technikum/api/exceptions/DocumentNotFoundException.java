package at.technikum.api.exceptions;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID id) {
        super("Document with ID " + id + " was not found.");
    }
}
