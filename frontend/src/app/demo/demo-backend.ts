import {
  Customer, DocumentLine, Invoice, InvoiceStatus, LoginResponse, Order, OrderStatus,
  Payment, PaymentType, Product, Quote, QuoteStatus, Role, User,
} from '../core/models';

/**
 * A copy of the backend's business rules, running in the browser for the online demo.
 * Same rules as the Spring Boot services: TTC pricing, statuses, payments, French messages.
 * The data lives in memory and goes back to the start on every page refresh.
 */

export class DemoError extends Error {
  constructor(readonly status: number, message: string, readonly errors?: Record<string, string>) {
    super(message);
  }
}

interface DemoUser extends User { password: string; }
interface LineInput { productId: number | null; designation: string | null; quantity: number; unitPriceTTC: number | null; vatRate: number | null; }

const QUOTE_LABEL: Record<QuoteStatus, string> = {
  DRAFT: 'Brouillon', SENT: 'Envoyé', ACCEPTED: 'Accepté', REJECTED: 'Refusé', CONVERTED: 'Transformé en commande',
};
const ORDER_LABEL: Record<OrderStatus, string> = {
  PENDING: 'En attente', IN_PRODUCTION: 'En fabrication', DELIVERED: 'Livrée', CANCELLED: 'Annulée',
};
const TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['IN_PRODUCTION', 'CANCELLED'],
  IN_PRODUCTION: ['DELIVERED', 'CANCELLED'],
  DELIVERED: [],
  CANCELLED: [],
};

