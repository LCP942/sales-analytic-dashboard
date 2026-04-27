import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KpiCardsComponent } from './kpi-cards.component';
import { KpiMetrics } from '../../../core/models/stats.models';

const mockKpis: KpiMetrics = {
  revenue: 12500.50,
  orderCount: 84,
  avgOrderValue: 148.82,
  revenueGrowth: 12.5,
  orderGrowth: -3.2,
  avgOrderGrowth: 2.1,
};

describe('KpiCardsComponent', () => {
  let fixture: ComponentFixture<KpiCardsComponent>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KpiCardsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(KpiCardsComponent);
    fixture.componentRef.setInput('kpis', mockKpis);
    fixture.detectChanges();
    compiled = fixture.nativeElement as HTMLElement;
  });

  it('renders three KPI cards', () => {
    const cards = compiled.querySelectorAll('.kpi-card');
    expect(cards.length).toBe(3);
  });

  it('applies .up class when growth > 0', () => {
    const deltas = compiled.querySelectorAll('.kpi-card__delta');
    const upDeltas = Array.from(deltas).filter(d => d.classList.contains('up'));
    expect(upDeltas.length).toBeGreaterThan(0);
  });

  it('applies .down class when growth < 0', () => {
    // orderGrowth is -3.2, which drives the Orders card
    const deltas = compiled.querySelectorAll('.kpi-card__delta');
    const downDeltas = Array.from(deltas).filter(d => d.classList.contains('down'));
    expect(downDeltas.length).toBeGreaterThan(0);
  });

  it('displays revenue formatted with euro sign', () => {
    const values = compiled.querySelectorAll('.kpi-card__value');
    expect(values[0].textContent).toContain('€');
  });

  it('displays order count without currency symbol', () => {
    const values = compiled.querySelectorAll('.kpi-card__value');
    expect(values[1].textContent).not.toContain('€');
  });
});
