import {
  ChangeDetectionStrategy, Component, ElementRef, EventEmitter,
  HostListener, Input, Output, forwardRef, inject, signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { SelectOption } from '../../models/select.models';

@Component({
  selector: 'app-select',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './select.component.html',
  styleUrl: './select.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'relative block' },
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SelectComponent),
    multi: true,
  }],
})
export class SelectComponent implements ControlValueAccessor {
  @Input({ required: true }) options: SelectOption[] = [];
  @Input() placeholder = 'Select…';
  @Output() readonly selectionChange = new EventEmitter<any>();

  protected readonly value    = signal<any>(null);
  protected readonly isOpen   = signal(false);
  protected readonly disabled = signal(false);

  private readonly el = inject(ElementRef);
  private onChange: (v: any) => void = () => {};
  private onTouched = () => {};

  protected get displayLabel(): string {
    return this.options.find(o => o.value === this.value())?.label ?? '';
  }

  protected toggle(): void {
    if (this.disabled()) return;
    this.isOpen.update(v => !v);
    if (!this.isOpen()) this.onTouched();
  }

  protected select(opt: SelectOption): void {
    this.value.set(opt.value);
    this.onChange(opt.value);
    this.onTouched();
    this.isOpen.set(false);
    this.selectionChange.emit(opt.value);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') { this.isOpen.set(false); this.onTouched(); }
  }

  @HostListener('document:click', ['$event.target'])
  protected onOutsideClick(target: EventTarget | null): void {
    if (target && !this.el.nativeElement.contains(target as Node)) this.isOpen.set(false);
  }

  writeValue(v: any): void        { this.value.set(v); }
  registerOnChange(fn: any): void  { this.onChange = fn; }
  registerOnTouched(fn: any): void { this.onTouched = fn; }
  setDisabledState(d: boolean): void { this.disabled.set(d); }
}
