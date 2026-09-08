package com.example.hex.domain.port;

import com.example.hex.domain.model.Order;
import java.util.List;
import java.util.Optional;

/**
 * Secondary Port: the domain NEEDS this, but doesn't know HOW it's implemented.
 * Could be MySQL, PostgreSQL, MongoDB, file system — the domain doesn't care.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findAll();

    void deleteById(String id);
}