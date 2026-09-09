import { computed, Injectable, signal } from '@angular/core';
import { CreateOrderPayload, Order, OrderStatus, STATUS_ORDER } from '../models/order';

const API_BASE = '/api/orders';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly orders = signal<Order[]>([]);
  private readonly loading = signal(false);
  private readonly error = signal<string | null>(null);

  /** Sorted newest first */
  readonly list = computed(() =>
    [...this.orders()].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    ),
  );
  readonly isLoading = this.loading.asReadonly();
  readonly loadError = this.error.asReadonly();

  readonly totalOrders = computed(() => this.orders().length);
  readonly totalValue = computed(() =>
    this.orders()
      .filter((o) => o.status !== 'CANCELLED')
      .reduce((sum, o) => sum + o.totalPrice, 0),
  );
  readonly pendingCount = computed(() => this.count('PENDING'));
  readonly confirmedCount = computed(() => this.count('CONFIRMED'));
  readonly shippedCount = computed(() => this.count('SHIPPED'));
  readonly cancelledCount = computed(() => this.count('CANCELLED'));

  private count(status: OrderStatus) {
    return this.orders().filter((o) => o.status === status).length;
  }

  statusCount(status: OrderStatus) {
    return this.orders()
      .filter((o) => o.status === status)
      .length;
  }

  /** A stable ordering used by the dashboard tabs */
  readonly statusOrder = STATUS_ORDER;

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await fetch(API_BASE);
      if (!res.ok) throw new Error(`Request failed (${res.status})`);
      this.orders.set((await res.json()) as Order[]);
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'Could not reach the server');
    } finally {
      this.loading.set(false);
    }
  }

  async create(payload: CreateOrderPayload): Promise<Order> {
    const body = JSON.stringify({
      productName: payload.productName.trim(),
      quantity: payload.quantity,
      unitPrice: payload.unitPrice,
    });
    const res = await fetch(API_BASE, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
    });
    return this.handle(res);
  }

  async confirm(id: string): Promise<Order> {
    return this.action(id, 'confirm');
  }

  async ship(id: string): Promise<Order> {
    return this.action(id, 'ship');
  }

  async cancel(id: string): Promise<Order> {
    return this.action(id, 'cancel');
  }

  private async action(id: string, action: 'confirm' | 'ship' | 'cancel'): Promise<Order> {
    const res = await fetch(`${API_BASE}/${id}/${action}`, { method: 'POST' });
    return this.handle(res);
  }

  private async handle(res: Response): Promise<Order> {
    if (!res.ok) {
      let message = `Request failed (${res.status}). Please try again.`;
      try {
        const text = await res.text();
        if (text && text.trim().length > 0) message = text.trim();
      } catch {
        /* ignore */
      }
      throw new Error(message);
    }
    const order = (await res.json()) as Order;
    this.orders.update((prev) => [...prev.filter((o) => o.id !== order.id), order]);
    return order;
  }
}