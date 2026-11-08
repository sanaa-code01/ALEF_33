import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { UsersService } from '../../core/users.service';
import { errorMessage } from '../../core/api-error';
import { Role, User } from '../../core/models';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Utilisateurs</h1>
      <button class="btn btn-primary" (click)="openForm()">+ Nouvel utilisateur</button>
    </div>
    <p class="muted page-help">
      Les <strong>administrateurs</strong> gèrent tout. Les <strong>employés</strong> créent devis, commandes, factures
      et règlements, mais ne modifient pas les prix et ne suppriment rien.
    </p>

    <div class="card">
      <table class="table">
        <thead><tr><th>Nom</th><th>Email</th><th>Rôle</th><th>Statut</th><th></th></tr></thead>
        <tbody>
          @for (u of users(); track u.id) {
            <tr [class.inactive]="!u.enabled">
              <td class="strong">{{ u.fullName }}</td>
              <td>{{ u.email }}</td>
              <td><span class="badge" [class.badge-admin]="u.role === 'ADMIN'">{{ u.role === 'ADMIN' ? 'Administrateur' : 'Employé' }}</span></td>
              <td><span class="badge" [class.badge-ok]="u.enabled" [class.badge-ko]="!u.enabled">{{ u.enabled ? 'Actif' : 'Désactivé' }}</span></td>
              <td class="actions">
                @if (u.email !== auth.user()?.email) {
                  <button class="btn btn-small" [class.btn-danger]="u.enabled" (click)="toggle(u)">
                    {{ u.enabled ? 'Désactiver' : 'Réactiver' }}
                  </button>
                } @else {
                  <span class="muted">(vous)</span>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    @if (formOpen()) {
      <div class="modal-backdrop" (click)="formOpen.set(false)">
        <form class="card modal" [formGroup]="form" (ngSubmit)="save()" (click)="$event.stopPropagation()">
          <h2>Nouvel utilisateur</h2>
          <label>Nom complet *<input formControlName="fullName" /></label>
          <label>Email *<input type="email" formControlName="email" autocomplete="off" /></label>
          <label>Mot de passe * (8 caractères minimum)
            <input type="password" formControlName="password" autocomplete="new-password" />
          </label>
          <label>Rôle *
            <select formControlName="role">
              <option value="EMPLOYEE">Employé</option>
              <option value="ADMIN">Administrateur</option>
            </select>
          </label>
          @if (formError()) {
            <p class="form-error">{{ formError() }}</p>
          }
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="formOpen.set(false)">Annuler</button>
            <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">Créer le compte</button>
          </div>
        </form>
      </div>
    }
  `,
})
export class Users {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(UsersService);
  private readonly toast = inject(ToastService);

  protected readonly users = signal<User[]>([]);
  protected readonly formOpen = signal(false);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['EMPLOYEE' as Role, Validators.required],
  });

  constructor() {
    this.load();
  }

  private load() {
    this.api.list().subscribe({
      next: list => this.users.set(list),
      error: err => this.toast.error(errorMessage(err)),
    });
  }

  protected openForm() {
    this.form.reset({ fullName: '', email: '', password: '', role: 'EMPLOYEE' });
    this.formError.set(null);
    this.formOpen.set(true);
  }

  protected save() {
    this.saving.set(true);
    this.api.create(this.form.getRawValue()).subscribe({
      next: u => {
        this.toast.success(`Compte créé pour ${u.fullName}`);
        this.saving.set(false);
        this.formOpen.set(false);
        this.load();
      },
      error: err => {
        this.formError.set(errorMessage(err));
        this.saving.set(false);
      },
    });
  }

  protected toggle(u: User) {
    const enable = !u.enabled;
    if (!enable && !confirm(`Désactiver le compte de ${u.fullName} ? Il ne pourra plus se connecter.`)) return;
    this.api.setEnabled(u.id, enable).subscribe({
      next: () => {
        this.toast.success(enable ? 'Compte réactivé' : 'Compte désactivé');
        this.load();
      },
      error: err => this.toast.error(errorMessage(err)),
    });
  }
}
