import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { ProductsService } from '../../core/products.service';
import { errorMessage } from '../../core/api-error';
import { Product, ProductRequest } from '../../core/models';
import { MoneyPipe } from '../../shared/money.pipe';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-products',
  imports: [ReactiveFormsModule, MoneyPipe],
  template: `
    <div class="page-header">
      <h1>Produits</h1>
      @if (auth.isAdmin()) {
        <button class="btn btn-primary" (click)="openForm()">+ Nouveau produit</button>
      }
    </div>

    <div class="toolbar">
      <label class="checkbox">
        <input type="checkbox" [checked]="activeOnly()" (change)="toggleActiveOnly()" />
        Seulement les produits actifs
      </label>
    </div>

    <div class="card">
      @if (loading()) {
        <p class="muted">Chargement…</p>
      } @else if (products().length === 0) {
        <p class="muted">Aucun produit.</p>
      } @else {
        <table class="table">
          <thead>
            <tr>
              <th>Référence</th><th>Désignation</th>
              <th class="num">Prix TTC</th><th class="num">Prix HT</th><th class="num">TVA</th>
              <th>Statut</th><th></th>
            </tr>
          </thead>
          <tbody>
            @for (p of products(); track p.id) {
              <tr [class.inactive]="!p.active">
                <td class="mono">{{ p.reference }}</td>
                <td class="strong">{{ p.designation }}</td>
                <td class="num strong">{{ p.unitPriceTTC | money }}</td>
                <td class="num muted">{{ p.unitPriceHT | money }}</td>
                <td class="num">{{ p.vatRate }} %</td>
                <td>
                  <span class="badge" [class.badge-ok]="p.active">{{ p.active ? 'Actif' : 'Désactivé' }}</span>
                </td>
                <td class="actions">
                  @if (auth.isAdmin()) {
                    <button class="btn btn-small" (click)="openForm(p)">Modifier</button>
                    @if (p.active) {
                      <button class="btn btn-small btn-danger" (click)="deactivate(p)">Désactiver</button>
                    }
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
          <h2>{{ editing() ? 'Modifier le produit' : 'Nouveau produit' }}</h2>
          <div class="grid-2">
            <label>Référence *<input formControlName="reference" /></label>
            <label>TVA (%)<input type="number" formControlName="vatRate" min="0" max="100" /></label>
          </div>
          <label>Désignation *<input formControlName="designation" /></label>
          <label>Description<textarea rows="2" formControlName="description"></textarea></label>
          <label>Prix unitaire TTC (DH) *<input type="number" formControlName="unitPriceTTC" min="0" step="0.01" /></label>
          <p class="hint">Prix HT calculé : <strong>{{ priceHT() | money }} DH</strong></p>
          <label class="checkbox"><input type="checkbox" formControlName="active" /> Produit actif</label>
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
export class Products {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ProductsService);
  private readonly toast = inject(ToastService);

  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly activeOnly = signal(false);

  protected readonly formOpen = signal(false);
  protected readonly editing = signal<Product | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    reference: ['', [Validators.required, Validators.maxLength(50)]],
    designation: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    unitPriceTTC: [0, [Validators.required, Validators.min(0)]],
    vatRate: [20, [Validators.min(0), Validators.max(100)]],
    active: [true],
  });

  /** Same calculation as the backend: HT = TTC ÷ (1 + TVA) */
  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  protected readonly priceHT = computed(() => {
    const v = this.formValue();
    const ttc = Number(v.unitPriceTTC) || 0;
    const rate = Number(v.vatRate) || 0;
    return Math.round((ttc / (1 + rate / 100)) * 100) / 100;
  });

  constructor() {
    this.load();
  }

  protected toggleActiveOnly() {
    this.activeOnly.update(v => !v);
    this.load();
  }

  private load() {
    this.loading.set(true);
    this.api.list(this.activeOnly()).subscribe({
      next: list => {
        this.products.set(list);
        this.loading.set(false);
      },
      error: err => {
        this.toast.error(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  protected openForm(product?: Product) {
    this.editing.set(product ?? null);
    this.formError.set(null);
    this.form.reset({
      reference: product?.reference ?? '',
      designation: product?.designation ?? '',
      description: product?.description ?? '',
      unitPriceTTC: product?.unitPriceTTC ?? 0,
      vatRate: product?.vatRate ?? 20,
      active: product?.active ?? true,
    });
    this.formOpen.set(true);
  }

  protected closeForm() {
    this.formOpen.set(false);
  }

  protected save() {
    const v = this.form.getRawValue();
    const body: ProductRequest = {
      reference: v.reference.trim(),
      designation: v.designation.trim(),
      description: v.description || null,
      unitPriceTTC: Number(v.unitPriceTTC),
      vatRate: v.vatRate === null ? null : Number(v.vatRate),
      active: v.active,
    };
    const current = this.editing();
    const request = current ? this.api.update(current.id, body) : this.api.create(body);

    this.saving.set(true);
    request.subscribe({
      next: () => {
        this.toast.success(current ? 'Produit modifié' : 'Produit créé');
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

  protected deactivate(product: Product) {
    if (!confirm(`Désactiver « ${product.designation} » ? Il ne pourra plus être ajouté aux nouveaux devis.`)) return;
    this.api.deactivate(product.id).subscribe({
      next: () => {
        this.toast.success('Produit désactivé');
        this.load();
      },
      error: err => this.toast.error(errorMessage(err)),
    });
  }
}
