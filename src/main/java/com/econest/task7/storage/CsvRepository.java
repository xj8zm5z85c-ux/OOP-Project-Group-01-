package com.econest.task7.storage;

import com.econest.task7.model.ProductStock;
import com.econest.task7.model.Sale;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CsvRepository {
    private final Path dataDirectory = Path.of(System.getProperty("user.dir"), "data");
    private final Path inventoryFile = dataDirectory.resolve("inventory.csv");
    private final Path salesFile = dataDirectory.resolve("sales.csv");

    public CsvRepository() {
        try {
            Files.createDirectories(dataDirectory);
            if (Files.notExists(inventoryFile)) Files.writeString(inventoryFile, "sku,name,quantity,reorderLevel,unitPrice\n", StandardCharsets.UTF_8);
            if (Files.notExists(salesFile)) Files.writeString(salesFile, "orderId,date,clientName,total\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize the data folder: " + e.getMessage(), e);
        }
    }

    public List<ProductStock> loadInventory() throws IOException {
        List<ProductStock> records = new ArrayList<>();
        List<String> lines = Files.readAllLines(inventoryFile, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            try {
                List<String> c = parseLine(lines.get(i));
                records.add(new ProductStock(c.get(0), c.get(1), Integer.parseInt(c.get(2)),
                        Integer.parseInt(c.get(3)), new BigDecimal(c.get(4))));
            } catch (RuntimeException e) {
                throw new IOException("Invalid inventory.csv row " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return records;
    }

    public List<Sale> loadSales() throws IOException {
        List<Sale> records = new ArrayList<>();
        List<String> lines = Files.readAllLines(salesFile, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            try {
                List<String> c = parseLine(lines.get(i));
                records.add(new Sale(c.get(0), LocalDate.parse(c.get(1)), c.get(2), new BigDecimal(c.get(3))));
            } catch (RuntimeException e) {
                throw new IOException("Invalid sales.csv row " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return records;
    }

    public void saveInventory(List<ProductStock> products) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("sku,name,quantity,reorderLevel,unitPrice");
        for (ProductStock p : products) lines.add(csv(p.sku()) + "," + csv(p.name()) + "," + p.quantity()
                + "," + p.reorderLevel() + "," + p.unitPrice().toPlainString());
        Files.write(inventoryFile, lines, StandardCharsets.UTF_8);
    }

    public void saveSales(List<Sale> sales) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("orderId,date,clientName,total");
        for (Sale s : sales) lines.add(csv(s.orderId()) + "," + s.date() + "," + csv(s.clientName())
                + "," + s.total().toPlainString());
        Files.write(salesFile, lines, StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted && ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                field.append('"'); i++;
            } else if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                fields.add(field.toString()); field.setLength(0);
            } else {
                field.append(ch);
            }
        }
        if (quoted) throw new IllegalArgumentException("Unclosed quoted field.");
        fields.add(field.toString());
        return fields;
    }
}
