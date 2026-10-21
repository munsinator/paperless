import { computed, effect, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../environment';
import { AuthDto } from '../models/user.model';
import { LoggerService } from './logger.service';

const TOKEN_KEY = 'token';
const SOURCE = 'AuthService';

interface JwtPayload {
    sub?: string;
    exp?: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    private readonly logger = inject(LoggerService);
    private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
    private readonly apiUrl = `${environment.apiUrl}`;

    private readonly _token = signal<string | null>(this.loadToken());
    readonly token = this._token.asReadonly();

    // loggt automatisch aus, sobald das Token ablaeuft
    // onCleanup: funktion wird vor der nächsten AUsführung des Effects
    // oder beim Zerstören des Effects aufgerufen -> alter Timer würde sonst übrig bleiben
    private readonly expiryTimer = effect(onCleanup => {
        const token = this._token();

        // in SSR gibt es kein localStorage
        if (!this.isBrowser || !token) return;

        const expiration = this.decodePayload(token)?.exp;
        if (expiration === undefined) return;

        // TypeScript-Typ, der den Rückgabewert von setTimeout beschreibt
        let timer: ReturnType<typeof setTimeout>;
        const schedule = () => {
            // remaining = Zeit bis zum Ablauf in ms
            const remaining = expiration * 1000 - Date.now();
            if (remaining <= 0) {
                this.logger.info(SOURCE,'Token expired, logging out');
                this.logout();
                return;
            }
            timer = setTimeout(schedule, Math.min(remaining, 2 ** 31 - 1));
        };
        schedule();
        // clearTimeout(timer): Bricht den Timer ab, der in timer gespeichert ist
        // Wann wird aufgeräumt? Zum Beispiel, wenn sich _token ändert und der Effect erneut ausgeführt wird
        onCleanup(() => clearTimeout(timer));
    });

    private readonly payload = computed(() => {
        const token = this._token();
        return token ? this.decodePayload(token) : null;
    });

    readonly username = computed(() => this.payload()?.sub ?? null);

    readonly isLoggedIn = computed(() => {
        const payload = this.payload();
        if (!payload) return false;
        return payload.exp === undefined || payload.exp * 1000 > Date.now();
    });

    register(credentials: AuthDto): Promise<string> {
        this.logger.info(SOURCE,`Registering user '${credentials.username}'`);
        return firstValueFrom(
            this.http.post(`${this.apiUrl}/users`, credentials, { responseType: 'text' })
        );
    }

    async login(credentials: AuthDto): Promise<void> {
        this.logger.info(SOURCE,`Login attempt for '${credentials.username}'`);
        const { token } = await firstValueFrom(
            this.http.post<{ token: string }>(`${this.apiUrl}/sessions`, credentials)
        );
        this._token.set(token);
        if (this.isBrowser) localStorage.setItem(TOKEN_KEY, token);
        this.logger.info(SOURCE,'Login successful');
    }

    logout(redirect = true): void {
        this.logger.info(SOURCE,'Logged out');
        this._token.set(null);
        if (this.isBrowser) localStorage.removeItem(TOKEN_KEY);
        if (redirect) this.router.navigate(['/auth']);
    }

    private loadToken(): string | null {
        return this.isBrowser ? localStorage.getItem(TOKEN_KEY) : null;
    }

    private decodePayload(token: string): JwtPayload | null {
        try {
            const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
            const json = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map(c => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
                    .join('')
            );
            return JSON.parse(json) as JwtPayload;
        } catch {
            this.logger.warn(SOURCE,'Stored token could not be decoded');
            return null;
        }
    }
}

/**
 * schedule: Funktion, die nach Ablauf des Timers erneut ausgeführt wird
 * Math.min: Timer soll nicht länger als 24,9 Tage warten weil setTimeout das nicht unterstützt
 * 
 */