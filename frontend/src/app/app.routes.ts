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
    path: 'orders/new',
    loadComponent: () =>
      import('./orders/order-create/order-create.component').then(m => m.OrderCreateComponent),
  },
  {
    path: 'orders/:id',
    loadComponent: () =>
      import('./orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent),
    resolve: { order: orderDetailResolver },
  },
  {
    path: 'customers',
    loadComponent: () =>
      import('./customers/customers-list/customers-list.component').then(m => m.CustomersListComponent),
  },
  {
    path: 'customers/new',
    loadComponent: () =>
      import('./customers/customer-create/customer-create.component').then(m => m.CustomerCreateComponent),
  },
  {
    path: 'customers/:id',
    loadComponent: () =>
      import('./customers/customer-detail/customer-detail.component').then(m => m.CustomerDetailComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
