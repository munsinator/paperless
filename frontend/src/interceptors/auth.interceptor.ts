import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const token = authService.token();
    const apiUrl = environment.apiUrl.replace(/\/$/, '');
    const isBackendRequest = req.url.startsWith(`${apiUrl}/`) || req.url === apiUrl;
    const isAuthRequest = [`${apiUrl}/users`, `${apiUrl}/sessions`]
        .some(path => req.url === path || req.url.startsWith(`${path}/`));

    const authReq = token && isBackendRequest
        ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : req;

    return next(authReq).pipe(
        catchError((err: HttpErrorResponse) => {
            if (err.status === 401 && isBackendRequest && !isAuthRequest) {
                authService.logout();
            }
            return throwError(() => err);
        })
    );
};