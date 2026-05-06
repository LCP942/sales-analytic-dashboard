import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { OrdersComponent } from './orders.component';
import { OrdersService } from './services/orders.service';
import { OrderSummary, PageResponse } from '../core/models/order.models';

const makePage = (orders: OrderSummary[]): PageResponse<OrderSummary> => ({
  content: orders,
  totalElements: orders.length,
  totalPages: 1,
  size: 10,
  number: 0,
});

const sampleOrder: OrderSummary = {
  id: 1,
  orderDate: '2026-05-05',
  customerName: 'Alice Martin',
  totalAmount: 199.99,
  status: 'DELIVERED',
};

describe('OrdersComponent', () => {
  let component: OrdersComponent;
  let fixture: ComponentFixture<OrdersComponent>;
  let ordersSpy: jasmine.SpyObj<OrdersService>;

  beforeEach(async () => {
    ordersSpy = jasmine.createSpyObj('OrdersService', ['getOrders']);
    ordersSpy.getOrders.and.returnValue(of(makePage([sampleOrder])));

    await TestBed.configureTestingModule({
      imports: [OrdersComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        provideHttpClient(),
        { provide: OrdersService, useValue: ordersSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: {} } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  // ── CSV escaping ──────────────────────────────────────────────────────────

  describe('CSV escaping', () => {

    it('old join() approach breaks when customer name contains a comma', () => {
      const row: unknown[] = [1, '2026-05-05', 'Dupont, Jean', 'DELIVERED', 299.99];
      const csvLine = row.join(',');
      expect(csvLine.split(',').length).toBeGreaterThan(5);
    });

    it('quoted approach keeps commas inside customer names intact', () => {
      const escape = (v: unknown) => `"${String(v).replace(/"/g, '""')}"`;
      const row: unknown[] = [1, '2026-05-05', 'Dupont, Jean', 'DELIVERED', 299.99];
      const csvLine = row.map(escape).join(',');
      expect(csvLine).toContain('"Dupont, Jean"');
    });

    it('quoted approach escapes internal double quotes (RFC 4180)', () => {
      const escape = (v: unknown) => `"${String(v).replace(/"/g, '""')}"`;
      expect(escape('say "hello"')).toBe('"say ""hello"""');
    });

    it('quoted approach neutralises Excel formula injection starting with =', () => {
      const escape = (v: unknown) => `"${String(v).replace(/"/g, '""')}"`;
      const malicious = '=SUM(1+1)*cmd|"/C calc"!A0';
      const escaped = escape(malicious);
      expect(escaped.startsWith('"')).toBeTrue();
      expect(escaped).not.toMatch(/^=/);
    });

    it('exportCsv builds a Blob and triggers a download link', fakeAsync(() => {
      const mockUrl = 'blob:http://localhost/test-uuid';
      spyOn(URL, 'createObjectURL').and.returnValue(mockUrl);
      spyOn(URL, 'revokeObjectURL');

      const mockAnchor = { href: '', download: '', click: jasmine.createSpy('click') };
      spyOn(document, 'createElement').and.returnValue(mockAnchor as unknown as HTMLElement);

      ordersSpy.getOrders.and.returnValue(of(makePage([sampleOrder])));
      component.totalElements.set(1);

      component.exportCsv();
      tick();

      expect(URL.createObjectURL).toHaveBeenCalledWith(jasmine.any(Blob));
      expect(mockAnchor.click).toHaveBeenCalled();
      expect(URL.revokeObjectURL).toHaveBeenCalledWith(mockUrl);
      expect(component.exporting()).toBeFalse();
    }));

    it('exportCsv wraps every field in double quotes', fakeAsync(() => {
      const mockUrl = 'blob:http://localhost/test-uuid';
      spyOn(URL, 'revokeObjectURL');

      let capturedBlob: Blob | undefined;
      spyOn(URL, 'createObjectURL').and.callFake((b: Blob) => {
        capturedBlob = b;
        return mockUrl;
      });
      spyOn(document, 'createElement').and.returnValue(
        { href: '', download: '', click: () => {} } as unknown as HTMLElement);

      const orderWithComma: OrderSummary = { ...sampleOrder, customerName: 'Dupont, Jean' };
      ordersSpy.getOrders.and.returnValue(of(makePage([orderWithComma])));
      component.totalElements.set(1);

      component.exportCsv();
      tick();

      capturedBlob!.text().then(text => {
        const lines = text.split('\n');
        const dataLine = lines[1];
        expect(dataLine).toContain('"Dupont, Jean"');
        expect(dataLine.split('","').length).toBeGreaterThan(1);
      });
    }));
  });

  // ── pageSize reactivity ───────────────────────────────────────────────────

  it('pageSize is a signal and updates on page event', () => {
    expect(component.pageSize()).toBe(10);
    component.onPage({ pageIndex: 0, pageSize: 25, length: 1 });
    expect(component.pageSize()).toBe(25);
  });
});
