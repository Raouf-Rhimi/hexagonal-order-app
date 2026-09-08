package com.example.hex.domain.service;

import com.example.hex.domain.model.Order;
import com.example.hex.domain.port.InMemoryOrderRepository;
import com.example.hex.domain.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the DOMAIN only.
 * No Spring context, no MySQL, no HTTP — just the port + an in-memory fake adapter.
 * This is the core selling point of hexagonal architecture.
 */
class OrderServiceImplTest {

    private OrderRepository repository;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        orderService = new OrderServiceImpl(repository);
    }

    @Test
    void shouldCreateOrderAsPending() {
        Order order = orderService.createOrder("Laptop", 2, new BigDecimal("1500.00"));

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("3000.00");
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> orderService.createOrder("Laptop", -1, new BigDecimal("10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity");
    }

    @Test
    void shouldRejectZeroOrNegativePrice() {
        assertThatThrownBy(() -> orderService.createOrder("Laptop", 1, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price");
    }

    @Test
    void shouldConfirmOrder() {
        Order order = orderService.createOrder("Laptop", 1, new BigDecimal("1500.00"));

        Order confirmed = orderService.confirmOrder(order.getId());

        assertThat(confirmed.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    void couldNotShipUnconfirmedOrder() {
        Order order = orderService.createOrder("Laptop", 1, new BigDecimal("1500.00"));

        assertThatThrownBy(() -> orderService.shipOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("confirmed");
    }

    @Test
    void shouldNotCancelShippedOrder() {
        Order order = orderService.createOrder("Laptop", 1, new BigDecimal("1500.00"));
        orderService.confirmOrder(order.getId());
        orderService.shipOrder(order.getId());

        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shipped or delivered");
    }

    @Test
    void shouldGetAllOrders() {
        orderService.createOrder("Laptop", 1, new BigDecimal("1500.00"));
        orderService.createOrder("Mouse", 3, new BigDecimal("50.00"));

        List<Order> orders = orderService.getAllOrders();

        assertThat(orders).hasSize(2);
    }
}