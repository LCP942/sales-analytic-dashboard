import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FilterStripComponent } from './filter-strip.component';
import { FilterService } from '../../../core/services/filter.service';

describe('FilterStripComponent', () => {
  let fixture: ComponentFixture<FilterStripComponent>;
  let el: HTMLElement;
  let filterService: FilterService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilterStripComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterStripComponent);
    filterService = TestBed.inject(FilterService);
    fixture.detectChanges();
    el = fixture.nativeElement;
  });

  it('renders 4 preset buttons', () => {
    expect(el.querySelectorAll('.filter-strip__btn').length).toBe(4);
  });

  it('renders preset labels in order', () => {
    const labels = Array.from(el.querySelectorAll('.filter-strip__btn')).map(b => b.textContent?.trim());
    expect(labels).toEqual(['Today', 'Last 7 days', 'Last 30 days', 'Last 12 months']);
  });

  it('marks the active preset button with .active', () => {
    filterService.setPreset('7d');
    fixture.detectChanges();
    const active = Array.from(el.querySelectorAll('.filter-strip__btn')).find(b => b.classList.contains('active'));
    expect(active?.textContent?.trim()).toBe('Last 7 days');
  });

  it('calls FilterService.setPreset when a preset button is clicked', () => {
    spyOn(filterService, 'setPreset');
    const buttons = el.querySelectorAll<HTMLButtonElement>('.filter-strip__btn');
    buttons[0].click();
    expect(filterService.setPreset).toHaveBeenCalledWith('today');
  });

  it('calls setCustomRange when a valid from date is selected (from ≤ to)', () => {
    spyOn(filterService, 'setCustomRange');
    filterService.to.set('2026-12-31');
    (fixture.componentInstance as any).onFromChange(new Date('2026-01-01'));
    expect(filterService.setCustomRange).toHaveBeenCalledWith('2026-01-01', '2026-12-31');
  });

  it('does not call setCustomRange when from date is after to date', () => {
    spyOn(filterService, 'setCustomRange');
    filterService.to.set('2026-01-01');
    (fixture.componentInstance as any).onFromChange(new Date('2026-06-01'));
    expect(filterService.setCustomRange).not.toHaveBeenCalled();
  });

  it('calls setCustomRange when a valid to date is selected (from ≤ to)', () => {
    spyOn(filterService, 'setCustomRange');
    filterService.from.set('2026-01-01');
    (fixture.componentInstance as any).onToChange(new Date('2026-03-31'));
    expect(filterService.setCustomRange).toHaveBeenCalledWith('2026-01-01', '2026-03-31');
  });

  it('does not call setCustomRange when to date is before from date', () => {
    spyOn(filterService, 'setCustomRange');
    filterService.from.set('2026-06-01');
    (fixture.componentInstance as any).onToChange(new Date('2026-01-01'));
    expect(filterService.setCustomRange).not.toHaveBeenCalled();
  });
});
