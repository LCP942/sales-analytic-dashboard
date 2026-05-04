import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CustomerCreateRequest, CustomerSummary } from '../../core/models/customer.models';
import { PageResponse } from '../../core/models/order.models';

@Injectable({ providedIn: 'root' })
export class CustomersService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/customers`;

  getCustomers(name = '', page = 0, size = 20): Observable<PageResponse<CustomerSummary>> {
    const params = new HttpParams()
      .set('name', name)
      .set('page', page)
      .set('size', size);
    return this.http.get<PageResponse<CustomerSummary>>(this.base, { params });
  }

  getCustomer(id: number): Observable<CustomerSummary> {
    return this.http.get<CustomerSummary>(`${this.base}/${id}`);
  }

  createCustomer(req: CustomerCreateRequest): Observable<CustomerSummary> {
    return this.http.post<CustomerSummary>(this.base, req);
  }
}
