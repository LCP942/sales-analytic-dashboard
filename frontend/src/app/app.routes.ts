import { Routes } from '@angular/router';
import { orderDetailResolver } from './orders/resolvers/order-detail.resolver';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./orders/orders.component').then(m => m.OrdersComponent),
  },
  {
    path: 'orders/:id',
    loadComponent: () =>
      import('./orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent),
    resolve: { order: orderDetailResolver },
  },
  { path: '**', redirectTo: 'dashboard' },
];