// ── Small helpers ───────────────────────────────────────────
const round2 = (v: number) => Math.round((v + Number.EPSILON) * 100) / 100;
const htFromTtc = (ttc: number, rate: number) => round2(ttc / (1 + rate / 100));
function isoDate(offsetDays = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
const YEAR = new Date().getFullYear();

export class DemoBackend {
  private ids = 100;
  private customers: Customer[] = [];
  private products: Product[] = [];
  private quotes: Quote[] = [];
  private orders: Order[] = [];
  private invoices: Invoice[] = [];
  private payments = new Map<number, Payment[]>();   // orderId → payments
  private users: DemoUser[] = [];
  private currentRole: Role = 'ADMIN';

  constructor() {
    this.seed();
  }

  // ════════════════════════════════════════════════════════
  //  Router: (method, path, body) → answer, like the REST API
  // ════════════════════════════════════════════════════════
  handle(method: string, path: string, params: URLSearchParams, body: any): unknown {
    const p = path.replace(/^.*\/api/, '');
    const seg = p.split('/').filter(Boolean);           // e.g. ["quotes", "2", "accept"]
    const id = seg[1] !== undefined ? Number(seg[1]) : NaN;

    if (p === '/auth/login' && method === 'POST') return this.login(body.email, body.password);
    if (p === '/ping') return { status: 'ok', app: 'ALEF 33 (demo)' };

    switch (seg[0]) {
      case 'customers': return this.customersApi(method, id, params, body);
      case 'products': return this.productsApi(method, id, params, body);
      case 'quotes': return this.quotesApi(method, id, seg[2], body);
      case 'orders': return this.ordersApi(method, id, seg[2], body);
      case 'invoices':
        if (method === 'GET' && seg.length === 1) return this.list(this.invoices).map(i => this.withPaid(i));
        if (method === 'GET') return this.withPaid(this.find(this.invoices, id, 'Facture'));
        break;
      case 'payments':
        if (method === 'DELETE') return this.deletePayment(id);
        break;
      case 'users': return this.usersApi(method, id, params, body);
    }
    throw new DemoError(404, 'Introuvable');
  }

  // ── Auth ──
  private login(email: string, password: string): LoginResponse {
    const user = this.users.find(u => u.email.toLowerCase() === (email ?? '').trim().toLowerCase());
    if (!user || user.password !== password) throw new DemoError(401, 'Email ou mot de passe incorrect');
    if (!user.enabled) throw new DemoError(401, 'Ce compte est désactivé');
    this.currentRole = user.role;
    const expiresAt = new Date(Date.now() + 8 * 3600 * 1000).toISOString();
    return { token: 'demo-token', tokenType: 'Bearer', expiresAt, email: user.email, fullName: user.fullName, role: user.role };
  }

  /** Called on each request with the role of the logged-in user (the "token") */
  setRole(role: Role | null) {
    if (role) this.currentRole = role;
  }

  private requireAdmin() {
    if (this.currentRole !== 'ADMIN') throw new DemoError(403, "Vous n'avez pas les droits pour cette action.");
  }

  // ── Customers ──
  private customersApi(method: string, id: number, params: URLSearchParams, body: any) {
    if (method === 'GET' && isNaN(id)) {
      const search = (params.get('search') ?? '').trim().toLowerCase();
      return this.customers
        .filter(c => !search || c.name.toLowerCase().includes(search))
        .sort((a, b) => a.name.localeCompare(b.name));
    }
    if (method === 'GET') return this.find(this.customers, id, 'Client');
    if (method === 'POST' || method === 'PUT') {
      if (!body.name?.trim()) throw new DemoError(400, 'Certaines informations sont invalides', { name: 'Le nom du client est obligatoire' });
      const data = { name: body.name.trim(), ice: body.ice, email: body.email, phone: body.phone, address: body.address, city: body.city };
      if (method === 'POST') {
        const c = { id: this.nextId(), ...data } as Customer;
        this.customers.push(c);
        return c;
      }
      return Object.assign(this.find(this.customers, id, 'Client'), data);
    }
    if (method === 'DELETE') {
      this.requireAdmin();
      const c = this.find(this.customers, id, 'Client');
      if (this.quotes.some(q => q.customerId === id)) {
        throw new DemoError(409, `Le client ${c.name} a des devis et ne peut pas être supprimé`);
      }
      this.customers = this.customers.filter(x => x.id !== id);
      return null;
    }
    throw new DemoError(405, 'Action non disponible');
  }

  // ── Products ──
  private productsApi(method: string, id: number, params: URLSearchParams, body: any) {
    if (method === 'GET' && isNaN(id)) {
      const activeOnly = params.get('activeOnly') === 'true';
      return this.products.filter(p => !activeOnly || p.active).sort((a, b) => a.designation.localeCompare(b.designation));
    }
    if (method === 'GET') return this.find(this.products, id, 'Produit');
    if (method === 'POST' || method === 'PUT') {
      this.requireAdmin();
      const ref = (body.reference ?? '').trim();
      if (this.products.some(p => p.reference === ref && p.id !== id)) {
        throw new DemoError(409, `La référence ${ref} est déjà utilisée`);
      }
      const rate = body.vatRate ?? 20;
      const data = {
        reference: ref, designation: body.designation.trim(), description: body.description,
        unitPriceTTC: Number(body.unitPriceTTC), unitPriceHT: htFromTtc(Number(body.unitPriceTTC), rate),
        vatRate: rate, active: body.active ?? true,
      };
      if (method === 'POST') {
        const p = { id: this.nextId(), ...data } as Product;
        this.products.push(p);
        return p;
      }
      return Object.assign(this.find(this.products, id, 'Produit'), data);
    }
    if (method === 'DELETE') {
      this.requireAdmin();
      this.find(this.products, id, 'Produit').active = false;
      return null;
    }
    throw new DemoError(405, 'Action non disponible');
  }

  // ── Quotes ──
  private quotesApi(method: string, id: number, action: string | undefined, body: any) {
    if (method === 'GET' && isNaN(id)) return this.list(this.quotes);
    if (method === 'GET') return this.find(this.quotes, id, 'Devis');
    if (method === 'POST' && isNaN(id)) return this.createQuote(body);

    const q = this.find(this.quotes, id, 'Devis');
    const require = (...allowed: QuoteStatus[]) => {
      if (!allowed.includes(q.status)) {
        throw new DemoError(409, `Le devis ${q.number} est « ${QUOTE_LABEL[q.status]} » : action impossible`);
      }
    };
    switch (action) {
      case 'send': require('DRAFT'); q.status = 'SENT'; return q;
      case 'accept':
        require('DRAFT', 'SENT');
        if (q.validUntil && q.validUntil < isoDate()) throw new DemoError(409, `Le devis ${q.number} a expiré`);
        q.status = 'ACCEPTED';
        return q;
      case 'reject': require('DRAFT', 'SENT'); q.status = 'REJECTED'; return q;
      case 'convert': {
        require('ACCEPTED');
        if (this.orders.some(o => o.quoteId === q.id)) {
          throw new DemoError(409, `Le devis ${q.number} a déjà été transformé en commande`);
        }
        const order: Order = {
          id: this.nextId(), number: this.nextNumber('CMD', this.orders), quoteId: q.id, quoteNumber: q.number,
          customerId: q.customerId, customerName: q.customerName, orderDate: isoDate(), deliveryDate: null,
          status: 'PENDING', paid: 0, remaining: q.totalTTC, notes: q.notes,
          lines: q.lines.map(l => ({ ...l, id: this.nextId() })),
          totalHT: q.totalHT, totalVat: q.totalVat, totalTTC: q.totalTTC,
        };
        this.orders.push(order);
        q.status = 'CONVERTED';
        return this.withPaid(order);
      }
    }
    throw new DemoError(405, 'Action non disponible');
  }

  private createQuote(body: { customerId: number; validUntil: string | null; notes: string | null; lines: LineInput[] }) {
    const customer = this.find(this.customers, body.customerId, 'Client');
    if (!body.lines?.length) throw new DemoError(409, 'Le document doit contenir au moins une ligne');
    const lines = body.lines.map(input => this.buildLine(input));
    const quote: Quote = {
      id: this.nextId(), number: this.nextNumber('DEV', this.quotes), customerId: customer.id, customerName: customer.name,
      issueDate: isoDate(), validUntil: body.validUntil || isoDate(30), status: 'DRAFT', notes: body.notes, lines,
      ...this.totals(lines),
    };
    this.quotes.push(quote);
    return quote;
  }

  /** Same as LineMapper + PricingService on the backend */
  private buildLine(input: LineInput): DocumentLine {
    const product = input.productId != null ? this.find(this.products, input.productId, 'Produit') : null;
    if (product && !product.active) throw new DemoError(409, `Le produit ${product.reference} n'est plus actif`);
    const designation = input.designation?.trim() || product?.designation;
    const unitPriceTTC = input.unitPriceTTC ?? product?.unitPriceTTC;
    const vatRate = input.vatRate ?? product?.vatRate ?? 20;
    if (!designation) throw new DemoError(409, 'Chaque ligne doit avoir une désignation ou un produit');
    if (unitPriceTTC == null) throw new DemoError(409, 'Chaque ligne doit avoir un prix TTC ou un produit');
    if (!(input.quantity > 0)) throw new DemoError(409, 'La quantité doit être supérieure à 0');
    if (unitPriceTTC < 0) throw new DemoError(409, 'Le prix unitaire doit être positif ou nul');
    const lineTotalTTC = round2(unitPriceTTC * input.quantity);
    return {
      id: this.nextId(), productId: product?.id ?? null, designation, quantity: input.quantity,
      unitPriceTTC, unitPriceHT: htFromTtc(unitPriceTTC, vatRate), vatRate,
      lineTotalTTC, lineTotalHT: htFromTtc(lineTotalTTC, vatRate),
    };
  }

  private totals(lines: DocumentLine[]) {
    const totalTTC = round2(lines.reduce((s, l) => s + l.lineTotalTTC, 0));
    const totalHT = round2(lines.reduce((s, l) => s + l.lineTotalHT, 0));
    return { totalTTC, totalHT, totalVat: round2(totalTTC - totalHT) };
  }

  // ── Orders, invoices, payments ──
  private ordersApi(method: string, id: number, action: string | undefined, body: any) {
    if (method === 'GET' && isNaN(id)) return this.list(this.orders).map(o => this.withPaid(o));
    const order = this.find(this.orders, id, 'Commande');
    if (method === 'GET' && !action) return this.withPaid(order);
    if (method === 'GET' && action === 'payments') return [...(this.payments.get(id) ?? [])];
    if (method === 'GET' && action === 'payment-summary') {
      const o = this.withPaid(order);
      return { orderNumber: o.number, totalTTC: o.totalTTC, paid: o.paid, remaining: o.remaining };
    }
    if (method === 'PATCH' && action === 'status') {
      const next = body.status as OrderStatus;
      if (!TRANSITIONS[order.status].includes(next)) {
        throw new DemoError(409, `Commande ${order.number} : impossible de passer de « ${ORDER_LABEL[order.status]} » à « ${ORDER_LABEL[next]} »`);
      }
      order.status = next;
      if (next === 'DELIVERED') order.deliveryDate = isoDate();
      return this.withPaid(order);
    }
    if (method === 'POST' && action === 'invoice') {
      if (order.status === 'CANCELLED') throw new DemoError(409, `La commande ${order.number} est annulée`);
      if (this.invoices.some(i => i.orderId === id)) throw new DemoError(409, `La commande ${order.number} est déjà facturée`);
      const invoice: Invoice = {
        id: this.nextId(), number: this.nextNumber('FAC', this.invoices), orderId: order.id, orderNumber: order.number,
        customerId: order.customerId, customerName: order.customerName, issueDate: isoDate(), dueDate: isoDate(30),
        status: 'UNPAID', paid: 0, remaining: order.totalTTC, notes: order.notes,
        lines: order.lines.map(l => ({ ...l, id: this.nextId() })),
        totalHT: order.totalHT, totalVat: order.totalVat, totalTTC: order.totalTTC,
      };
      this.invoices.push(invoice);
      return this.withPaid(invoice);
    }
    if (method === 'POST' && action === 'payments') return this.addPayment(order, body);
    throw new DemoError(405, 'Action non disponible');
  }

  private addPayment(order: Order, body: any): Payment {
    if (order.status === 'CANCELLED') throw new DemoError(409, `La commande ${order.number} est annulée`);
    const amount = Number(body.amount);
    if (!(amount > 0)) throw new DemoError(409, 'Le montant du règlement doit être supérieur à 0');
    if (!body.method) throw new DemoError(409, 'Le mode de règlement est obligatoire');
    const paid = this.paidFor(order.id);
    const remaining = round2(order.totalTTC - paid);
    if (amount > remaining + 0.001) {
      throw new DemoError(409, `Le règlement de ${amount.toFixed(2)} DH dépasse le reste à payer (${remaining.toFixed(2)} DH)`);
    }
    const everything = Math.abs(amount - remaining) < 0.001;
    const type: PaymentType = body.type ?? (everything && paid === 0 ? 'FULL' : everything ? 'BALANCE' : paid === 0 ? 'DEPOSIT' : 'INSTALLMENT');
    const payment: Payment = {
      id: this.nextId(), amount, paymentDate: body.paymentDate || isoDate(), method: body.method, type,
      reference: body.reference ?? null, notes: body.notes ?? null,
    };
    this.payments.set(order.id, [...(this.payments.get(order.id) ?? []), payment]);
    return payment;
  }

  private deletePayment(paymentId: number) {
    this.requireAdmin();
    for (const [orderId, list] of this.payments) {
      if (list.some(p => p.id === paymentId)) {
        this.payments.set(orderId, list.filter(p => p.id !== paymentId));
        return null;
      }
    }
    throw new DemoError(404, `Règlement introuvable (n° ${paymentId})`);
  }

  private paidFor(orderId: number) {
    return round2((this.payments.get(orderId) ?? []).reduce((s, p) => s + p.amount, 0));
  }

  /** Adds paid / remaining, and the invoice status that follows from them */
  private withPaid<T extends Order | Invoice>(doc: T): T {
    const orderId = 'orderId' in doc ? doc.orderId : doc.id;
    const order = this.orders.find(o => o.id === orderId)!;
    const paid = this.paidFor(orderId);
    const result = { ...doc, paid, remaining: round2(order.totalTTC - paid) };
    if ('orderId' in result && result.status !== 'CANCELLED') {
      (result as Invoice).status = (paid <= 0 ? 'UNPAID' : paid >= order.totalTTC ? 'PAID' : 'PARTIALLY_PAID') as InvoiceStatus;
      Object.assign(doc, { status: (result as Invoice).status });
    }
    return result;
  }

  // ── Users ──
  private usersApi(method: string, id: number, params: URLSearchParams, body: any) {
    this.requireAdmin();
    if (method === 'GET') return this.users.map(({ password, ...u }) => u).sort((a, b) => a.fullName.localeCompare(b.fullName));
    if (method === 'POST') {
      if (this.users.some(u => u.email.toLowerCase() === body.email.toLowerCase())) {
        throw new DemoError(409, `L'email ${body.email} est déjà utilisé`);
      }
      const user: DemoUser = { id: this.nextId(), email: body.email.toLowerCase(), fullName: body.fullName, role: body.role, enabled: true, password: body.password };
      this.users.push(user);
      const { password, ...publicUser } = user;
      return publicUser;
    }
    if (method === 'PATCH') {
      const user = this.find(this.users, id, 'Utilisateur');
      const enabled = params.get('enabled') === 'true';
      if (!enabled && user.role === 'ADMIN' && user.email === 'admin@alef33.ma') {
        throw new DemoError(409, 'Vous ne pouvez pas désactiver votre propre compte');
      }
      user.enabled = enabled;
      const { password, ...publicUser } = user;
      return publicUser;
    }
    throw new DemoError(405, 'Action non disponible');
  }

  // ── Generic helpers ──
  private nextId() {
    return ++this.ids;
  }

  private nextNumber(prefix: string, docs: { number: string }[]) {
    const last = docs.map(d => Number(d.number.split('-')[2])).reduce((a, b) => Math.max(a, b), 0);
    return `${prefix}-${YEAR}-${String(last + 1).padStart(4, '0')}`;
  }

  private find<T extends { id: number }>(list: T[], id: number, label: string): T {
    const item = list.find(x => x.id === id);
    if (!item) throw new DemoError(404, `${label} introuvable (n° ${id})`);
    return item;
  }

  /** Newest first, like the API */
  private list<T extends { id: number }>(items: T[]): T[] {
    return [...items].sort((a, b) => b.id - a.id);
  }

  // ════════════════════════════════════════════════════════
  //  Demo data (same spirit as the backend's DemoDataRunner)
  // ════════════════════════════════════════════════════════
  private seed() {
    this.users = [
      { id: 1, email: 'admin@alef33.ma', fullName: 'Administrateur', role: 'ADMIN', enabled: true, password: 'demo' },
      { id: 2, email: 'employe@alef33.ma', fullName: 'Employé Démo', role: 'EMPLOYEE', enabled: true, password: 'demo' },
    ];
    this.customers = [
      { id: 1, name: 'Hôtel Atlas Agadir', ice: '001234567000089', email: 'achats@hotel-atlas.example', phone: null, address: 'Boulevard du 20 Août', city: 'Agadir' },
      { id: 2, name: 'Mr. Ali', ice: null, email: null, phone: null, address: 'City Salam', city: 'Agadir' },
      { id: 3, name: 'Café Les Dunes', ice: null, email: 'contact@cafe-dunes.example', phone: null, address: 'Avenue Hassan II', city: 'Inezgane' },
    ];
    const product = (id: number, reference: string, designation: string, ttc: number): Product =>
      ({ id, reference, designation, description: null, unitPriceTTC: ttc, unitPriceHT: htFromTtc(ttc, 20), vatRate: 20, active: true });
    this.products = [
      product(1, 'FOUR-6N', 'Four mixte 6 niveaux', 54000),
      product(2, 'TABLE-INOX-180', 'Table inox 180 cm', 3840),
      product(3, 'ETAGERE-80', 'Étagère Inox 80 / 33cm', 800),
      product(4, 'FRITEUSE-2', 'Friteuse double à gaz', 6000),
      product(5, 'COMPTOIR-REF-3P', 'Comptoir réfrigéré Inox 3 portes', 10000),
    ];

    const line = (productId: number | null, designation: string, quantity: number, ttc: number) =>
      this.buildLine({ productId, designation, quantity, unitPriceTTC: ttc, vatRate: 20 });
    const quote = (id: number, n: number, customer: Customer, status: QuoteStatus, days: number, lines: DocumentLine[], notes: string | null): Quote =>
      ({ id, number: `DEV-${YEAR}-000${n}`, customerId: customer.id, customerName: customer.name, issueDate: isoDate(days),
         validUntil: isoDate(days + 30), status, notes, lines, ...this.totals(lines) });

    const [atlas, ali, dunes] = this.customers;
    const q1 = quote(1, 1, atlas, 'CONVERTED', -20, [
      line(1, 'Four mixte 6 niveaux', 1, 54000),
      line(2, 'Table inox 180 cm', 3, 3840),
      line(null, 'Installation et mise en service', 1, 3000),
    ], 'Livraison sous 3 semaines');
    const q2 = quote(2, 2, ali, 'CONVERTED', -8, [
      line(1, 'Four mixte 6 niveaux', 1, 54000),
      line(3, 'Étagère Inox 80 / 33cm', 4, 800),
    ], null);
    const q3 = quote(3, 3, dunes, 'SENT', -2, [
      line(5, 'Comptoir réfrigéré Inox 3 portes', 1, 10000),
      line(4, 'Friteuse double à gaz', 1, 6000),
      line(null, 'Habillage inox du bar sur mesure', 1, 8500),
    ], 'Prix valables 30 jours');
    this.quotes = [q1, q2, q3];

    const order = (id: number, n: number, q: Quote, status: OrderStatus, days: number, delivered: boolean): Order =>
      ({ id, number: `CMD-${YEAR}-000${n}`, quoteId: q.id, quoteNumber: q.number, customerId: q.customerId, customerName: q.customerName,
         orderDate: isoDate(days), deliveryDate: delivered ? isoDate(days + 12) : null, status, paid: 0, remaining: q.totalTTC,
         notes: q.notes, lines: q.lines.map(l => ({ ...l })), totalHT: q.totalHT, totalVat: q.totalVat, totalTTC: q.totalTTC });
    const o1 = order(1, 1, q1, 'DELIVERED', -18, true);
    const o2 = order(2, 2, q2, 'IN_PRODUCTION', -6, false);
    this.orders = [o1, o2];

    const pay = (id: number, amount: number, days: number, method: Payment['method'], type: PaymentType, reference: string | null): Payment =>
      ({ id, amount, paymentDate: isoDate(days), method, type, reference, notes: null });
    this.payments.set(1, [
      pay(1, 20556, -18, 'CHEQUE', 'DEPOSIT', 'CHQ-458921'),
      pay(2, 20000, -6, 'BANK_TRANSFER', 'INSTALLMENT', null),
      pay(3, 27964, -1, 'BANK_TRANSFER', 'BALANCE', null),
    ]);
    this.payments.set(2, [pay(4, 17160, -6, 'CHEQUE', 'DEPOSIT', null)]);

    const invoice = (id: number, n: number, o: Order, days: number): Invoice =>
      ({ id, number: `FAC-${YEAR}-000${n}`, orderId: o.id, orderNumber: o.number, customerId: o.customerId, customerName: o.customerName,
         issueDate: isoDate(days), dueDate: isoDate(days + 30), status: 'UNPAID', paid: 0, remaining: o.totalTTC, notes: o.notes,
         lines: o.lines.map(l => ({ ...l })), totalHT: o.totalHT, totalVat: o.totalVat, totalTTC: o.totalTTC });
    this.invoices = [invoice(1, 1, o1, -10), invoice(2, 2, o2, -5)];
  }
}
