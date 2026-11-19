package com.econest.task7.model;

import java.math.BigDecimal;
import java.util.Objects;

public record ProductStock(String sku, String name, int quantity, int reorderLevel,
                           BigDecimal unitPrice) {
    public ProductStock {
        sku = Objects.requireNonNull(sku, "SKU").trim();
        name = Objects.requireNonNull(name, "Product name").trim();
        unitPrice = Objects.requireNonNull(unitPrice, "Unit price");
        if (sku.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("SKU and product name are required.");
        if (quantity < 0 || reorderLevel < 0) throw new IllegalArgumentException("Quantity and reorder level cannot be negative.");
        if (unitPrice.signum() < 0) throw new IllegalArgumentException("Unit price cannot be negative.");
    }

    public boolean isLowStock() { return quantity <= reorderLevel; }
}
