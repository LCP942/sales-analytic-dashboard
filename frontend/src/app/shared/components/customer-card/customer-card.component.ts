import { Component, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Customer } from '../../../core/models/order.models';

@Component({
  selector: 'app-customer-card',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './customer-card.component.html',
  styleUrls: ['./customer-card.component.scss'],
})
export class CustomerCardComponent {
  customer = input.required<Customer>();

  initial(): string {
    return this.customer().name.charAt(0).toUpperCase();
  }
}
