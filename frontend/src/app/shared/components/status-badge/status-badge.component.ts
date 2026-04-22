import { Component, input } from '@angular/core';
import { OrderStatus } from '../../../core/models/order.models';

const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING:   'Pending',
  CONFIRMED: 'Confirmed',
  SHIPPED:   'Shipped',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.scss'],
})
export class StatusBadgeComponent {
  status = input.required<OrderStatus>();

  label(): string {
    return STATUS_LABELS[this.status()];
  }
}
