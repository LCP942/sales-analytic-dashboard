import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CustomerCardComponent } from './customer-card.component';
import { Customer } from '../../../core/models/order.models';

registerLocaleData(localeFr);

const mockCustomer: Customer = {
  id: 1,
  name: 'François Martin',
  email: 'francois.martin@example.fr',
  city: 'Paris',
  orderCount: 7,
  lifetimeValue: 1240.50,
};

describe('CustomerCardComponent', () => {
  let fixture: ComponentFixture<CustomerCardComponent>;
  let el: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerCardComponent);
    fixture.componentRef.setInput('customer', mockCustomer);
    fixture.detectChanges();
    el = fixture.nativeElement;
  });

  it('displays the uppercase first initial of the customer name', () => {
    expect(el.querySelector('.customer-card__avatar')?.textContent?.trim()).toBe('F');
  });

  it('displays the full customer name', () => {
    expect(el.querySelector('.customer-card__name')?.textContent?.trim()).toBe('François Martin');
  });

  it('displays the email address', () => {
    expect(el.textContent).toContain('francois.martin@example.fr');
  });

  it('displays the city', () => {
    expect(el.textContent).toContain('Paris');
  });

  it('has role="region" and aria-label="Customer information" on the card', () => {
    const card = el.querySelector('.customer-card');
    expect(card?.getAttribute('role')).toBe('region');
    expect(card?.getAttribute('aria-label')).toBe('Customer information');
  });

  it('marks the avatar as aria-hidden (decorative)', () => {
    expect(el.querySelector('.customer-card__avatar')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('derives initial from single-word name correctly', () => {
    fixture.componentRef.setInput('customer', { ...mockCustomer, name: 'Inès' });
    fixture.detectChanges();
    expect(el.querySelector('.customer-card__avatar')?.textContent?.trim()).toBe('I');
  });

  it('displays orders total count', () => {
    expect(el.textContent).toContain('7 orders total');
  });

  it('uses singular "order" when count is 1', () => {
    fixture.componentRef.setInput('customer', { ...mockCustomer, orderCount: 1 });
    fixture.detectChanges();
    expect(el.textContent).toContain('1 order total');
    expect(el.textContent).not.toContain('1 orders total');
  });

  it('displays lifetime value with EUR currency', () => {
    expect(el.textContent).toContain('1\u202f240,50\u00a0€');
  });
});
