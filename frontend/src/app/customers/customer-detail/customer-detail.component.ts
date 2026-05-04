import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CustomersService } from '../services/customers.service';
import { CustomerSummary } from '../../core/models/customer.models';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './customer-detail.component.html',
  styleUrls: ['./customer-detail.component.scss'],
})
export class CustomerDetailComponent implements OnInit {
  private readonly service = inject(CustomersService);
  private readonly route   = inject(ActivatedRoute);
  private readonly router  = inject(Router);

  customer = signal<CustomerSummary | null>(null);
  loading  = signal(true);
  error    = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.service.getCustomer(id).subscribe({
      next: c => { this.customer.set(c); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }

  back(): void {
    this.router.navigate(['/customers']);
  }

  viewOrders(): void {
    const c = this.customer();
    if (c) this.router.navigate(['/orders'], { queryParams: { customer: c.name } });
  }

  createOrder(): void {
    const c = this.customer();
    if (c) this.router.navigate(['/orders/new'], {
      queryParams: { customerId: c.id, customerName: c.name, customerCity: c.city },
    });
  }
}
