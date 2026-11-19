# EcoNest Task 7 Reports

A Java Swing desktop application for EcoNest's monthly sales and inventory reports. It provides a monthly revenue summary and alerts for products at or below their reorder level.

## Open in Apache NetBeans

1. In NetBeans, choose **File > Open Project**.
2. Select this folder (the one containing `pom.xml`).
3. Run the project. The entry point is `com.econest.task7.App`.

The project uses Java 17 and has no third-party dependencies.

## Use

- Add or update product stock on the **Inventory** tab. A product is low-stock when its quantity on hand is less than or equal to its reorder level.
- Record completed orders on the **Sales** tab. Each order has an ISO date (`yyyy-MM-dd`) and a total amount.
- On **Monthly Reports**, enter a month as `yyyy-MM`, then select **Generate Report**. Revenue and order counts include sales dated in that month; the low-stock list reflects current inventory.
- Select **Export CSV** to save the displayed report.

The application creates `data/inventory.csv` and `data/sales.csv` in its working directory on first run. Keep the `data` folder with the project to retain the records.

## Task 7 scope

This project implements the specification's seventh core requirement: **Generate Monthly Sales & Inventory Reports (Low-stock alerts, revenue summaries)**. The inventory and sales screens provide the records needed to produce those reports. They are lightweight inputs for this standalone module, not a replacement for the full group's catalogue or order-processing modules.
