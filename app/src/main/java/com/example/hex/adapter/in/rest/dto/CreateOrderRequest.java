package com.example.hex.adapter.in.rest;

import java.math.BigDecimal;

public record CreateOrderRequest(String productName, int quantity, BigDecimal unitPrice) {
}