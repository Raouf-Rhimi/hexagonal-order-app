package com.example.hex.config;

import com.example.hex.domain.port.OrderRepository;
import com.example.hex.domain.port.OrderService;
import com.example.hex.domain.service.OrderServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root: the ONLY place that wires things together.
 * The core library is pure Java — it has no @Service annotations.
 * Here we tell Spring "build a service, and give it that repository adapter".
 * Swap the repository adapter bean and the domain works with a different DB.
 */
@Configuration
public class AppConfig {

    @Bean
    public OrderService orderService(OrderRepository orderRepository) {
        return new OrderServiceImpl(orderRepository);
    }
}