import { HttpErrorResponse } from '@angular/common/http';

export type ErrorAction = 'load' | 'save' | 'delete' | 'upload';

const ACTION_LABEL: Record<ErrorAction, string> = {
    load: 'Das Laden',
    save: 'Das Speichern',
    delete: 'Das Löschen',
    upload: 'Der Upload'
};

export const FORBIDDEN_MESSAGE = 'Dazu hast du keine Berechtigung.';

// übersetzt einen HTTP-Fehler in eine verständliche Meldung für den Benutzer
export function toErrorMessage(error: unknown, action: ErrorAction): string {
    const label = ACTION_LABEL[action];

    if (!(error instanceof HttpErrorResponse)) {
        return `${label} ist fehlgeschlagen. Bitte versuche es erneut.`;
    }

    switch (error.status) {
        case 0:
            return 'Der Server ist nicht erreichbar. Bitte versuche es später erneut.';
        case 400:
        case 422:
            return 'Der Server hat die Eingaben abgelehnt. Bitte prüfe deine Daten.';
        case 401:
            return 'Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.';
        case 403:
            return FORBIDDEN_MESSAGE;
        case 404:
            return action === 'upload'
                ? 'Der Upload-Endpunkt ist noch nicht im Backend implementiert.'
                : 'Das Dokument wurde nicht gefunden. Möglicherweise wurde es bereits gelöscht.';
        case 405:
        case 415:
            return 'Der Upload-Endpunkt ist noch nicht im Backend implementiert.';
        case 413:
            return 'Die Datei ist für den Server zu groß.';
        default:
            return error.status >= 500
                ? `${label} ist wegen eines Serverfehlers fehlgeschlagen.`
                : `${label} ist fehlgeschlagen (HTTP ${error.status}).`;
    }
}
