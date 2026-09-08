import { Component, inject, input, output } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Order, OrderStatus } from '../../models/order';

const AVATAR_GRADIENTS = [
  'linear-gradient(135deg,#6c4cf1,#a78bfa)',
  'linear-gradient(135deg,#00b8a9,#2dd4bf)',
  'linear-gradient(135deg,#f5a623,#fbbf24)',
  'linear-gradient(135deg,#f6464c,#fb7185)',
  'linear-gradient(135deg,#0ea5e9,#38bdf8)',
];

function hash(str: string): number {
  let h = 0;
  for (let i = 0; i < str.length; i++) h = (h << 5) - h + str.charCodeAt(i);
  return Math.abs(h);
}

@Component({
  selector: 'app-order-card',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './order-card.html',
  styleUrl: './order-card.css',
})
export class OrderCard {
  readonly order = input.required<Order>();
  readonly busy = input(false);

  readonly confirm = output<Order>();
  readonly ship = output<Order>();
  readonly cancel = output<Order>();

  protected avatarGradient(order: Order): string {
    return AVATAR_GRADIENTS[hash(order.productName) % AVATAR_GRADIENTS.length];
  }

  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0]!.toUpperCase())
      .join('');
  }

  protected canConfirm(status: OrderStatus) {
    return status === 'PENDING';
  }
  protected canShip(status: OrderStatus) {
    return status === 'CONFIRMED';
  }
  protected canCancel(status: OrderStatus) {
    return status === 'PENDING' || status === 'CONFIRMED';
  }
}