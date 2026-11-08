import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CustomersService } from '../../core/customers.service';
import { DocumentsService } from '../../core/documents.service';
import { ProductsService } from '../../core/products.service';
import { errorMessage } from '../../core/api-error';
import { Customer, Product, QuoteRequest } from '../../core/models';
import { MoneyPipe } from '../../shared/money.pipe';
import { ToastService } from '../../shared/toast.service';

interface EditableLine {
  productId: number | null;
  designation: string;
  quantity: number;
  unitPriceTTC: number;
  vatRate: number;
}

/** Same rounding as the backend's PricingService */
function round2(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}
function lineTTC(l: EditableLine): number {
  return round2((Number(l.unitPriceTTC) || 0) * (Number(l.quantity) || 0));
}
function lineHT(l: EditableLine): number {
  return round2(lineTTC(l) / (1 + (Number(l.vatRate) || 0) / 100));
}

@Component({
  selector: 'app-quote-form',
  imports: [FormsModule, RouterLink, MoneyPipe],
  template: `
    <div class="page-header">
      <h1>Nouveau devis</h1>
      <a class="btn btn-ghost" routerLink="/devis">← Retour aux devis</a>
    </div>

    <div class="card section">
      <div class="grid-3">
        <label>Client *
          <select [ngModel]="customerId()" (ngModelChange)="customerId.set($event)">
            <option [ngValue]="null" disabled>Choisir un client…</option>
            @for (c of customers(); track c.id) {
              <option [ngValue]="c.id">{{ c.name }}{{ c.city ? ' (' + c.city + ')' : '' }}</option>
            }
          </select>
        </label>
        <label>Valable jusqu'au
          <input type="date" [ngModel]="validUntil()" (ngModelChange)="validUntil.set($event)" />
        </label>
        <label>Remarques
          <input [ngModel]="notes()" (ngModelChange)="notes.set($event)" placeholder="Ex : Livraison sous 3 semaines" />
        </label>
      </div>
    </div>

    <div class="card section">
      <h2>Lignes</h2>
      <table class="table lines-table">
        <thead>
          <tr>
            <th style="width: 24%">Produit</th>
            <th>Désignation *</th>
            <th class="num" style="width: 8%">Qté</th>
            <th class="num" style="width: 13%">P.U TTC (DH)</th>
            <th class="num" style="width: 8%">TVA %</th>
            <th class="num" style="width: 13%">Total TTC</th>
            <th style="width: 4%"></th>
          </tr>
        </thead>
        <tbody>
          @for (line of lines(); track $index; let i = $index) {
            <tr>
              <td>
                <select [ngModel]="line.productId" (ngModelChange)="pickProduct(i, $event)">
                  <option [ngValue]="null">Ligne libre</option>
                  @for (p of products(); track p.id) {
                    <option [ngValue]="p.id">{{ p.reference }} - {{ p.designation }}</option>
                  }
                </select>
              </td>
              <td><input [ngModel]="line.designation" (ngModelChange)="update(i, { designation: $event })" /></td>
              <td><input class="num" type="number" min="1" [ngModel]="line.quantity" (ngModelChange)="update(i, { quantity: $event })" /></td>
              <td><input class="num" type="number" min="0" step="0.01" [ngModel]="line.unitPriceTTC" (ngModelChange)="update(i, { unitPriceTTC: $event })" /></td>
              <td><input class="num" type="number" min="0" max="100" [ngModel]="line.vatRate" (ngModelChange)="update(i, { vatRate: $event })" /></td>
              <td class="num strong">{{ lineTotal(line) | money }}</td>
              <td>
                @if (lines().length > 1) {
                  <button class="btn btn-small btn-danger" title="Supprimer la ligne" (click)="removeLine(i)">✕</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
      <button class="btn" (click)="addLine()">+ Ajouter une ligne</button>

      <div class="totals">
        <div><span>Total HT</span><strong>{{ totals().ht | money }} DH</strong></div>
        <div><span>TVA</span><strong>{{ totals().vat | money }} DH</strong></div>
        <div class="total-ttc"><span>Total TTC</span><strong>{{ totals().ttc | money }} DH</strong></div>
      </div>
    </div>

    @if (error()) {
      <p class="form-error">{{ error() }}</p>
    }
    <div class="form-footer">
      <a class="btn btn-ghost" routerLink="/devis">Annuler</a>
      <button class="btn btn-primary" [disabled]="!canSave() || saving()" (click)="save()">
        {{ saving() ? 'Enregistrement…' : 'Créer le devis' }}
      </button>
    </div>
  `,
})
export class QuoteForm {
  private readonly documents = inject(DocumentsService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly customers = signal<Customer[]>([]);
  protected readonly products = signal<Product[]>([]);

  protected readonly customerId = signal<number | null>(null);
  protected readonly validUntil = signal<string>('');
  protected readonly notes = signal('');
  protected readonly lines = signal<EditableLine[]>([this.emptyLine()]);

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Live totals, calculated exactly like the backend */
  protected readonly totals = computed(() => {
    const ttc = round2(this.lines().reduce((sum, l) => sum + lineTTC(l), 0));
    const ht = round2(this.lines().reduce((sum, l) => sum + lineHT(l), 0));
    return { ttc, ht, vat: round2(ttc - ht) };
  });

  protected readonly canSave = computed(() =>
    this.customerId() !== null &&
    this.lines().length > 0 &&
    this.lines().every(l => l.designation.trim() !== '' && Number(l.quantity) >= 1 && Number(l.unitPriceTTC) >= 0),
  );

  constructor() {
    inject(CustomersService).list().subscribe(list => this.customers.set(list));
    inject(ProductsService).list(true).subscribe(list => this.products.set(list));
  }

  protected lineTotal(line: EditableLine) {
    return lineTTC(line);
  }

  protected addLine() {
    this.lines.update(list => [...list, this.emptyLine()]);
  }

  protected removeLine(index: number) {
    this.lines.update(list => list.filter((_, i) => i !== index));
  }

  protected update(index: number, change: Partial<EditableLine>) {
    this.lines.update(list => list.map((l, i) => (i === index ? { ...l, ...change } : l)));
  }

  /** Choosing a product fills the name, TTC price and VAT (they can still be changed) */
  protected pickProduct(index: number, productId: number | null) {
    const product = this.products().find(p => p.id === productId);
    this.update(index, product
      ? { productId: product.id, designation: product.designation, unitPriceTTC: product.unitPriceTTC, vatRate: product.vatRate }
      : { productId: null });
  }

  protected save() {
    const body: QuoteRequest = {
      customerId: this.customerId()!,
      validUntil: this.validUntil() || null,
      notes: this.notes().trim() || null,
      lines: this.lines().map(l => ({
        productId: l.productId,
        designation: l.designation.trim(),
        quantity: Number(l.quantity),
        unitPriceTTC: Number(l.unitPriceTTC),
        vatRate: Number(l.vatRate),
      })),
    };
    this.saving.set(true);
    this.error.set(null);
    this.documents.createQuote(body).subscribe({
      next: quote => {
        this.toast.success(`Devis ${quote.number} créé`);
        this.router.navigate(['/devis', quote.id]);
      },
      error: err => {
        this.error.set(errorMessage(err));
        this.saving.set(false);
      },
    });
  }

  private emptyLine(): EditableLine {
    return { productId: null, designation: '', quantity: 1, unitPriceTTC: 0, vatRate: 20 };
  }
}
