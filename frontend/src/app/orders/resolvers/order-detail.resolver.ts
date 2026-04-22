import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { EMPTY } from 'rxjs';
import { OrderDetail } from '../../core/models/order.models';
import { OrdersService } from '../services/orders.service';

export const orderDetailResolver: ResolveFn<OrderDetail> = route => {
  const id = Number(route.paramMap.get('id'));
  if (!Number.isInteger(id) || id <= 0) {
    inject(Router).navigate(['/orders']);
    return EMPTY;
  }
  return inject(OrdersService).getOrder(id);
};
