package com.example.hex.adapter.in.rest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        String id,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt) {
}