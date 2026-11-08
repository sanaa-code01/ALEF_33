import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { DocumentsService } from '../../core/documents.service';
import { errorMessage } from '../../core/api-error';
import { Invoice, Order, OrderStatus, Payment, PaymentMethod, PaymentRequest, PaymentType } from '../../core/models';
import { FrDatePipe, today } from '../../shared/date.pipe';
import { INVOICE_STATUS, ORDER_STATUS, PAYMENT_METHOD, PAYMENT_TYPE } from '../../shared/labels';
import { LinesTable } from '../../shared/lines-table';
import { MoneyPipe } from '../../shared/money.pipe';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-order-detail',
  imports: [RouterLink, ReactiveFormsModule, MoneyPipe, FrDatePipe, LinesTable],
  template: `
    <div class="page-header">
      <a class="btn btn-ghost" routerLink="/commandes">← Retour aux commandes</a>
    </div>

    @if (order(); as o) {
      <!-- Header, status and invoice -->
      <div class="card section">
        <div class="doc-header">
          <div>
            <h1>Commande {{ o.number }}</h1>
            <p class="muted">
              {{ o.customerName }} · du {{ o.orderDate | frDate }}
              @if (o.quoteNumber) { · devis {{ o.quoteNumber }} }
              @if (o.deliveryDate) { · livrée le {{ o.deliveryDate | frDate }} }
            </p>
          </div>
          <span class="badge badge-big {{ orderStatus[o.status].css }}">{{ orderStatus[o.status].label }}</span>
        </div>

        <div class="actions-bar">
          @if (o.status === 'PENDING') {
            <button class="btn btn-primary" (click)="changeStatus('IN_PRODUCTION')">Lancer la fabrication</button>
          }
          @if (o.status === 'IN_PRODUCTION') {
            <button class="btn btn-primary" (click)="changeStatus('DELIVERED')">Marquer comme livrée</button>
          }
          @if (o.status === 'PENDING' || o.status === 'IN_PRODUCTION') {
            <button class="btn btn-danger" (click)="changeStatus('CANCELLED')">Annuler la commande</button>
          }

          @if (invoice(); as inv) {
            <span class="invoice-chip">
              Facture {{ inv.number }}
              <span class="badge {{ invoiceStatus[inv.status].css }}">{{ invoiceStatus[inv.status].label }}</span>
            </span>
            <button class="btn" (click)="downloadInvoice(inv)">⬇ PDF de la facture</button>
          } @else if (o.status !== 'CANCELLED') {
            <button class="btn" (click)="createInvoice()">Créer la facture</button>
          }
        </div>
      </div>

      <div class="two-columns">
        <!-- Lines and totals -->
        <div class="card section">
          <h2>Articles</h2>
          <app-lines-table [lines]="o.lines" />
          <div class="totals">
            <div><span>Total HT</span><strong>{{ o.totalHT | money }} DH</strong></div>
            <div><span>TVA</span><strong>{{ o.totalVat | money }} DH</strong></div>
            <div class="total-ttc"><span>Total TTC</span><strong>{{ o.totalTTC | money }} DH</strong></div>
          </div>
        </div>

        <!-- Payments -->
        <div class="card section">
          <h2>Règlements</h2>
          <div class="progress"><div class="progress-bar" [style.width.%]="paidPercent()"></div></div>
          <div class="paid-summary">
            <span>Payé : <strong>{{ o.paid | money }} DH</strong></span>
            <span>Reste : <strong [class.text-danger]="o.remaining > 0">{{ o.remaining | money }} DH</strong></span>
          </div>

          @if (payments().length === 0) {
            <p class="muted">Aucun règlement.</p>
          } @else {
            <table class="table compact">
              <tbody>
                @for (p of payments(); track p.id) {
                  <tr>
                    <td>{{ p.paymentDate | frDate }}</td>
                    <td>{{ paymentType[p.type] }}</td>
                    <td>{{ paymentMethod[p.method] }}{{ p.reference ? ' · ' + p.reference : '' }}</td>
                    <td class="num strong">{{ p.amount | money }}</td>
                    @if (auth.isAdmin()) {
                      <td class="actions"><button class="btn btn-small btn-danger" title="Supprimer" (click)="deletePayment(p)">✕</button></td>
                    }
                  </tr>
                }
              </tbody>
            </table>
          }

          @if (o.remaining > 0 && o.status !== 'CANCELLED') {
            <form class="payment-form" [formGroup]="paymentForm" (ngSubmit)="addPayment()">
              <h3>Nouveau règlement</h3>
              <div class="grid-2">
                <label>Montant (DH) *
                  <input type="number" min="0.01" step="0.01" formControlName="amount" />
                </label>
                <label>Date
                  <input type="date" formControlName="paymentDate" />
                </label>
                <label>Mode *
                  <select formControlName="method">
                    @for (m of methods; track m) { <option [value]="m">{{ paymentMethod[m] }}</option> }
                  </select>
                </label>
                <label>Type
                  <select formControlName="type">
                    <option value="">Automatique</option>
                    @for (t of types; track t) { <option [value]="t">{{ paymentType[t] }}</option> }
                  </select>
                </label>
              </div>
              <label>Référence (n° de chèque, virement…)<input formControlName="reference" /></label>
              <div class="quick-amounts">
                <button type="button" class="btn btn-small" (click)="setAmount(Math.min(o.totalTTC * 0.3, o.remaining))">Acompte 30 %</button>
                <button type="button" class="btn btn-small" (click)="setAmount(o.remaining)">Tout le reste</button>
              </div>
              <button class="btn btn-primary btn-block" type="submit" [disabled]="paymentForm.invalid || saving()">
                Enregistrer le règlement
              </button>
            </form>
          }
        </div>
      </div>
    } @else {
      <p class="muted">Chargement…</p>
    }
  `,
})
export class OrderDetail implements OnInit {
  /** The ":id" of the URL /commandes/:id */
  readonly id = input.required<string>();

