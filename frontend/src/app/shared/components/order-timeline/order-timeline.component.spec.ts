import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderTimelineComponent } from './order-timeline.component';
import { OrderStatus } from '../../../core/models/order.models';

describe('OrderTimelineComponent', () => {
  let fixture: ComponentFixture<OrderTimelineComponent>;
  let el: HTMLElement;

  function create(status: OrderStatus): void {
    fixture = TestBed.createComponent(OrderTimelineComponent);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderTimelineComponent],
    }).compileComponents();
  });

  describe('when status is SHIPPED', () => {
    beforeEach(() => create('SHIPPED'));

    it('renders 4 timeline steps', () => {
      expect(el.querySelectorAll('.timeline__step').length).toBe(4);
    });

    it('marks Pending and Confirmed as done', () => {
      const steps = el.querySelectorAll('.timeline__step');
      expect(steps[0].getAttribute('data-state')).toBe('done');
      expect(steps[1].getAttribute('data-state')).toBe('done');
    });

    it('marks Shipped as active with aria-current="step"', () => {
      const steps = el.querySelectorAll('.timeline__step');
      expect(steps[2].getAttribute('data-state')).toBe('active');
      expect(steps[2].getAttribute('aria-current')).toBe('step');
    });

    it('marks Delivered as upcoming', () => {
      const steps = el.querySelectorAll('.timeline__step');
      expect(steps[3].getAttribute('data-state')).toBe('upcoming');
    });

    it('only the active step has aria-current', () => {
      const withAriaCurrent = Array.from(el.querySelectorAll('[aria-current="step"]'));
      expect(withAriaCurrent.length).toBe(1);
    });
  });

  describe('when status is DELIVERED', () => {
    beforeEach(() => create('DELIVERED'));

    it('marks Pending, Confirmed and Shipped as done', () => {
      const steps = el.querySelectorAll('.timeline__step');
      expect(steps[0].getAttribute('data-state')).toBe('done');
      expect(steps[1].getAttribute('data-state')).toBe('done');
      expect(steps[2].getAttribute('data-state')).toBe('done');
    });

    it('marks Delivered as active', () => {
      const steps = el.querySelectorAll('.timeline__step');
      expect(steps[3].getAttribute('data-state')).toBe('active');
    });
  });

  describe('when status is PENDING', () => {
    beforeEach(() => create('PENDING'));

    it('marks Pending as active and all others as upcoming', () => {
      const steps = el.querySelectorAll('.timeline__step');
      expect(steps[0].getAttribute('data-state')).toBe('active');
      expect(steps[1].getAttribute('data-state')).toBe('upcoming');
      expect(steps[2].getAttribute('data-state')).toBe('upcoming');
      expect(steps[3].getAttribute('data-state')).toBe('upcoming');
    });
  });

  describe('when status is CANCELLED', () => {
    beforeEach(() => create('CANCELLED'));

    it('shows the cancelled terminal badge instead of the stepper', () => {
      expect(el.querySelector('.timeline-cancelled')).toBeTruthy();
      expect(el.querySelector('.timeline')).toBeNull();
    });

    it('cancelled badge has role="status"', () => {
      expect(el.querySelector('.timeline-cancelled__badge')?.getAttribute('role')).toBe('status');
    });

    it('cancelled badge has descriptive aria-label', () => {
      expect(el.querySelector('.timeline-cancelled__badge')?.getAttribute('aria-label'))
        .toBe('Order status: Cancelled');
    });
  });
});
