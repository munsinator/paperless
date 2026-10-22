import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { ErrorAction, toErrorMessage } from '../utils/http-error';

export interface Notification {
    id: number;
    type: 'error' | 'success';
    message: string;
}

const DISPLAY_TIME_MS = 6000;

// zentrale Stelle für Meldungen an den Benutzer (werden als Toast angezeigt)
@Injectable({ providedIn: 'root' })
export class NotificationService {
    private nextId = 1;
    private readonly _notifications = signal<Notification[]>([]);
    readonly notifications = this._notifications.asReadonly();

    error(message: string): void {
        this.show('error', message);
    }

    success(message: string): void {
        this.show('success', message);
    }

    // Fehler aus einem HTTP-Aufruf anzeigen; 403 meldet schon der Interceptor
    httpError(error: unknown, action: ErrorAction): void {
        if (error instanceof HttpErrorResponse && error.status === 403) return;
        this.error(toErrorMessage(error, action));
    }

    dismiss(id: number): void {
        this._notifications.update(list => list.filter(item => item.id !== id));
    }

    private show(type: Notification['type'], message: string): void {
        // gleiche Meldung nicht doppelt anzeigen
        if (this._notifications().some(item => item.type === type && item.message === message)) return;

        const id = this.nextId++;
        this._notifications.update(list => [...list, { id, type, message }]);
        setTimeout(() => this.dismiss(id), DISPLAY_TIME_MS);
    }
}
