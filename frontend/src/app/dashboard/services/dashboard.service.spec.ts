import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DashboardService } from './dashboard.service';
import { KpiMetrics, DataPoint, TopProduct, CategoryBreakdown } from '../../core/models/stats.models';
import { environment } from '../../../environments/environment';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/stats`;
  const from = '2026-01-01';
  const to = '2026-01-31';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getKpis() calls GET /stats/kpis with correct params', () => {
    const mockKpis: KpiMetrics = { revenue: 1000, orderCount: 10, avgOrderValue: 100, revenueGrowth: 5, orderGrowth: 3 };

    service.getKpis(from, to).subscribe(data => expect(data).toEqual(mockKpis));

    const req = httpMock.expectOne(r => r.url === `${base}/kpis`);
    expect(req.request.params.get('from')).toBe(from);
    expect(req.request.params.get('to')).toBe(to);
    req.flush(mockKpis);
  });

  it('getRevenueOverTime() calls GET /stats/revenue-over-time', () => {
    const mock: DataPoint[] = [{ label: '2026-01-01', revenue: 500 }];

    service.getRevenueOverTime(from, to).subscribe(data => expect(data).toEqual(mock));

    const req = httpMock.expectOne(r => r.url === `${base}/revenue-over-time`);
    expect(req.request.params.get('from')).toBe(from);
    req.flush(mock);
  });

  it('getOrderCountOverTime() calls GET /stats/orders-over-time', () => {
    const mock: DataPoint[] = [{ label: '2026-01-01', revenue: 5 }];

    service.getOrderCountOverTime(from, to).subscribe(data => expect(data).toEqual(mock));

    const req = httpMock.expectOne(r => r.url === `${base}/orders-over-time`);
    req.flush(mock);
  });

  it('getTopProducts() calls GET /stats/top-products', () => {
    const mock: TopProduct[] = [{ name: 'Laptop', revenue: 1299 }];

    service.getTopProducts(from, to).subscribe(data => expect(data).toEqual(mock));

    const req = httpMock.expectOne(r => r.url === `${base}/top-products`);
    req.flush(mock);
  });

  it('getOrdersByCategory() calls GET /stats/orders-by-category', () => {
    const mock: CategoryBreakdown[] = [{ category: 'Electronics', itemCount: 42 }];

    service.getOrdersByCategory(from, to).subscribe(data => expect(data).toEqual(mock));

    const req = httpMock.expectOne(r => r.url === `${base}/orders-by-category`);
    req.flush(mock);
  });
});
