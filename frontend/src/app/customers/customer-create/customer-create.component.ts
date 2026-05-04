import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { CustomersService } from '../services/customers.service';

@Component({
  selector: 'app-customer-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './customer-create.component.html',
  styleUrls: ['./customer-create.component.scss'],
})
export class CustomerCreateComponent {
  private readonly service  = inject(CustomersService);
  private readonly router   = inject(Router);
  private readonly fb       = inject(FormBuilder);
  private readonly snackbar = inject(MatSnackBar);

  saving = signal(false);

  form = this.fb.group({
    name:  ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    city:  ['', [Validators.required, Validators.minLength(2)]],
  });

  submit(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);

    const { name, email, city } = this.form.getRawValue();
    this.service.createCustomer({ name: name!, email: email!, city: city! }).subscribe({
      next: c => {
        this.snackbar.open(`Customer "${c.name}" created`, 'Close', { duration: 3000 });
        this.router.navigate(['/customers', c.id]);
      },
      error: () => {
        this.snackbar.open('Failed to create customer', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  back(): void {
    this.router.navigate(['/customers']);
  }
}
