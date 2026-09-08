import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './order-form.html',
  styleUrl: './order-form.css',
})
export class OrderForm {
  private readonly orders = inject(OrderService);
  private readonly toast = inject(ToastService);

  protected readonly submitting = this.orders.isLoading;

  protected readonly form = new FormGroup({
    productName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2)],
    }),
    quantity: new FormControl<number>(1, {
      validators: [Validators.required, Validators.min(1)],
      nonNullable: true,
    }),
    unitPrice: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
  });

  protected get titleTooShort() {
    return this.form.controls.productName.touched && this.form.controls.productName.hasError('minlength');
  }
  protected get titleRequired() {
    return this.form.controls.productName.touched && this.form.controls.productName.hasError('required');
  }
  protected get quantityInvalid() {
    return this.form.controls.quantity.touched && this.form.controls.quantity.hasError('min');
  }
  protected get priceInvalid() {
    return this.form.controls.unitPrice.touched &&
      (this.form.controls.unitPrice.hasError('required') || this.form.controls.unitPrice.hasError('min'));
  }

  protected estimatedTotal(): number {
    const q = this.form.controls.quantity.value || 0;
    const p = this.form.controls.unitPrice.value || 0;
    return q * p;
  }

  protected async submit(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const { productName, quantity, unitPrice } = this.form.getRawValue();
    try {
      const order = await this.orders.create({ productName, quantity, unitPrice: unitPrice! });
      this.toast.success(`${order.productName} — order placed`);
      this.form.reset({ productName: '', quantity: 1, unitPrice: null });
      this.form.controls.unitPrice.enable();
    } catch (e) {
      this.toast.error(e instanceof Error ? e.message : 'Failed to create order');
    }
  }
}