import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Invoice, Order, OrderStatus, Payment, PaymentRequest, Quote, QuoteRequest,
} from './models';

/** All calls for quotes, orders, invoices and payments */
@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // ── Quotes ──
  quotes() { return this.http.get<Quote[]>(`${this.api}/quotes`); }
  quote(id: number) { return this.http.get<Quote>(`${this.api}/quotes/${id}`); }
  createQuote(body: QuoteRequest) { return this.http.post<Quote>(`${this.api}/quotes`, body); }
  quoteAction(id: number, action: 'send' | 'accept' | 'reject') {
    return this.http.post<Quote>(`${this.api}/quotes/${id}/${action}`, null);
  }
  convertQuote(id: number) { return this.http.post<Order>(`${this.api}/quotes/${id}/convert`, null); }

  // ── Orders ──
  orders() { return this.http.get<Order[]>(`${this.api}/orders`); }
  order(id: number) { return this.http.get<Order>(`${this.api}/orders/${id}`); }
  changeOrderStatus(id: number, status: OrderStatus) {
    return this.http.patch<Order>(`${this.api}/orders/${id}/status`, { status });
  }
  createInvoice(orderId: number) { return this.http.post<Invoice>(`${this.api}/orders/${orderId}/invoice`, null); }

  // ── Payments ──
  payments(orderId: number) { return this.http.get<Payment[]>(`${this.api}/orders/${orderId}/payments`); }
  addPayment(orderId: number, body: PaymentRequest) {
    return this.http.post<Payment>(`${this.api}/orders/${orderId}/payments`, body);
  }
  deletePayment(paymentId: number) { return this.http.delete<void>(`${this.api}/payments/${paymentId}`); }

  // ── Invoices ──
  invoices() { return this.http.get<Invoice[]>(`${this.api}/invoices`); }

  // ── PDFs (downloaded with the token, then saved by the browser) ──
  downloadPdf(kind: 'quotes' | 'invoices', id: number, fileName: string): Observable<void> {
    return this.http.get(`${this.api}/${kind}/${id}/pdf`, { responseType: 'blob' }).pipe(
      map(blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${fileName}.pdf`;
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      }),
    );
  }
}
