import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { environment } from '../environment';
import { FORBIDDEN_MESSAGE } from '../utils/http-error';
import { LoggerService } from '../services/logger.service';

const SOURCE = 'AuthInterceptor';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const logger = inject(LoggerService);
    const authService = inject(AuthService);
    const notifications = inject(NotificationService);
    const token = authService.token();
    const apiUrl = environment.apiUrl.replace(/\/$/, '');

    // checkt, ob die Anfrage an das Backend geht (nicht an externe APIs)
    const isBackendRequest = req.url.startsWith(`${apiUrl}/`) || req.url === apiUrl;

    // checkt, ob es zu auth Endpunkt geht, da logout keinen Sinn macht
    const isAuthRequest = [`${apiUrl}/users`, `${apiUrl}/sessions`]
        .some(path => req.url === path || req.url.startsWith(`${path}/`));

    const authReq = token && isBackendRequest
        ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : req;

    if (isBackendRequest) logger.debug(SOURCE, `${req.method} ${req.url}`);

    // next(authReq) schickt die HTTP-Anfrage weiter und liefert ein Observable zurück.
        // .pipe() ermöglicht es, dieses Observable zu bearbeiten
    return next(authReq).pipe(
        tap(event => {
            if (isBackendRequest && event instanceof HttpResponse) {
                logger.debug(SOURCE, `${req.method} ${req.url} -> ${event.status}`);
            }
        }),
        catchError((err: HttpErrorResponse) => {
            if (isBackendRequest) {
                // 4xx sind erwartbare Fehler (warn), Netzwerkfehler und 5xx sind echte Fehler (error)
                const level = err.status >= 400 && err.status < 500 ? 'warn' : 'error';
                logger[level](SOURCE, `${req.method} ${req.url} failed with status ${err.status}`);
            }
            if (isBackendRequest && !isAuthRequest) {
                if (err.status === 401) {
                    // Token ungültig oder abgelaufen -> abmelden
                    logger.info(SOURCE, 'Session rejected by server, logging out');
                    authService.logout();
                } else if (err.status === 403) {
                    // angemeldet, aber keine Rechte -> Meldung, aber kein Logout
                    notifications.error(FORBIDDEN_MESSAGE);
                }
            }
            return throwError(() => err);
        })
    );
};
