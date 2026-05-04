import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormArray, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { map, of } from 'rxjs';

import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { HttpClient } from '@angular/common/http';

import { OrdersService } from '../services/orders.service';
import { CustomersService } from '../../customers/services/customers.service';
import { Product } from '../../core/models/product.models';
import { OrderStatus } from '../../core/models/order.models';
import { environment } from '../../../environments/environment';
import { SelectComponent } from '../../shared/components/select/select.component';
import { ComboboxComponent } from '../../shared/components/combobox/combobox.component';
import { SelectOption } from '../../shared/models/select.models';

@Component({
  selector: 'app-order-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    MatDividerModule,
    SelectComponent,
    ComboboxComponent,
  ],
  templateUrl: './order-create.component.html',
  styleUrls: ['./order-create.component.scss'],
})
export class OrderCreateComponent implements OnInit {
  private readonly ordersService    = inject(OrdersService);
  private readonly customersService = inject(CustomersService);
  private readonly http             = inject(HttpClient);
  private readonly router           = inject(Router);
  private readonly route            = inject(ActivatedRoute);
  private readonly fb               = inject(FormBuilder);
  private readonly snackbar         = inject(MatSnackBar);

  products        = signal<Product[]>([]);
  saving          = signal(false);
  loading         = signal(true);
  customerPreset  = signal<SelectOption | null>(null);

  readonly statusOptions: SelectOption[] = [
    { value: 'PENDING',   label: 'Pending' },
    { value: 'CONFIRMED', label: 'Confirmed' },
    { value: 'SHIPPED',   label: 'Shipped' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  readonly paymentOptions: SelectOption[] = [
    { value: 'Credit Card',    label: 'Credit Card' },
    { value: 'PayPal',         label: 'PayPal' },
    { value: 'Bank Transfer',  label: 'Bank Transfer' },
    { value: 'Cash',           label: 'Cash' },
  ];

  /** Filters the in-memory product list — no backend call needed. */
  readonly searchProducts = (query: string) => {
    const q = query.trim().toLowerCase();
    const all = this.products();
    const filtered = q
      ? all.filter(p => p.name.toLowerCase().includes(q) || p.category.toLowerCase().includes(q))
      : all;
    return of(filtered.slice(0, 10).map(p => ({ value: p.id, label: p.name, sublabel: p.category })));
  };

  /** Debounced search toward the backend, max 10 results. */
  readonly searchCustomers = (query: string) =>
    this.customersService.getCustomers(query, 0, 10).pipe(
      map(page => page.content.map(c => ({ value: c.id, label: c.name, sublabel: c.city }))),
    );

  form = this.fb.group({
    customerId:     [null as number | null, Validators.required],
    orderDate:      [new Date(), Validators.required],
    status:         ['PENDING' as OrderStatus, Validators.required],
    paymentMethod:  ['Credit Card', Validators.required],
    shippingAmount: [0, [Validators.required, Validators.min(0)]],
    items: this.fb.array([], Validators.minLength(1)),
  });

  get itemsArray(): FormArray { return this.form.controls.items as FormArray; }

  itemTotal(ctrl: AbstractControl): number {
    const qty   = ctrl.get('quantity')?.value  ?? 0;
    const price = ctrl.get('unitPrice')?.value ?? 0;
    return qty * price;
  }

  get orderTotal(): number {
    const shipping = this.form.value.shippingAmount ?? 0;
    const items    = (this.form.value.items as any[] ?? []);
    const subtotal = items.reduce((sum, i) => sum + (i.quantity ?? 0) * (i.unitPrice ?? 0), 0);
    return subtotal + Number(shipping);
  }

  ngOnInit(): void {
    const params             = this.route.snapshot.queryParams;
    const preloadCustomerId  = Number(params['customerId'])   || null;
    const preloadName        = params['customerName']  as string | undefined;
    const preloadCity        = params['customerCity']  as string | undefined;

    if (preloadCustomerId && preloadName) {
      const opt: SelectOption = { value: preloadCustomerId, label: preloadName, sublabel: preloadCity };
      this.customerPreset.set(opt);
      this.form.controls.customerId.setValue(preloadCustomerId);
    }

    this.http.get<Product[]>(`${environment.apiBaseUrl}/products`).subscribe({
      next: products => {
        this.products.set(products);
        this.loading.set(false);
        this.addItem();
      },
      error: () => this.loading.set(false),
    });
  }

  addItem(): void {
    this.itemsArray.push(this.fb.group({
      productId: [null as number | null, Validators.required],
      quantity:  [1, [Validators.required, Validators.min(1)]],
      unitPrice: [0, [Validators.required, Validators.min(0.01)]],
    }));
  }

  removeItem(index: number): void {
    this.itemsArray.removeAt(index);
  }

  onProductChange(index: number, productId: number): void {
    const product = this.products().find(p => p.id === productId);
    if (product) {
      this.itemsArray.at(index).get('unitPrice')?.setValue(product.price);
    }
  }

  submit(): void {
    if (this.form.invalid || this.saving() || this.itemsArray.length === 0) return;
    this.saving.set(true);

    const v = this.form.getRawValue();
    const rawDate = v.orderDate;
    const orderDate = rawDate instanceof Date
      ? rawDate.toISOString().split('T')[0]
      : rawDate != null ? String(rawDate) : new Date().toISOString().split('T')[0];

    this.ordersService.createOrder({
      customerId:    v.customerId!,
      orderDate,
      status:        v.status!,
      paymentMethod: v.paymentMethod!,
      shippingAmount: v.shippingAmount ?? 0,
      items: (v.items as any[]).map(i => ({
        productId: i.productId,
        quantity:  i.quantity,
        unitPrice: i.unitPrice,
      })),
    }).subscribe({
      next: order => {
        this.snackbar.open(`Order #${order.id} created`, 'Close', { duration: 3000 });
        this.router.navigate(['/orders', order.id]);
      },
      error: () => {
        this.snackbar.open('Failed to create order', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  back(): void {
    this.router.navigate(['/orders']);
  }
}
