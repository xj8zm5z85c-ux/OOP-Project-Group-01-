package com.econest.task7.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Sale(String orderId, LocalDate date, String clientName, BigDecimal total) {
    public Sale {
        orderId = Objects.requireNonNull(orderId, "Order ID").trim();
        date = Objects.requireNonNull(date, "Sale date");
        clientName = Objects.requireNonNull(clientName, "Client name").trim();
        total = Objects.requireNonNull(total, "Order total");
        if (orderId.isEmpty() || clientName.isEmpty()) throw new IllegalArgumentException("Order ID and client name are required.");
        if (total.signum() < 0) throw new IllegalArgumentException("Order total cannot be negative.");
    }
}
