package com.example.hex.adapter.out.mysql;

import com.example.hex.domain.model.Order;
import com.example.hex.domain.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Secondary Adapter (Output).
 * Implements the OrderRepository PORT using JPA/MySQL.
 * The domain only knows the port interface — this class is swappable
 * (try swapping to MongoDB, files, or in-memory — the domain never changes).
 */
@Repository
public class OrderRepositoryJpaAdapter implements OrderRepository {

    private final SpringDataOrderJpaRepository springDataRepository;

    public OrderRepositoryJpaAdapter(SpringDataOrderJpaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = toEntity(order);
        OrderJpaEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(String id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Order> findAll() {
        return springDataRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        springDataRepository.deleteById(id);
    }

    // --- Mapping between DB entity and Domain model ---

    private OrderJpaEntity toEntity(Order order) {
        return new OrderJpaEntity(
                order.getId(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }

    private Order toDomain(OrderJpaEntity entity) {
        Order order = new Order();
        order.setId(entity.getId());
        order.setProductName(entity.getProductName());
        order.setQuantity(entity.getQuantity());
        order.setUnitPrice(entity.getUnitPrice());
        order.setStatus(Order.OrderStatus.valueOf(entity.getStatus()));
        order.setCreatedAt(entity.getCreatedAt());
        return order;
    }
}