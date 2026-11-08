import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse } from './models';

const STORAGE_KEY = 'alef33.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** The logged-in user (null when logged out), kept across page reloads */
  private readonly session = signal<LoginResponse | null>(this.restore());

  readonly user = this.session.asReadonly();
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.role === 'ADMIN');

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, password })
      .pipe(tap(response => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
        this.session.set(response);
      }));
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    this.router.navigateByUrl('/login');
  }

  token(): string | null {
    return this.session()?.token ?? null;
  }

  /** Reads the saved session, and drops it if the token has expired */
  private restore(): LoginResponse | null {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (!saved) return null;
      const session = JSON.parse(saved) as LoginResponse;
      if (new Date(session.expiresAt).getTime() <= Date.now()) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      return null;
    }
  }
}