  protected readonly auth = inject(AuthService);
  private readonly api = inject(DocumentsService);
  private readonly toast = inject(ToastService);

  protected readonly orderStatus = ORDER_STATUS;
  protected readonly invoiceStatus = INVOICE_STATUS;
  protected readonly paymentMethod = PAYMENT_METHOD;
  protected readonly paymentType = PAYMENT_TYPE;
  protected readonly methods = Object.keys(PAYMENT_METHOD) as PaymentMethod[];
  protected readonly types = Object.keys(PAYMENT_TYPE) as PaymentType[];

  protected readonly order = signal<Order | null>(null);
  protected readonly payments = signal<Payment[]>([]);
  protected readonly invoice = signal<Invoice | null>(null);
  protected readonly saving = signal(false);
  protected readonly Math = Math;

  protected readonly paidPercent = computed(() => {
    const o = this.order();
    return o && o.totalTTC > 0 ? Math.min(100, (o.paid / o.totalTTC) * 100) : 0;
  });

  protected readonly paymentForm = inject(FormBuilder).nonNullable.group({
    amount: [0, [Validators.required, Validators.min(0.01)]],
    paymentDate: [today()],
    method: ['CASH' as PaymentMethod, Validators.required],
    type: [''],
    reference: [''],
  });

  ngOnInit() {
    this.reload();
  }

  /** Order, its payments and its invoice (if any), loaded together */
  private reload() {
    const id = +this.id();
    forkJoin({
      order: this.api.order(id),
      payments: this.api.payments(id),
      invoices: this.api.invoices(),
    }).subscribe({
      next: ({ order, payments, invoices }) => {
        this.order.set(order);
        this.payments.set(payments);
        this.invoice.set(invoices.find(i => i.orderId === id) ?? null);
      },
      error: err => this.toast.error(errorMessage(err)),
    });
  }

  protected changeStatus(status: OrderStatus) {
    if (status === 'CANCELLED' && !confirm('Annuler cette commande ? Cette action est définitive.')) return;
    this.api.changeOrderStatus(+this.id(), status).subscribe({
      next: () => {
        this.toast.success(`Commande : ${ORDER_STATUS[status].label.toLowerCase()}`);
        this.reload();
      },
      error: err => this.toast.error(errorMessage(err)),
    });
  }

  protected createInvoice() {
    this.api.createInvoice(+this.id()).subscribe({
      next: inv => {
        this.toast.success(`Facture ${inv.number} créée`);
        this.reload();
      },
      error: err => this.toast.error(errorMessage(err)),
    });
  }

  protected downloadInvoice(inv: Invoice) {
    this.api.downloadPdf('invoices', inv.id, inv.number).subscribe({
      error: err => this.toast.error(errorMessage(err)),
    });
  }

  protected setAmount(value: number) {
    this.paymentForm.controls.amount.setValue(Math.round(value * 100) / 100);
  }

  protected addPayment() {
    const v = this.paymentForm.getRawValue();
    const body: PaymentRequest = {
      amount: Number(v.amount),
      paymentDate: v.paymentDate || null,
      method: v.method,
      type: (v.type || null) as PaymentType | null,
      reference: v.reference.trim() || null,
      notes: null,
    };
    this.saving.set(true);
    this.api.addPayment(+this.id(), body).subscribe({
      next: p => {
        this.toast.success(`${PAYMENT_TYPE[p.type]} de ${p.amount} DH enregistré`);
        this.saving.set(false);
        this.paymentForm.reset({ amount: 0, paymentDate: today(), method: 'CASH', type: '', reference: '' });
        this.reload();
      },
      error: err => {
        this.toast.error(errorMessage(err));   // e.g. more than the remaining amount
        this.saving.set(false);
      },
    });
  }

  protected deletePayment(p: Payment) {
    if (!confirm(`Supprimer le règlement de ${p.amount} DH ?`)) return;
    this.api.deletePayment(p.id).subscribe({
      next: () => {
        this.toast.success('Règlement supprimé');
        this.reload();
      },
      error: err => this.toast.error(errorMessage(err)),
    });
  }
}
