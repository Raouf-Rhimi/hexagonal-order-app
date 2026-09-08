import { Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  templateUrl: './toast.html',
  styleUrl: './toast.css',
})
export class Toast {
  protected readonly service = inject(ToastService);
  protected readonly toasts = this.service.toasts;

  dismiss(id: number) {
    this.service.dismiss(id);
  }
}