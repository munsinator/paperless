package at.technikum.api.exceptions;

public class SearchException extends RuntimeException {
    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
