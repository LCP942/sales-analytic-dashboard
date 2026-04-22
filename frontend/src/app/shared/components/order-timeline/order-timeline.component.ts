import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderStatus } from '../../../core/models/order.models';

type StepState = 'done' | 'active' | 'upcoming';

interface TimelineStep {
  status: OrderStatus;
  label: string;
  state: StepState;
}

const STEPS: { status: OrderStatus; label: string }[] = [
  { status: 'PENDING',   label: 'Pending' },
  { status: 'CONFIRMED', label: 'Confirmed' },
  { status: 'SHIPPED',   label: 'Shipped' },
  { status: 'DELIVERED', label: 'Delivered' },
];

function stepState(stepIndex: number, activeIndex: number): StepState {
  if (stepIndex < activeIndex) return 'done';
  if (stepIndex === activeIndex) return 'active';
  return 'upcoming';
}

@Component({
  selector: 'app-order-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-timeline.component.html',
  styleUrls: ['./order-timeline.component.scss'],
})
export class OrderTimelineComponent {
  status = input.required<OrderStatus>();

  steps = computed<TimelineStep[]>(() => {
    const activeIndex = STEPS.findIndex(s => s.status === this.status());
    return STEPS.map((s, i) => ({
      ...s,
      state: stepState(i, activeIndex),
    }));
  });
}
