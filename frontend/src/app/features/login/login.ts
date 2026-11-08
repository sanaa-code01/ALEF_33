import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/api-error';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  template: `
    <div class="login-page">
      <form class="card login-card" [formGroup]="form" (ngSubmit)="submit()">
        <img src="logo-alef33.jpg" alt="ALEF 33" class="login-logo" />
        <h1>Gestion commerciale</h1>

        <label>Email
          <input type="email" formControlName="email" autocomplete="username" />
        </label>
        <label>Mot de passe
          <input type="password" formControlName="password" autocomplete="current-password" />
        </label>

        @if (error()) {
          <p class="form-error">{{ error() }}</p>
        }

        <button class="btn btn-primary btn-block" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Connexion…' : 'Se connecter' }}
        </button>
      </form>
    </div>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  constructor() {
    if (this.auth.isLoggedIn()) this.router.navigateByUrl('/');
  }

  submit() {
    const { email, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: err => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
