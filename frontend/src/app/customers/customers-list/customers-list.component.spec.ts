import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of } from 'rxjs';

import { CustomersListComponent } from './customers-list.component';
import { CustomersService } from '../services/customers.service';
import { CustomerSummary } from '../../core/models/customer.models';
import { PageResponse } from '../../core/models/order.models';

function makePage(items: CustomerSummary[], total = items.length): PageResponse<CustomerSummary> {
  return { content: items, totalElements: total, totalPages: Math.ceil(total / 20), size: 20, number: 0 };
}

const alice: CustomerSummary = { id: 1, name: 'Alice Martin', email: 'alice@test.com', city: 'Paris', orderCount: 3, lifetimeValue: 900 };
const bob:   CustomerSummary = { id: 2, name: 'Bob Dupont',   email: 'bob@test.com',   city: 'Lyon',  orderCount: 1, lifetimeValue: 200 };

describe('CustomersListComponent', () => {
  let fixture: ComponentFixture<CustomersListComponent>;
  let component: CustomersListComponent;
  let el: HTMLElement;
  let serviceSpy: jasmine.SpyObj<CustomersService>;
  let router: Router;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj('CustomersService', ['getCustomers']);
    serviceSpy.getCustomers.and.returnValue(of(makePage([alice, bob], 2)));

    await TestBed.configureTestingModule({
      imports: [CustomersListComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        { provide: CustomersService, useValue: serviceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomersListComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement;
    router = TestBed.inject(Router);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('loads customers on init', () => {
    expect(serviceSpy.getCustomers).toHaveBeenCalledWith('', 0, 20);
    expect(component.customers()).toHaveSize(2);
  });

  it('renders a row for each customer', () => {
    const rows = el.querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(2);
  });

  it('displays customer names in the table', () => {
    expect(el.textContent).toContain('Alice Martin');
    expect(el.textContent).toContain('Bob Dupont');
  });

  it('renders the search input', () => {
    expect(el.querySelector('.search-bar__input')).toBeTruthy();
  });

  it('onSearchInput() updates searchName and triggers debounced load', fakeAsync(() => {
    serviceSpy.getCustomers.calls.reset();
    const input = el.querySelector<HTMLInputElement>('.search-bar__input')!;
    input.value = 'Alice';
    input.dispatchEvent(new Event('input'));
    tick(300);
    expect(component.searchName).toBe('Alice');
    expect(serviceSpy.getCustomers).toHaveBeenCalledWith('Alice', 0, 20);
  }));

  it('clearSearch() resets searchName and triggers debounced load', fakeAsync(() => {
    component.searchName = 'Alice';
    serviceSpy.getCustomers.calls.reset();
    component.clearSearch();
    tick(300);
    expect(component.searchName).toBe('');
    expect(serviceSpy.getCustomers).toHaveBeenCalledWith('', 0, 20);
  }));

  it('clear button appears only when searchName is non-empty', fakeAsync(() => {
    expect(el.querySelector('.search-bar__clear')).toBeNull();
    component.searchName = 'test';
    fixture.detectChanges();
    expect(el.querySelector('.search-bar__clear')).toBeTruthy();
  }));

  it('openCustomer() navigates to /customers/:id', () => {
    const spy = spyOn(router, 'navigate');
    component.openCustomer(1);
    expect(spy).toHaveBeenCalledWith(['/customers', 1]);
  });

  it('createCustomer() navigates to /customers/new', () => {
    const spy = spyOn(router, 'navigate');
    component.createCustomer();
    expect(spy).toHaveBeenCalledWith(['/customers/new']);
  });

  it('resets page to 0 on new search', fakeAsync(() => {
    component.pageIndex.set(2);
    const input = el.querySelector<HTMLInputElement>('.search-bar__input')!;
    input.value = 'Bob';
    input.dispatchEvent(new Event('input'));
    tick(300);
    expect(component.pageIndex()).toBe(0);
  }));
});
