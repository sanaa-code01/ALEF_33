import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

/** The frame around every page: ALEF 33 sidebar on the left, user bar on top */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <img src="logo-alef33.jpg" alt="ALEF 33" class="sidebar-logo" />
        <nav>
          <a routerLink="/clients" routerLinkActive="active">Clients</a>
          <a routerLink="/produits" routerLinkActive="active">Produits</a>
        </nav>
      </aside>

      <div class="main">
        <header class="topbar">
          <span class="user">
            {{ auth.user()?.fullName }}
            <span class="badge" [class.badge-admin]="auth.isAdmin()">
              {{ auth.isAdmin() ? 'Administrateur' : 'Employé' }}
            </span>
          </span>
          <button class="btn btn-ghost" (click)="auth.logout()">Se déconnecter</button>
        </header>
        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
}
