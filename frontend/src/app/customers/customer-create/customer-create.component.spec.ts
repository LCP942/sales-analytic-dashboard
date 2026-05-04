import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';

import { CustomerCreateComponent } from './customer-create.component';
import { CustomersService } from '../services/customers.service';
import { CustomerSummary } from '../../core/models/customer.models';
import { Router } from '@angular/router';

const mockCreated: CustomerSummary = {
  id: 10001, name: 'Alice Martin', email: 'alice@example.com', city: 'Paris',
  orderCount: 0, lifetimeValue: 0,
};

describe('CustomerCreateComponent', () => {
  let fixture: ComponentFixture<CustomerCreateComponent>;
  let component: CustomerCreateComponent;
  let el: HTMLElement;
  let serviceSpy: jasmine.SpyObj<CustomersService>;
  let router: Router;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj('CustomersService', ['createCustomer']);

    await TestBed.configureTestingModule({
      imports: [CustomerCreateComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        { provide: CustomersService, useValue: serviceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerCreateComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('renders the form with name, email and city inputs', () => {
    expect(el.querySelector('#cc-name')).toBeTruthy();
    expect(el.querySelector('#cc-email')).toBeTruthy();
    expect(el.querySelector('#cc-city')).toBeTruthy();
  });

  it('submit button is disabled when the form is empty', () => {
    const btn = el.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    expect(btn.disabled).toBeTrue();
  });

  it('submit button is enabled when all fields are valid', () => {
    component.form.setValue({ name: 'Alice', email: 'alice@test.com', city: 'Paris' });
    fixture.detectChanges();
    const btn = el.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    expect(btn.disabled).toBeFalse();
  });

  it('shows "Name is required" error after touching the name field', () => {
    component.form.controls.name.markAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('Name is required');
  });

  it('shows "Name must be at least 2 characters" when name is 1 char', () => {
    component.form.controls.name.setValue('A');
    component.form.controls.name.markAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('Name must be at least 2 characters');
  });

  it('shows "Email is required" after touching the email field empty', () => {
    component.form.controls.email.markAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('Email is required');
  });

  it('shows "Enter a valid email address" for a malformed email', () => {
    component.form.controls.email.setValue('not-an-email');
    component.form.controls.email.markAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('Enter a valid email address');
  });

  it('shows "City is required" after touching the city field empty', () => {
    component.form.controls.city.markAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('City is required');
  });

  it('calls createCustomer with correct payload on submit', fakeAsync(() => {
    spyOn(router, 'navigate');
    serviceSpy.createCustomer.and.returnValue(of(mockCreated));
    component.form.setValue({ name: 'Alice Martin', email: 'alice@example.com', city: 'Paris' });
    component.submit();
    tick();
    expect(serviceSpy.createCustomer).toHaveBeenCalledOnceWith({
      name: 'Alice Martin', email: 'alice@example.com', city: 'Paris',
    });
  }));

  it('sets saving to true during the request', fakeAsync(() => {
    spyOn(router, 'navigate');
    serviceSpy.createCustomer.and.returnValue(of(mockCreated));
    component.form.setValue({ name: 'Alice', email: 'a@b.com', city: 'Lyon' });
    component.submit();
    expect(component.saving()).toBeTrue();
    tick();
  }));

  it('does not call service when form is invalid', () => {
    component.submit();
    expect(serviceSpy.createCustomer).not.toHaveBeenCalled();
  });

  it('resets saving to false on service error', fakeAsync(() => {
    serviceSpy.createCustomer.and.returnValue(throwError(() => new Error('err')));
    component.form.setValue({ name: 'Alice', email: 'a@b.com', city: 'Lyon' });
    component.submit();
    tick();
    expect(component.saving()).toBeFalse();
  }));
});
