import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OrderDetail, OrderStatus, OrderSummary, PageResponse } from '../../core/models/order.models';

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/orders`;

  getOrders(
    from: string,
    to: string,
    customer: string,
    statuses: OrderStatus[],
    page: number,
    size: number,
    sort = 'orderDate,desc',
    minAmount?: number | null,
    maxAmount?: number | null,
    categories: string[] = [],
    product = '',
  ): Observable<PageResponse<OrderSummary>> {
    let params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('customer', customer)
      .set('page', page)
      .set('size', size)
      .set('sort', sort)
      .set('product', product);

    if (statuses.length > 0)   params = params.set('statuses', statuses.join(','));
    if (categories.length > 0) params = params.set('categories', categories.join(','));
    if (minAmount != null) params = params.set('minAmount', minAmount);
    if (maxAmount != null) params = params.set('maxAmount', maxAmount);

    return this.http.get<PageResponse<OrderSummary>>(this.base, { params });
  }

  getOrder(id: number): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${this.base}/${id}`);
  }

}
