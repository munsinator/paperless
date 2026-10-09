import { computed, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../environment';
import { AuthDto } from '../models/user.model';

const TOKEN_KEY = 'token';

interface JwtPayload {
    sub?: string;
    exp?: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
    private readonly apiUrl = `${environment.apiUrl}`;

    private readonly _token = signal<string | null>(this.loadToken());
    readonly token = this._token.asReadonly();

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
        return firstValueFrom(
            this.http.post(`${this.apiUrl}/users`, credentials, { responseType: 'text' })
        );
    }

    async login(credentials: AuthDto): Promise<void> {
        const { token } = await firstValueFrom(
            this.http.post<{ token: string }>(`${this.apiUrl}/sessions `, credentials)
        );
        this._token.set(token);
        if (this.isBrowser) localStorage.setItem(TOKEN_KEY, token);
    }

    logout(redirect = true): void {
        this._token.set(null);
        if (this.isBrowser) localStorage.removeItem(TOKEN_KEY);
        if (redirect) this.router.navigate(['/login']);
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
            return null;
        }
    }
}