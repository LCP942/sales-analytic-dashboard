import { Component, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { OrderDetail } from '../../core/models/order.models';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { OrderTimelineComponent } from '../../shared/components/order-timeline/order-timeline.component';
import { CustomerCardComponent } from '../../shared/components/customer-card/customer-card.component';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    StatusBadgeComponent,
    OrderTimelineComponent,
    CustomerCardComponent,
  ],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent {
  order = input.required<OrderDetail>();
  columns = ['product', 'category', 'qty', 'unitPrice', 'lineTotal'];

  private readonly router = inject(Router);

  back(): void {
    this.router.navigate(['/orders']);
  }
}
