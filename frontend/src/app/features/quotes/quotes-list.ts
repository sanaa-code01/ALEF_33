import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DocumentsService } from '../../core/documents.service';
import { errorMessage } from '../../core/api-error';
import { Quote } from '../../core/models';
import { FrDatePipe } from '../../shared/date.pipe';
import { QUOTE_STATUS } from '../../shared/labels';
import { MoneyPipe } from '../../shared/money.pipe';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-quotes-list',
  imports: [RouterLink, MoneyPipe, FrDatePipe],
  template: `
    <div class="page-header">
      <h1>Devis</h1>
      <a class="btn btn-primary" routerLink="/devis/nouveau">+ Nouveau devis</a>
    </div>

    <div class="card">
      @if (loading()) {
        <p class="muted">Chargement…</p>
      } @else if (quotes().length === 0) {
        <p class="muted">Aucun devis pour le moment.</p>
      } @else {
        <table class="table clickable">
          <thead>
            <tr><th>N°</th><th>Client</th><th>Date</th><th>Valable jusqu'au</th><th class="num">Total TTC</th><th>Statut</th></tr>
          </thead>
          <tbody>
            @for (q of quotes(); track q.id) {
              <tr [routerLink]="['/devis', q.id]">
                <td class="mono">{{ q.number }}</td>
                <td class="strong">{{ q.customerName }}</td>
                <td>{{ q.issueDate | frDate }}</td>
                <td>{{ q.validUntil | frDate }}</td>
                <td class="num strong">{{ q.totalTTC | money }} DH</td>
                <td><span class="badge {{ status[q.status].css }}">{{ status[q.status].label }}</span></td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
})
export class QuotesList {
  private readonly api = inject(DocumentsService);
  private readonly toast = inject(ToastService);

  protected readonly status = QUOTE_STATUS;
  protected readonly quotes = signal<Quote[]>([]);
  protected readonly loading = signal(true);

  constructor() {
    this.api.quotes().subscribe({
      next: list => { this.quotes.set(list); this.loading.set(false); },
      error: err => { this.toast.error(errorMessage(err)); this.loading.set(false); },
    });
  }
}
