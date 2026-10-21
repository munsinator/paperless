import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoggerService } from '../../services/logger.service';

const SOURCE = 'AuthComponent';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly logger = inject(LoggerService);

  readonly isLogin = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  private readonly passwordConfirmationValidator: ValidatorFn = (
    control: AbstractControl
  ): ValidationErrors | null => {
    const password = control.get('password')?.value;
    const repeatPassword = control.get('repeatPassword')?.value;

    if (!repeatPassword) return { repeatPasswordRequired: true };
    return password === repeatPassword ? null : { passwordMismatch: true };
  };

  readonly authForm = this.fb.nonNullable.group({
    username: ['', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(20),
      Validators.pattern(/^\S+$/)
    ]],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/\S/)
    ]],
    repeatPassword: ['']
  });

  setMode(login: boolean): void {
    this.isLogin.set(login);
    this.authForm.setValidators(login ? null : this.passwordConfirmationValidator);
    this.authForm.controls.password.reset('');
    this.authForm.controls.repeatPassword.reset('');
    this.authForm.controls.username.markAsUntouched();
    this.authForm.controls.password.markAsUntouched();
    this.authForm.controls.repeatPassword.markAsUntouched();
    this.authForm.updateValueAndValidity();
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  async onSubmit(): Promise<void> {
    this.authForm.markAllAsTouched();
    if (this.authForm.invalid || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { username, password } = this.authForm.getRawValue();

    try {
      if (this.isLogin()) {
        await this.authService.login({ username, password });
        await this.router.navigateByUrl(this.returnUrl());
      } else {
        const message = await this.authService.register({ username, password });
        this.setMode(true);
        this.successMessage.set(message || 'Account erstellt. Du kannst dich jetzt anmelden.');
      }
    } catch (error) {
      this.logger.warn(SOURCE,`${this.isLogin() ? 'Login' : 'Registration'} failed for '${username}'`);
      this.errorMessage.set(this.toMessage(error));
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private returnUrl(): string {
    const url = this.route.snapshot.queryParamMap.get('returnUrl');
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
