import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { CustomersService } from '../../core/customers.service';
import { errorMessage } from '../../core/api-error';
import { Customer, CustomerRequest } from '../../core/models';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-customers',
  imports: [ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Clients</h1>
      <button class="btn btn-primary" (click)="openForm()">+ Nouveau client</button>
    </div>

    <div class="toolbar">
      <input class="search" placeholder="Rechercher un client…" [value]="search()"
             (input)="onSearch($any($event.target).value)" />
    </div>

    <div class="card">
      @if (loading()) {
        <p class="muted">Chargement…</p>
      } @else if (customers().length === 0) {
        <p class="muted">Aucun client.</p>
      } @else {
        <table class="table">
          <thead>
            <tr><th>Nom</th><th>Ville</th><th>Téléphone</th><th>Email</th><th>ICE</th><th></th></tr>
          </thead>
          <tbody>
            @for (c of customers(); track c.id) {
              <tr>
                <td class="strong">{{ c.name }}</td>
                <td>{{ c.city }}</td>
                <td class="nowrap">{{ c.phone }}</td>
                <td>{{ c.email }}</td>
                <td>{{ c.ice }}</td>
                <td class="actions">
                  <button class="btn btn-small" (click)="openForm(c)">Modifier</button>
                  @if (auth.isAdmin()) {
                    <button class="btn btn-small btn-danger" (click)="remove(c)">Supprimer</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>

    @if (formOpen()) {
      <div class="modal-backdrop" (click)="closeForm()">
        <form class="card modal" [formGroup]="form" (ngSubmit)="save()" (click)="$event.stopPropagation()">
          <h2>{{ editing() ? 'Modifier le client' : 'Nouveau client' }}</h2>
          <label>Nom *<input formControlName="name" /></label>
          <div class="grid-2">
            <label>Téléphone<input formControlName="phone" /></label>
            <label>Email<input type="email" formControlName="email" /></label>
          </div>
          <label>Adresse<input formControlName="address" /></label>
          <div class="grid-2">
            <label>Ville<input formControlName="city" /></label>
            <label>ICE<input formControlName="ice" /></label>
          </div>
          @if (formError()) {
            <p class="form-error">{{ formError() }}</p>
          }
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" (click)="closeForm()">Annuler</button>
            <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving()">Enregistrer</button>
          </div>
        </form>
      </div>
    }
  `,
})
export class Customers {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(CustomersService);
  private readonly toast = inject(ToastService);

  protected readonly customers = signal<Customer[]>([]);
  protected readonly loading = signal(true);
  protected readonly search = signal('');

  protected readonly formOpen = signal(false);
  protected readonly editing = signal<Customer | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    phone: [''],
    email: ['', Validators.email],
    address: [''],
    city: [''],
    ice: ['', Validators.maxLength(20)],
  });

  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.load();
  }

  protected onSearch(value: string) {
    this.search.set(value);
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.load(), 300);   // wait until the user stops typing
  }

  private load() {
    this.loading.set(true);
    this.api.list(this.search().trim()).subscribe({
      next: list => {
        this.customers.set(list);
        this.loading.set(false);
      },
      error: err => {
        this.toast.error(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  protected openForm(customer?: Customer) {
    this.editing.set(customer ?? null);
    this.formError.set(null);
    this.form.reset({
      name: customer?.name ?? '',
      phone: customer?.phone ?? '',
      email: customer?.email ?? '',
      address: customer?.address ?? '',
      city: customer?.city ?? '',
      ice: customer?.ice ?? '',
    });
    this.formOpen.set(true);
  }

  protected closeForm() {
    this.formOpen.set(false);
  }

  protected save() {
    const v = this.form.getRawValue();
    const body: CustomerRequest = {
      name: v.name.trim(),
      phone: v.phone || null,
      email: v.email || null,
      address: v.address || null,
      city: v.city || null,
      ice: v.ice || null,
    };
    const current = this.editing();
    const request = current ? this.api.update(current.id, body) : this.api.create(body);

    this.saving.set(true);
    request.subscribe({
      next: () => {
        this.toast.success(current ? 'Client modifié' : 'Client créé');
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

  protected remove(customer: Customer) {
    if (!confirm(`Supprimer le client « ${customer.name} » ?`)) return;
    this.api.delete(customer.id).subscribe({
      next: () => {
        this.toast.success('Client supprimé');
        this.load();
      },
      error: err => this.toast.error(errorMessage(err)),   // e.g. "has quotes and cannot be deleted"
    });
  }
}
