import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { provideHttpClient } from '@angular/common/http';

import { OrderCreateComponent } from './order-create.component';
import { OrdersService } from '../services/orders.service';
import { CustomersService } from '../../customers/services/customers.service';
import { Product } from '../../core/models/product.models';
import { CustomerSummary } from '../../core/models/customer.models';
import { OrderDetail } from '../../core/models/order.models';

registerLocaleData(localeFr);

const products: Product[] = [
  { id: 1, name: 'Laptop Pro', category: 'Electronics', price: 1299.99 },
  { id: 2, name: 'Headphones', category: 'Electronics', price: 149.99 },
];

const customers: CustomerSummary[] = [
  { id: 1, name: 'Alice Martin', email: 'alice@test.com', city: 'Paris', orderCount: 0, lifetimeValue: 0 },
];

const mockOrderDetail: OrderDetail = {
  id: 100001,
  orderDate: '2026-05-01',
  totalAmount: 1305.98,
  status: 'PENDING',
  customer: customers[0] as any,
  itemCount: 1,
  items: [],
  subtotal: 1299.99,
  shippingAmount: 5.99,
  paymentMethod: 'Credit Card',
};

describe('OrderCreateComponent', () => {
  let fixture: ComponentFixture<OrderCreateComponent>;
  let component: OrderCreateComponent;
  let el: HTMLElement;
  let ordersSpy: jasmine.SpyObj<OrdersService>;
  let customersSpy: jasmine.SpyObj<CustomersService>;
  let httpSpy: jasmine.SpyObj<HttpClient>;
  let router: Router;

  beforeEach(async () => {
    ordersSpy    = jasmine.createSpyObj('OrdersService',    ['createOrder']);
    customersSpy = jasmine.createSpyObj('CustomersService', ['getCustomers']);
    httpSpy      = jasmine.createSpyObj('HttpClient',       ['get']);

    customersSpy.getCustomers.and.returnValue(of({ content: customers, totalElements: 1, totalPages: 1, size: 200, number: 0 }));
    httpSpy.get.and.returnValue(of(products));

    await TestBed.configureTestingModule({
      imports: [OrderCreateComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        provideHttpClient(),
        { provide: OrdersService,    useValue: ordersSpy },
        { provide: CustomersService, useValue: customersSpy },
        { provide: HttpClient,       useValue: httpSpy },
        { provide: ActivatedRoute,   useValue: { snapshot: { queryParams: {} } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderCreateComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement;
    router = TestBed.inject(Router);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  // ── Form structure ────────────────────────────────────────────────────────

  it('starts with one empty line item after loading', () => {
    expect(component.itemsArray.length).toBe(1);
  });

  it('submit is disabled when form has no valid line items', () => {
    const btn = el.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    expect(btn.disabled).toBeTrue();
  });

  // ── addItem / removeItem ──────────────────────────────────────────────────

  it('addItem() adds a new line to itemsArray', () => {
    component.addItem();
    expect(component.itemsArray.length).toBe(2);
  });

  it('removeItem() removes the line at the given index', () => {
    component.addItem(); // now 2 items
    component.removeItem(0);
    expect(component.itemsArray.length).toBe(1);
  });

  // ── onProductChange ───────────────────────────────────────────────────────

  it('onProductChange() sets the unitPrice from the product catalogue', () => {
    component.products.set(products);
    component.onProductChange(0, 1); // select Laptop Pro (id=1)
    expect(component.itemsArray.at(0).get('unitPrice')?.value).toBe(1299.99);
  });

  it('onProductChange() sets unitPrice for the second line correctly', () => {
    component.addItem();
    component.products.set(products);
    component.onProductChange(1, 2); // second line → Headphones
    expect(component.itemsArray.at(1).get('unitPrice')?.value).toBe(149.99);
  });

  // ── orderTotal ────────────────────────────────────────────────────────────

  it('orderTotal returns 0 when items array is empty', () => {
    component.removeItem(0);
    component.form.controls.shippingAmount.setValue(0);
    expect(component.orderTotal).toBe(0);
  });

  it('orderTotal sums items and adds shipping', () => {
    component.itemsArray.at(0).patchValue({ quantity: 2, unitPrice: 100 });
    component.form.controls.shippingAmount.setValue(9.99);
    expect(component.orderTotal).toBeCloseTo(209.99, 2);
  });

  it('itemTotal() returns qty × unitPrice for a control', () => {
    const ctrl = component.itemsArray.at(0);
    ctrl.patchValue({ quantity: 3, unitPrice: 50 });
    expect(component.itemTotal(ctrl)).toBe(150);
  });

  // ── submit ────────────────────────────────────────────────────────────────

  it('does not call service when form is invalid', () => {
    component.submit();
    expect(ordersSpy.createOrder).not.toHaveBeenCalled();
  });

  it('calls createOrder with correct payload on valid submit', fakeAsync(() => {
    spyOn(router, 'navigate');
    ordersSpy.createOrder.and.returnValue(of(mockOrderDetail));
    component.form.patchValue({
      customerId: 1, status: 'PENDING', paymentMethod: 'Credit Card', shippingAmount: 5.99,
    });
    component.itemsArray.at(0).patchValue({ productId: 1, quantity: 1, unitPrice: 1299.99 });
    fixture.detectChanges();
    component.submit();
    tick();
    expect(ordersSpy.createOrder).toHaveBeenCalledTimes(1);
    const req = ordersSpy.createOrder.calls.first().args[0];
    expect(req.customerId).toBe(1);
    expect(req.shippingAmount).toBe(5.99);
    expect(req.items[0]).toEqual(jasmine.objectContaining({ productId: 1, quantity: 1, unitPrice: 1299.99 }));
  }));

  it('resets saving to false on service error', fakeAsync(() => {
    ordersSpy.createOrder.and.returnValue(throwError(() => new Error('err')));
    component.form.patchValue({
      customerId: 1, status: 'PENDING', paymentMethod: 'Credit Card', shippingAmount: 0,
    });
    component.itemsArray.at(0).patchValue({ productId: 1, quantity: 1, unitPrice: 100 });
    component.submit();
    tick();
    expect(component.saving()).toBeFalse();
  }));
});
