package com.example.hex.domain.port;

import com.example.hex.domain.model.Order;
import java.math.BigDecimal;
import java.util.List;

/**
 * Primary Port: the app EXPOSES this to the outside world.
 * Adapters (REST, CLI, gRPC) call these methods.
 */
public interface OrderService {

    Order createOrder(String productName, int quantity, BigDecimal unitPrice);

    Order getOrder(String id);

    List<Order> getAllOrders();

    Order confirmOrder(String id);

    Order shipOrder(String id);

    Order cancelOrder(String id);
}