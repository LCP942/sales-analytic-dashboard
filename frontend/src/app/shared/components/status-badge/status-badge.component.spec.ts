import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent } from './status-badge.component';
import { OrderStatus } from '../../../core/models/order.models';

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<StatusBadgeComponent>;
  let el: HTMLElement;

  function create(status: OrderStatus): void {
    fixture = TestBed.createComponent(StatusBadgeComponent);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();
  });

  it('sets data-status attribute to the given status', () => {
    create('DELIVERED');
    expect(el.querySelector('.status-badge')?.getAttribute('data-status')).toBe('DELIVERED');
  });

  it('renders the correct human-readable label', () => {
    const cases: [OrderStatus, string][] = [
      ['PENDING',   'Pending'],
      ['CONFIRMED', 'Confirmed'],
      ['SHIPPED',   'Shipped'],
      ['DELIVERED', 'Delivered'],
      ['CANCELLED', 'Cancelled'],
    ];
    for (const [status, label] of cases) {
      create(status);
      expect(el.querySelector('.status-badge')?.textContent?.trim()).toContain(label);
    }
  });

  it('sets aria-label with the status name', () => {
    create('SHIPPED');
    expect(el.querySelector('.status-badge')?.getAttribute('aria-label')).toBe('Order status: Shipped');
  });

  it('has role="status" on the badge element', () => {
    create('PENDING');
    expect(el.querySelector('.status-badge')?.getAttribute('role')).toBe('status');
  });

  it('renders a dot element marked aria-hidden', () => {
    create('CONFIRMED');
    expect(el.querySelector('.status-badge__dot')?.getAttribute('aria-hidden')).toBe('true');
  });
});
