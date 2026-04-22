import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderFiltersComponent, OrderFilters } from './order-filters.component';

describe('OrderFiltersComponent', () => {
  let fixture: ComponentFixture<OrderFiltersComponent>;
  let component: OrderFiltersComponent;
  let emitted: OrderFilters[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderFiltersComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderFiltersComponent);
    component = fixture.componentInstance;
    emitted = [];
    component.filtersChange.subscribe(v => emitted.push(v));
    fixture.detectChanges();
  });

  it('starts with ALL chip selected and no active status filters', () => {
    expect(component.chipValues()).toEqual(['ALL']);
  });

  it('emits selected statuses when status chips are chosen', () => {
    component.onChipChange({ value: ['CONFIRMED', 'SHIPPED'] } as any);
    expect(emitted.at(-1)).toEqual({ statuses: ['CONFIRMED', 'SHIPPED'], search: '' });
    expect(component.chipValues()).toEqual(['CONFIRMED', 'SHIPPED']);
  });

  it('resets to ALL when user explicitly clicks the All chip', () => {
    component.onChipChange({ value: ['CONFIRMED'] } as any);
    component.onChipChange({ value: ['CONFIRMED', 'ALL'] } as any);
    expect(emitted.at(-1)).toEqual({ statuses: [], search: '' });
    expect(component.chipValues()).toEqual(['ALL']);
  });

  it('falls back to ALL when all status chips are deselected', () => {
    component.onChipChange({ value: ['PENDING'] } as any);
    component.onChipChange({ value: [] } as any);
    expect(component.chipValues()).toEqual(['ALL']);
    expect(emitted.at(-1)).toEqual({ statuses: [], search: '' });
  });

  it('reset() restores ALL chip and emits empty filters', () => {
    component.onChipChange({ value: ['DELIVERED', 'SHIPPED'] } as any);
    component.reset();
    expect(component.chipValues()).toEqual(['ALL']);
    expect(emitted.at(-1)).toEqual({ statuses: [], search: '' });
  });

  it('reset() also clears the search input', () => {
    component.searchControl.setValue('test customer');
    component.reset();
    expect(component.searchControl.value).toBe('');
  });

  it('includes the current search value in emitted filters', () => {
    component.searchControl.setValue('Alice', { emitEvent: false });
    component.onChipChange({ value: ['PENDING'] } as any);
    expect(emitted.at(-1)?.search).toBe('Alice');
  });
});
