package com.example.hex.adapter.out.mysql;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository.
 * Primitive persistence — no business logic here. The adapter on top of it does the translating.
 */
public interface SpringDataOrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {
}