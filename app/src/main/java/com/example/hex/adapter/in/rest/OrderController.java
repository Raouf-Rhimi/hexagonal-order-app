package com.example.hex.adapter.in.rest;

import com.example.hex.domain.model.Order;
import com.example.hex.domain.port.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Primary Adapter (Input side).
 * Translates HTTP requests into OrderService PORT calls.
 * It knows NOTHING about MySQL, JPA, or how data is stored — it only talks to the port.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        Order created = orderService.createOrder(
                request.productName(), request.quantity(), request.unitPrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable String id) {
        return toResponse(orderService.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirmOrder(@PathVariable String id) {
        return toResponse(orderService.confirmOrder(id));
    }

    @PostMapping("/{id}/ship")
    public OrderResponse shipOrder(@PathVariable String id) {
        return toResponse(orderService.shipOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable String id) {
        return toResponse(orderService.cancelOrder(id));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}