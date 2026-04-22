import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategoryBreakdown, DataPoint, KpiMetrics, TopProduct } from '../../core/models/stats.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/stats`;

  getKpis(from: string, to: string): Observable<KpiMetrics> {
    return this.http.get<KpiMetrics>(`${this.base}/kpis`, { params: this.params(from, to) });
  }

  getRevenueOverTime(from: string, to: string): Observable<DataPoint[]> {
    return this.http.get<DataPoint[]>(`${this.base}/revenue-over-time`, { params: this.params(from, to) });
  }

  getOrderCountOverTime(from: string, to: string): Observable<DataPoint[]> {
    return this.http.get<DataPoint[]>(`${this.base}/orders-over-time`, { params: this.params(from, to) });
  }

  getTopProducts(from: string, to: string): Observable<TopProduct[]> {
    return this.http.get<TopProduct[]>(`${this.base}/top-products`, { params: this.params(from, to) });
  }

  getOrdersByCategory(from: string, to: string): Observable<CategoryBreakdown[]> {
    return this.http.get<CategoryBreakdown[]>(`${this.base}/orders-by-category`, { params: this.params(from, to) });
  }

  private params(from: string, to: string): HttpParams {
    return new HttpParams().set('from', from).set('to', to);
  }
}
