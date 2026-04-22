import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { OrdersService } from './orders.service';
import { OrderDetail, OrderSummary, PageResponse } from '../../core/models/order.models';
import { environment } from '../../../environments/environment';

const base = `${environment.apiBaseUrl}/orders`;

const emptyPage: PageResponse<OrderSummary> = {
  content: [], totalElements: 0, totalPages: 0, size: 10, number: 0,
};

describe('OrdersService', () => {
  let service: OrdersService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrdersService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getOrders()', () => {
    it('sends correct date range and pagination params', () => {
      service.getOrders('2026-01-01', '2026-01-31', '', [], 0, 10).subscribe();

      const req = httpMock.expectOne(r => r.url === base);
      expect(req.request.params.get('from')).toBe('2026-01-01');
      expect(req.request.params.get('to')).toBe('2026-01-31');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(emptyPage);
    });

    it('defaults sort to orderDate,desc', () => {
      service.getOrders('2026-01-01', '2026-01-31', '', [], 0, 10).subscribe();

      const req = httpMock.expectOne(r => r.url === base);
      expect(req.request.params.get('sort')).toBe('orderDate,desc');
      req.flush(emptyPage);
    });

    it('includes statuses param as comma-separated string when statuses are provided', () => {
      service.getOrders('2026-01-01', '2026-01-31', '', ['CONFIRMED', 'SHIPPED'], 0, 10).subscribe();

      const req = httpMock.expectOne(r => r.url === base);
      expect(req.request.params.get('statuses')).toBe('CONFIRMED,SHIPPED');
      req.flush(emptyPage);
    });

    it('omits statuses param when array is empty', () => {
      service.getOrders('2026-01-01', '2026-01-31', '', [], 0, 10).subscribe();

      const req = httpMock.expectOne(r => r.url === base);
      expect(req.request.params.has('statuses')).toBeFalse();
      req.flush(emptyPage);
    });

    it('returns the page response from the API', () => {
      const mock: PageResponse<OrderSummary> = {
        content: [{ id: 1, orderDate: '2026-01-10', customerName: 'Alice Martin', totalAmount: 199.99, status: 'DELIVERED' }],
        totalElements: 1, totalPages: 1, size: 10, number: 0,
      };

      service.getOrders('2026-01-01', '2026-01-31', '', [], 0, 10)
        .subscribe(data => expect(data).toEqual(mock));

      httpMock.expectOne(r => r.url === base).flush(mock);
    });
  });

  describe('getOrder()', () => {
    it('calls GET /orders/:id', () => {
      service.getOrder(42).subscribe();

      const req = httpMock.expectOne(`${base}/42`);
      expect(req.request.method).toBe('GET');
      req.flush({});
    });

    it('returns the order detail from the API', () => {
      const mock: OrderDetail = {
        id: 42,
        orderDate: '2026-01-15',
        totalAmount: 299.99,
        status: 'DELIVERED',
        customer: { id: 1, name: 'Alice Martin', email: 'alice.martin@example.fr', city: 'Paris', orderCount: 5, lifetimeValue: 890.00 },
        itemCount: 3,
        items: [],
        subtotal: 290.00,
        shippingAmount: 9.99,
        paymentMethod: 'Credit Card',
      };

      service.getOrder(42).subscribe(data => expect(data).toEqual(mock));

      httpMock.expectOne(`${base}/42`).flush(mock);
    });
  });
});
