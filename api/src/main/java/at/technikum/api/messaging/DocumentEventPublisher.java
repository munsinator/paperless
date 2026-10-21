package at.technikum.api.messaging;

public class DocumentEventPublisher {
    private String documentId;

    public DocumentEventPublisher(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
