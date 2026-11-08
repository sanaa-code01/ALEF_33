// TypeScript versions of the backend DTOs (same field names as the JSON)

export type Role = 'ADMIN' | 'EMPLOYEE';

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  email: string;
  fullName: string;
  role: Role;
}

export interface Customer {
  id: number;
  name: string;
  ice: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
}
export type CustomerRequest = Omit<Customer, 'id'>;

export interface Product {
  id: number;
  reference: string;
  designation: string;
  description: string | null;
  unitPriceTTC: number;
  unitPriceHT: number;
  vatRate: number;
  active: boolean;
}
export interface ProductRequest {
  reference: string;
  designation: string;
  description: string | null;
  unitPriceTTC: number;
  vatRate: number | null;
  active: boolean;
}

/** Error body returned by the backend (Spring ProblemDetail) */
export interface ApiProblem {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}

// ── Quotes, orders, invoices, payments ─────────────────────
export type QuoteStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED' | 'CONVERTED';
export type OrderStatus = 'PENDING' | 'IN_PRODUCTION' | 'DELIVERED' | 'CANCELLED';
export type InvoiceStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED';
export type PaymentMethod = 'CASH' | 'CHEQUE' | 'BANK_TRANSFER' | 'CARD' | 'BILL_OF_EXCHANGE';
export type PaymentType = 'DEPOSIT' | 'INSTALLMENT' | 'BALANCE' | 'FULL';

export interface DocumentLine {
  id: number;
  productId: number | null;
  designation: string;
  quantity: number;
  unitPriceTTC: number;
  unitPriceHT: number;
  vatRate: number;
  lineTotalTTC: number;
  lineTotalHT: number;
}

interface DocumentTotals {
  totalHT: number;
  totalVat: number;
  totalTTC: number;
}

export interface Quote extends DocumentTotals {
  id: number;
  number: string;
  customerId: number;
  customerName: string;
  issueDate: string;
  validUntil: string | null;
  status: QuoteStatus;
  notes: string | null;
  lines: DocumentLine[];
}

export interface LineRequest {
  productId: number | null;
  designation: string | null;
  quantity: number;
  unitPriceTTC: number | null;
  vatRate: number | null;
}

export interface QuoteRequest {
  customerId: number;
  validUntil: string | null;
  notes: string | null;
  lines: LineRequest[];
}

export interface Order extends DocumentTotals {
  id: number;
  number: string;
  quoteId: number | null;
  quoteNumber: string | null;
  customerId: number;
  customerName: string;
  orderDate: string;
  deliveryDate: string | null;
  status: OrderStatus;
  paid: number;
  remaining: number;
  notes: string | null;
  lines: DocumentLine[];
}

export interface Invoice extends DocumentTotals {
  id: number;
  number: string;
  orderId: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  issueDate: string;
  dueDate: string | null;
  status: InvoiceStatus;
  paid: number;
  remaining: number;
  notes: string | null;
  lines: DocumentLine[];
}

export interface Payment {
  id: number;
  amount: number;
  paymentDate: string;
  method: PaymentMethod;
  type: PaymentType;
  reference: string | null;
  notes: string | null;
}

export interface PaymentRequest {
  amount: number;
  paymentDate: string | null;
  method: PaymentMethod;
  type: PaymentType | null;
  reference: string | null;
  notes: string | null;
}
