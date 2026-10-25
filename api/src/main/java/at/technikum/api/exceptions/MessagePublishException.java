package at.technikum.api.exceptions;

public class MessagePublishException extends RuntimeException{
    public MessagePublishException(String message, Throwable exception){
        super(message,exception);
    }
}
