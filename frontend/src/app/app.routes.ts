import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then(m => m.Login),
  },
  {
    // Everything below needs a logged-in user, and is shown inside the shell (sidebar + header)
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then(m => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'devis' },
      { path: 'devis', loadComponent: () => import('./features/quotes/quotes-list').then(m => m.QuotesList) },
      { path: 'devis/nouveau', loadComponent: () => import('./features/quotes/quote-form').then(m => m.QuoteForm) },
      { path: 'devis/:id', loadComponent: () => import('./features/quotes/quote-detail').then(m => m.QuoteDetail) },
      { path: 'commandes', loadComponent: () => import('./features/orders/orders-list').then(m => m.OrdersList) },
      { path: 'commandes/:id', loadComponent: () => import('./features/orders/order-detail').then(m => m.OrderDetail) },
      { path: 'factures', loadComponent: () => import('./features/invoices/invoices-list').then(m => m.InvoicesList) },
      { path: 'clients', loadComponent: () => import('./features/customers/customers').then(m => m.Customers) },
      { path: 'produits', loadComponent: () => import('./features/products/products').then(m => m.Products) },
      {
        path: 'utilisateurs',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/users/users').then(m => m.Users),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
