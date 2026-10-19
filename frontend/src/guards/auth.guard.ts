import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// CanActivateFn ist eine Funktion, die Angular aufruft, bevor eine Route geöffnet wird
export const authGuard: CanActivateFn = (_route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.isLoggedIn()
        ? true
        : router.createUrlTree(['/auth'], { queryParams: { returnUrl: state.url } });
};