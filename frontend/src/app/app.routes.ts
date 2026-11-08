import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

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
      { path: '', pathMatch: 'full', redirectTo: 'clients' },
      { path: 'clients', loadComponent: () => import('./features/customers/customers').then(m => m.Customers) },
      { path: 'produits', loadComponent: () => import('./features/products/products').then(m => m.Products) },
    ],
  },
  { path: '**', redirectTo: '' },
];
