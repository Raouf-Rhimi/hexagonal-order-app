package com.example.hex.domain.service;

import com.example.hex.domain.model.Order;
import com.example.hex.domain.port.OrderRepository;
import com.example.hex.domain.port.OrderService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain Service: implements the OrderService port.
 * Contains business logic. Depends on the OrderRepository PORT (interface), not the implementation.
 * NOTE: pure Java — no Spring, no JPA, no HTTP.
 */
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order createOrder(String productName, int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        Order order = new Order(productName, quantity, unitPrice);
        return orderRepository.save(order);
    }

    @Override
    public Order getOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order confirmOrder(String id) {
        Order order = getOrder(id);
        order.confirm();
        return orderRepository.save(order);
    }

    @Override
    public Order shipOrder(String id) {
        Order order = getOrder(id);
        order.ship();
        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(String id) {
        Order order = getOrder(id);
        order.cancel();
        return orderRepository.save(order);
    }
}