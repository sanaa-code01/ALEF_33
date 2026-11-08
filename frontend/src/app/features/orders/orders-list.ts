import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DocumentsService } from '../../core/documents.service';
import { errorMessage } from '../../core/api-error';
import { Order } from '../../core/models';
import { FrDatePipe } from '../../shared/date.pipe';
import { ORDER_STATUS } from '../../shared/labels';
import { MoneyPipe } from '../../shared/money.pipe';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-orders-list',
  imports: [RouterLink, MoneyPipe, FrDatePipe],
  template: `
    <div class="page-header"><h1>Commandes</h1></div>
    <p class="muted page-help">Une commande est créée en transformant un devis accepté.</p>

    <div class="card">
      @if (loading()) {
        <p class="muted">Chargement…</p>
      } @else if (orders().length === 0) {
        <p class="muted">Aucune commande pour le moment.</p>
      } @else {
        <table class="table clickable">
          <thead>
            <tr>
              <th>N°</th><th>Client</th><th>Date</th><th>Statut</th>
              <th class="num">Total TTC</th><th class="num">Payé</th><th class="num">Reste</th>
            </tr>
          </thead>
          <tbody>
            @for (o of orders(); track o.id) {
              <tr [routerLink]="['/commandes', o.id]">
                <td class="mono">{{ o.number }}</td>
                <td class="strong">{{ o.customerName }}</td>
                <td>{{ o.orderDate | frDate }}</td>
                <td><span class="badge {{ status[o.status].css }}">{{ status[o.status].label }}</span></td>
                <td class="num strong">{{ o.totalTTC | money }}</td>
                <td class="num">{{ o.paid | money }}</td>
                <td class="num" [class.text-danger]="o.remaining > 0">{{ o.remaining | money }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
})
export class OrdersList {
  private readonly api = inject(DocumentsService);
  private readonly toast = inject(ToastService);

  protected readonly status = ORDER_STATUS;
  protected readonly orders = signal<Order[]>([]);
  protected readonly loading = signal(true);

  constructor() {
    this.api.orders().subscribe({
      next: list => { this.orders.set(list); this.loading.set(false); },
      error: err => { this.toast.error(errorMessage(err)); this.loading.set(false); },
    });
  }
}
