import { HttpErrorResponse } from '@angular/common/http';
import { ErrorAction, FORBIDDEN_MESSAGE, toErrorMessage } from './http-error';

function httpError(status: number): HttpErrorResponse {
    return new HttpErrorResponse({ status });
}

describe('toErrorMessage', () => {
    it.each<[ErrorAction, string]>([
        ['search', 'Die Suche ist derzeit nicht verfügbar. Bitte versuche es später erneut.'],
        ['upload', 'Der Upload ist derzeit nicht verfügbar. Bitte versuche es später erneut.'],
        ['load', 'Das Laden ist derzeit nicht verfügbar. Bitte versuche es später erneut.']
    ])('names the action for a 503 when %s fails', (action, expected) => {
        expect(toErrorMessage(httpError(503), action)).toBe(expected);
    });

    it('reports an unreachable server for status 0', () => {
        expect(toErrorMessage(httpError(0), 'search')).toContain('nicht erreichbar');
    });

    it('returns the forbidden message for 403', () => {
        expect(toErrorMessage(httpError(403), 'search')).toBe(FORBIDDEN_MESSAGE);
    });

    it('uses the generic server error for other 5xx codes', () => {
        expect(toErrorMessage(httpError(500), 'search')).toBe('Die Suche ist wegen eines Serverfehlers fehlgeschlagen.');
    });

    it('falls back to a generic message for non-HTTP errors', () => {
        expect(toErrorMessage(new Error('boom'), 'search')).toBe('Die Suche ist fehlgeschlagen. Bitte versuche es erneut.');
    });
});
