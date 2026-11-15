package at.technikum.api.service;

import java.util.UUID;

/**
 * Wird nach dem Löschen eines Dokuments veröffentlicht.
 * Der Listener entfernt den Eintrag erst nach dem Commit aus dem Suchindex.
 */
public record DocumentDeletedEvent(UUID documentId) {
}
