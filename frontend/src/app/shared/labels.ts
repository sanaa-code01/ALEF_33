import { InvoiceStatus, OrderStatus, PaymentMethod, PaymentType, QuoteStatus } from '../core/models';

// French labels and badge colors for the statuses sent by the API

export const QUOTE_STATUS: Record<QuoteStatus, { label: string; css: string }> = {
  DRAFT: { label: 'Brouillon', css: '' },
  SENT: { label: 'Envoyé', css: 'badge-info' },
  ACCEPTED: { label: 'Accepté', css: 'badge-ok' },
  REJECTED: { label: 'Refusé', css: 'badge-ko' },
  CONVERTED: { label: 'Transformé en commande', css: 'badge-admin' },
};

export const ORDER_STATUS: Record<OrderStatus, { label: string; css: string }> = {
  PENDING: { label: 'En attente', css: '' },
  IN_PRODUCTION: { label: 'En fabrication', css: 'badge-info' },
  DELIVERED: { label: 'Livrée', css: 'badge-ok' },
  CANCELLED: { label: 'Annulée', css: 'badge-ko' },
};

export const INVOICE_STATUS: Record<InvoiceStatus, { label: string; css: string }> = {
  UNPAID: { label: 'Non payée', css: 'badge-ko' },
  PARTIALLY_PAID: { label: 'Partiellement payée', css: 'badge-admin' },
  PAID: { label: 'Payée', css: 'badge-ok' },
  CANCELLED: { label: 'Annulée', css: '' },
};

export const PAYMENT_METHOD: Record<PaymentMethod, string> = {
  CASH: 'Espèces',
  CHEQUE: 'Chèque',
  BANK_TRANSFER: 'Virement',
  CARD: 'Carte bancaire',
  BILL_OF_EXCHANGE: 'Effet de commerce',
};

export const PAYMENT_TYPE: Record<PaymentType, string> = {
  DEPOSIT: 'Acompte',
  INSTALLMENT: 'Mensualité',
  BALANCE: 'Solde',
  FULL: 'Paiement total',
};
