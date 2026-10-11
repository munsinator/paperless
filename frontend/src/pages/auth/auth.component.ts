import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // UI-Zustand
  readonly isLogin = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Formularfelder (werden von Login und Register gemeinsam genutzt)
  readonly username = signal('');
  readonly password = signal('');
  readonly repeatPassword = signal('');

  // Abgeleiteter Zustand
  readonly passwordsMatch = computed(() => this.password() === this.repeatPassword());

  readonly canLogin = computed(() =>
    this.username().trim().length > 0 &&
    this.password().length > 0 &&
    !this.isSubmitting()
  );

  readonly canRegister = computed(() =>
    this.username().trim().length >= 3 &&
    this.password().length >= 8 &&
    this.passwordsMatch() &&
    !this.isSubmitting()
  );

  setMode(login: boolean): void {
    this.isLogin.set(login);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.password.set('');
    this.repeatPassword.set('');
  }

  async onLogin(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.canLogin()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    try {
      await this.authService.login({
        username: this.username().trim(),
        password: this.password()
      });
      await this.router.navigateByUrl(this.returnUrl());
    } catch (error) {
      this.errorMessage.set(this.toMessage(error));
    } finally {
      this.isSubmitting.set(false);
    }
  }

  async onRegister(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.canRegister()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    try {
      const message = await this.authService.register({
        username: this.username().trim(),
        password: this.password()
      });
      // Zurück zum Login-Tab, Benutzername bleibt eingetragen
      this.isLogin.set(true);
      this.password.set('');
      this.repeatPassword.set('');
      this.successMessage.set(message || 'Account erstellt. Du kannst dich jetzt anmelden.');
    } catch (error) {
      this.errorMessage.set(this.toMessage(error));
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private returnUrl(): string {
    const url = this.route.snapshot.queryParamMap.get('returnUrl');
    // nur interne Pfade zulassen (gegen Open Redirect)
    return url && url.startsWith('/') && !url.startsWith('//') ? url : '/dashboard';
  }

  private toMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) return 'Server nicht erreichbar.';
      if (this.isLogin() && (error.status === 401 || error.status === 403)) {
        return 'Benutzername oder Passwort falsch.';
      }
      if (!this.isLogin() && error.status === 409) {
        return 'Dieser Benutzername ist bereits vergeben.';
      }
    }
    return this.isLogin()
      ? 'Anmeldung fehlgeschlagen. Bitte versuche es erneut.'
      : 'Registrierung fehlgeschlagen. Bitte versuche es erneut.';
  }
}