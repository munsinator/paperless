package at.technikum.ocrworker.messaging;

public record OcrCompletedEvent(String documentId, String text) {
}
