import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DocumentsService } from '../../core/documents.service';
import { errorMessage } from '../../core/api-error';
import { Invoice } from '../../core/models';
import { FrDatePipe } from '../../shared/date.pipe';
import { INVOICE_STATUS } from '../../shared/labels';
import { MoneyPipe } from '../../shared/money.pipe';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-invoices-list',
  imports: [RouterLink, MoneyPipe, FrDatePipe],
  template: `
    <div class="page-header"><h1>Factures</h1></div>

    <div class="stats">
      <div class="card stat"><span>Total facturé</span><strong>{{ stats().billed | money }} DH</strong></div>
      <div class="card stat"><span>Encaissé</span><strong class="text-ok">{{ stats().paid | money }} DH</strong></div>
      <div class="card stat"><span>Reste à encaisser</span><strong class="text-danger">{{ stats().remaining | money }} DH</strong></div>
    </div>

    <div class="card">
      @if (loading()) {
        <p class="muted">Chargement…</p>
      } @else if (invoices().length === 0) {
        <p class="muted">Aucune facture. Une facture se crée depuis la page d'une commande.</p>
      } @else {
        <table class="table">
          <thead>
            <tr>
              <th>N°</th><th>Client</th><th>Date</th><th>Échéance</th>
              <th class="num">Total TTC</th><th class="num">Reste</th><th>Statut</th><th></th>
            </tr>
          </thead>
          <tbody>
            @for (inv of invoices(); track inv.id) {
              <tr>
                <td class="mono">{{ inv.number }}</td>
                <td class="strong">{{ inv.customerName }}</td>
                <td>{{ inv.issueDate | frDate }}</td>
                <td>{{ inv.dueDate | frDate }}</td>
                <td class="num strong">{{ inv.totalTTC | money }}</td>
                <td class="num" [class.text-danger]="inv.remaining > 0">{{ inv.remaining | money }}</td>
                <td><span class="badge {{ status[inv.status].css }}">{{ status[inv.status].label }}</span></td>
                <td class="actions">
                  <a class="btn btn-small" [routerLink]="['/commandes', inv.orderId]">Commande</a>
                  <button class="btn btn-small" (click)="download(inv)">⬇ PDF</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
})
export class InvoicesList {
  private readonly api = inject(DocumentsService);
  private readonly toast = inject(ToastService);

  protected readonly status = INVOICE_STATUS;
  protected readonly invoices = signal<Invoice[]>([]);
  protected readonly loading = signal(true);

  protected readonly stats = computed(() => {
    const active = this.invoices().filter(i => i.status !== 'CANCELLED');
    const sum = (f: (i: Invoice) => number) => active.reduce((s, i) => s + f(i), 0);
    return { billed: sum(i => i.totalTTC), paid: sum(i => i.paid), remaining: sum(i => i.remaining) };
  });

  constructor() {
    this.api.invoices().subscribe({
      next: list => { this.invoices.set(list); this.loading.set(false); },
      error: err => { this.toast.error(errorMessage(err)); this.loading.set(false); },
    });
  }

  protected download(inv: Invoice) {
    this.api.downloadPdf('invoices', inv.id, inv.number).subscribe({
      error: err => this.toast.error(errorMessage(err)),
    });
  }
}
