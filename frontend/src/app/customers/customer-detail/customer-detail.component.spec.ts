import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';

import { CustomerDetailComponent } from './customer-detail.component';
import { CustomersService } from '../services/customers.service';
import { CustomerSummary } from '../../core/models/customer.models';

const mockCustomer: CustomerSummary = {
  id: 1, name: 'Alice Martin', email: 'alice@example.com', city: 'Paris',
  orderCount: 5, lifetimeValue: 1250,
};

describe('CustomerDetailComponent', () => {
  let fixture: ComponentFixture<CustomerDetailComponent>;
  let component: CustomerDetailComponent;
  let el: HTMLElement;
  let serviceSpy: jasmine.SpyObj<CustomersService>;
  let router: Router;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj('CustomersService', ['getCustomer']);
    serviceSpy.getCustomer.and.returnValue(of(mockCustomer));

    await TestBed.configureTestingModule({
      imports: [CustomerDetailComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        { provide: CustomersService, useValue: serviceSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerDetailComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('displays the customer name', () => {
    expect(el.textContent).toContain('Alice Martin');
  });

  it('displays the city', () => {
    expect(el.textContent).toContain('Paris');
  });

  it('displays the email address', () => {
    expect(el.textContent).toContain('alice@example.com');
  });

  it('displays order count KPI', () => {
    expect(el.textContent).toContain('5');
  });

  it('viewOrders() navigates to /orders with customer query param', () => {
    const spy = spyOn(router, 'navigate');
    component.viewOrders();
    expect(spy).toHaveBeenCalledOnceWith(
      ['/orders'],
      { queryParams: { customer: 'Alice Martin' } },
    );
  });

  it('createOrder() navigates to /orders/new with customerId query param', () => {
    const spy = spyOn(router, 'navigate');
    component.createOrder();
    expect(spy).toHaveBeenCalledOnceWith(
      ['/orders/new'],
      { queryParams: { customerId: 1, customerName: 'Alice Martin', customerCity: 'Paris' } },
    );
  });

  it('shows error state when service fails', async () => {
    serviceSpy.getCustomer.and.returnValue(throwError(() => new Error('not found')));
    fixture = TestBed.createComponent(CustomerDetailComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.textContent).toContain('Customer not found');
  });
});
