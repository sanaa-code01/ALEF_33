import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DocumentsService } from '../../core/documents.service';
import { errorMessage } from '../../core/api-error';
import { Quote } from '../../core/models';
import { FrDatePipe } from '../../shared/date.pipe';
import { QUOTE_STATUS } from '../../shared/labels';
import { MoneyPipe } from '../../shared/money.pipe';
import { ToastService } from '../../shared/toast.service';
import { LinesTable } from '../../shared/lines-table';

@Component({
  selector: 'app-quote-detail',
  imports: [RouterLink, MoneyPipe, FrDatePipe, LinesTable],
  template: `
    <div class="page-header">
      <a class="btn btn-ghost" routerLink="/devis">← Retour aux devis</a>
    </div>

    @if (quote(); as q) {
      <div class="card section">
        <div class="doc-header">
          <div>
            <h1>Devis {{ q.number }}</h1>
            <p class="muted">
              {{ q.customerName }} · du {{ q.issueDate | frDate }} · valable jusqu'au {{ q.validUntil | frDate }}
            </p>
          </div>
          <span class="badge badge-big {{ status[q.status].css }}">{{ status[q.status].label }}</span>
        </div>

        <div class="actions-bar">
          <button class="btn" (click)="downloadPdf(q)">⬇ Télécharger le PDF</button>
          @if (q.status === 'DRAFT') {
            <button class="btn" (click)="act('send')">Marquer comme envoyé</button>
          }
          @if (q.status === 'DRAFT' || q.status === 'SENT') {
            <button class="btn btn-primary" (click)="act('accept')">Accepté par le client</button>
            <button class="btn btn-danger" (click)="act('reject')">Refusé</button>
          }
          @if (q.status === 'ACCEPTED') {
            <button class="btn btn-primary" (click)="convert()">Transformer en commande →</button>
          }
          @if (q.status === 'CONVERTED') {
            <a class="btn" routerLink="/commandes">Voir les commandes →</a>
          }
        </div>
      </div>

      <div class="card section">
        <app-lines-table [lines]="q.lines" />
        <div class="totals">
          <div><span>Total HT</span><strong>{{ q.totalHT | money }} DH</strong></div>
          <div><span>TVA</span><strong>{{ q.totalVat | money }} DH</strong></div>
          <div class="total-ttc"><span>Total TTC</span><strong>{{ q.totalTTC | money }} DH</strong></div>
        </div>
        @if (q.notes) {
          <p><strong>Remarques :</strong> {{ q.notes }}</p>
        }
      </div>
    } @else {
      <p class="muted">Chargement…</p>
    }
  `,
})
export class QuoteDetail implements OnInit {
  /** The ":id" of the URL /devis/:id */
  readonly id = input.required<string>();

  private readonly api = inject(DocumentsService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly status = QUOTE_STATUS;
  protected readonly quote = signal<Quote | null>(null);

  ngOnInit() {
    this.api.quote(+this.id()).subscribe({
      next: q => this.quote.set(q),
      error: err => this.toast.error(errorMessage(err)),
    });
  }

  protected act(action: 'send' | 'accept' | 'reject') {
    this.api.quoteAction(+this.id(), action).subscribe({
      next: q => {
        this.quote.set(q);
        this.toast.success(`Devis ${QUOTE_STATUS[q.status].label.toLowerCase()}`);
      },
      error: err => this.toast.error(errorMessage(err)),
    });
  }

  protected convert() {
    this.api.convertQuote(+this.id()).subscribe({
      next: order => {
        this.toast.success(`Commande ${order.number} créée`);
        this.router.navigate(['/commandes', order.id]);
      },
      error: err => this.toast.error(errorMessage(err)),
    });
  }

  protected downloadPdf(q: Quote) {
    this.api.downloadPdf('quotes', q.id, q.number).subscribe({
      error: err => this.toast.error(errorMessage(err)),
    });
  }
}
