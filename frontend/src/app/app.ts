import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Order, OrderStatus } from './models/order';
import { OrderService } from './services/order.service';
import { ToastService } from './services/toast.service';
import { OrderForm } from './components/order-form/order-form';
import { OrderCard } from './components/order-card/order-card';
import { Toast } from './components/toast/toast';

type Filter = 'ALL' | OrderStatus;

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, OrderForm, OrderCard, Toast],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly orders = inject(OrderService);
  private readonly toast = inject(ToastService);

  protected readonly filter = signal<Filter>('ALL');
  protected readonly busyIds = signal<string[]>([]);
  protected readonly theme = signal<'light' | 'dark'>(this.readTheme());

  protected readonly list = computed(() => {
    const f = this.filter();
    const all = this.orders.list();
    return f === 'ALL' ? all : all.filter((o) => o.status === f);
  });

  protected readonly isLoading = this.orders.isLoading;
  protected readonly loadError = this.orders.loadError;

  protected readonly counts = computed(() => {
    const all = this.orders.list();
    const by = (s: OrderStatus) => all.filter((o) => o.status === s).length;
    return {
      ALL: all.length,
      PENDING: by('PENDING'),
      CONFIRMED: by('CONFIRMED'),
      SHIPPED: by('SHIPPED'),
      DELIVERED: by('DELIVERED'),
      CANCELLED: by('CANCELLED'),
    };
  });

  ngOnInit() {
    this.orders
      .load()
      .catch(() => this.toast.error('Could not reach the orders service'));
  }

  protected readonly filters: readonly Filter[] = ['ALL', 'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  protected setFilter(f: Filter) {
    this.filter.set(f);
  }

  protected filterLabel(f: Filter): string {
    return f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase();
  }

  protected countOf(f: Filter): number {
    return this.counts()[f];
  }

  protected retry() {
    this.orders.load().catch(() => this.toast.error('Still cannot reach the server'));
  }

  protected toggleTheme() {
    const next = this.theme() === 'light' ? 'dark' : 'light';
    this.theme.set(next);
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('hex-theme', next);
  }

  protected async runAction(action: 'confirm' | 'ship' | 'cancel', order: Order) {
    if (this.isBusy(order)) return;
    this.busyIds.update((ids) => [...ids, order.id]);
    try {
      if (action === 'confirm') await this.orders.confirm(order.id);
      if (action === 'ship') await this.orders.ship(order.id);
      if (action === 'cancel') await this.orders.cancel(order.id);
      this.toast.success(`${order.productName} ${action}ed`);
    } catch (e) {
      this.toast.error(e instanceof Error ? e.message : `Failed to ${action} order`);
    } finally {
      this.busyIds.update((ids) => ids.filter((id) => id !== order.id));
    }
  }

  protected isBusy(order: Order) {
    return this.busyIds().includes(order.id);
  }

  protected delay(i: number): string {
    return Math.min(i * 45, 320) + 'ms';
  }

  private readTheme(): 'light' | 'dark' {
    return (document.documentElement.getAttribute('data-theme') as 'light' | 'dark') || 'light';
  }
}