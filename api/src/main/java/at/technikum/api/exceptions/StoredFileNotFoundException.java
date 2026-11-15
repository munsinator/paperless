package at.technikum.api.exceptions;

import java.util.UUID;

public class StoredFileNotFoundException extends RuntimeException {

    public StoredFileNotFoundException(UUID documentId) {
        super("No file is stored for document with ID " + documentId + ".");
    }
}
