package at.technikum.api.service;

import java.util.UUID;

/**
 * Wird nach dem Speichern oder Ändern eines Dokuments veröffentlicht.
 * Der Listener schreibt den Titel erst nach dem Commit in den Suchindex.
 */
public record DocumentTitleChangedEvent(UUID documentId, String title) {
}
