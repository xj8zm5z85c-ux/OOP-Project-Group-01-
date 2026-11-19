package com.econest.task7.service;

import com.econest.task7.model.ProductStock;
import com.econest.task7.model.Sale;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlyReport(YearMonth month, BigDecimal revenue, int orderCount,
                            BigDecimal averageOrderValue, List<ProductStock> lowStockProducts) {
    public MonthlyReport {
        lowStockProducts = List.copyOf(lowStockProducts);
    }

    public static MonthlyReport create(YearMonth month, List<Sale> sales, List<ProductStock> inventory) {
        List<Sale> monthSales = sales.stream().filter(s -> YearMonth.from(s.date()).equals(month)).toList();
        BigDecimal revenue = monthSales.stream().map(Sale::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = monthSales.isEmpty() ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(monthSales.size()), 2, java.math.RoundingMode.HALF_UP);
        List<ProductStock> lowStock = inventory.stream().filter(ProductStock::isLowStock)
                .sorted(java.util.Comparator.comparing(ProductStock::sku)).toList();
        return new MonthlyReport(month, revenue, monthSales.size(), average, lowStock);
    }
}
