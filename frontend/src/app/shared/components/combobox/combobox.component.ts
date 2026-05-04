import {
  ChangeDetectionStrategy, Component, ElementRef, EventEmitter,
  HostListener, Input, Output, ViewChild, forwardRef, inject, signal,
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { Observable, Subject, catchError, debounceTime, distinctUntilChanged, merge, of, switchMap, tap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SelectOption } from '../../models/select.models';

@Component({
  selector: 'app-combobox',
  standalone: true,
  imports: [MatIconModule, ReactiveFormsModule],
  templateUrl: './combobox.component.html',
  styleUrl: './combobox.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'relative block' },
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ComboboxComponent),
    multi: true,
  }],
})
export class ComboboxComponent implements ControlValueAccessor {
  /** Called with a search string, must return up to 10 options. */
  @Input({ required: true }) searchFn!: (query: string) => Observable<SelectOption[]>;
  @Input() placeholder = 'Search…';
  @Output() readonly selectionChange = new EventEmitter<any>();

  @ViewChild('searchInput') private searchInputRef?: ElementRef<HTMLInputElement>;

  /** Pre-selected option provided by the parent (e.g. pre-filled from a route param). */
  @Input() set preset(opt: SelectOption | null) {
    this._preset.set(opt);
    if (opt) this.selectedOpt.set(opt);
  }

  protected readonly isOpen      = signal(false);
  protected readonly disabled    = signal(false);
  protected readonly loading     = signal(false);
  protected readonly results     = signal<SelectOption[]>([]);
  protected readonly selectedOpt = signal<SelectOption | null>(null);
  private readonly _preset       = signal<SelectOption | null>(null);

  protected readonly searchControl = new FormControl('');

  private readonly el = inject(ElementRef);
  private onChange: (v: any) => void = () => {};
  private onTouched = () => {};

  // Fired immediately (no debounce) when the dropdown opens to load initial results.
  private readonly openTrigger$ = new Subject<string>();

  constructor() {
    merge(
      // Initial load on open — no debounce
      this.openTrigger$,
      // Incremental search — debounced
      this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged()),
    ).pipe(
      tap(() => this.loading.set(true)),
      switchMap(q => this.searchFn(q ?? '').pipe(catchError(() => of([])))),
      takeUntilDestroyed(),
    ).subscribe(opts => {
      this.results.set(opts);
      this.loading.set(false);
    });
  }

  protected open(): void {
    if (this.disabled() || this.isOpen()) return;
    this.isOpen.set(true);
    this.onTouched();
    this.searchControl.setValue('', { emitEvent: false });
    this.openTrigger$.next('');
    // Defer focus so the input is rendered first
    setTimeout(() => this.searchInputRef?.nativeElement.focus(), 0);
  }

  protected close(): void {
    this.isOpen.set(false);
    this.searchControl.setValue('', { emitEvent: false });
  }

  protected select(opt: SelectOption): void {
    this.selectedOpt.set(opt);
    this.onChange(opt.value);
    this.selectionChange.emit(opt.value);
    this.close();
  }

  protected clear(event: MouseEvent): void {
    event.stopPropagation();
    this.selectedOpt.set(null);
    this.onChange(null);
    this.onTouched();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') { this.close(); this.onTouched(); }
  }

  @HostListener('document:click', ['$event.target'])
  protected onOutsideClick(target: EventTarget | null): void {
    if (target && !this.el.nativeElement.contains(target as Node)) this.close();
  }

  writeValue(v: any): void {
    if (v == null) {
      this.selectedOpt.set(null);
      return;
    }
    // If a preset was provided and matches, keep its label instead of going blank.
    const preset = this._preset();
    if (preset && preset.value === v) {
      this.selectedOpt.set(preset);
    } else if (this.selectedOpt()?.value !== v) {
      this.selectedOpt.set(null);
    }
  }
  registerOnChange(fn: any): void   { this.onChange = fn; }
  registerOnTouched(fn: any): void  { this.onTouched = fn; }
  setDisabledState(d: boolean): void { this.disabled.set(d); }
}
