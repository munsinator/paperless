import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { LoggerService } from '../services/logger.service';

const SOURCE = 'AuthGuard';

// CanActivateFn ist eine Funktion, die Angular aufruft, bevor eine Route geöffnet wird
// z.B. verhindern, dass ein unangemeldeter Benutzer /dashboard öffnet
export const authGuard: CanActivateFn = (_route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.isLoggedIn()) return true;

    inject(LoggerService).info(SOURCE,`Not logged in, redirecting from ${state.url} to /auth`);
    return router.createUrlTree(['/auth'], { queryParams: { returnUrl: state.url } });
};