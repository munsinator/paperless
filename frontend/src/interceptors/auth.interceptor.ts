import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { environment } from '../environment';
import { FORBIDDEN_MESSAGE } from '../utils/http-error';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
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

        // next(authReq) schickt die HTTP-Anfrage weiter und liefert ein Observable zurück.
        // .pipe() ermöglicht es, dieses Observable zu bearbeiten
    return next(authReq).pipe(
        catchError((err: HttpErrorResponse) => {
            if (isBackendRequest && !isAuthRequest) {
                if (err.status === 401) {
                    // Token ungültig oder abgelaufen -> abmelden
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
