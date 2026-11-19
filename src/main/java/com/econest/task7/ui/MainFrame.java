package com.econest.task7.ui;

import com.econest.task7.model.ProductStock;
import com.econest.task7.model.Sale;
import com.econest.task7.service.MonthlyReport;
import com.econest.task7.storage.CsvRepository;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public final class MainFrame extends JFrame {
    private final CsvRepository repository;
    private final List<ProductStock> inventory = new ArrayList<>();
    private final List<Sale> sales = new ArrayList<>();
    private final DefaultTableModel inventoryModel = model("SKU", "Product", "Quantity", "Reorder level", "Unit price (LKR)");
    private final DefaultTableModel salesModel = model("Order ID", "Date", "Client", "Total (LKR)");
    private final JTable inventoryTable = table(inventoryModel);
    private final JTable salesTable = table(salesModel);
    private final JTextField skuField = new JTextField(9);
    private final JTextField productField = new JTextField(12);
    private final JTextField quantityField = new JTextField(5);
    private final JTextField reorderField = new JTextField(5);
    private final JTextField priceField = new JTextField(8);
    private final JTextField orderIdField = new JTextField(9);
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField clientField = new JTextField(12);
    private final JTextField totalField = new JTextField(8);
    private final JTextField monthField = new JTextField(7);
    private final JTextArea reportArea = new JTextArea();
    private MonthlyReport latestReport;

    public MainFrame(CsvRepository repository) {
        super("EcoNest | Monthly Sales & Inventory Reports");
        this.repository = repository;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 650);
        setMinimumSize(new java.awt.Dimension(820, 520));
        setLocationRelativeTo(null);
        monthField.setText(YearMonth.now().toString());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inventory", inventoryPanel());
        tabs.addTab("Sales", salesPanel());
        tabs.addTab("Monthly Reports", reportPanel());
        add(tabs, BorderLayout.CENTER);
        loadData();
    }

    private JPanel inventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.add(new JLabel("SKU")); form.add(skuField);
        form.add(new JLabel("Product")); form.add(productField);
        form.add(new JLabel("On hand")); form.add(quantityField);
        form.add(new JLabel("Reorder at")); form.add(reorderField);
        form.add(new JLabel("Unit price LKR")); form.add(priceField);
        JButton save = new JButton("Add product");
        JButton update = new JButton("Update selected");
        JButton remove = new JButton("Remove selected");
        save.addActionListener(e -> addProduct());
        update.addActionListener(e -> updateProduct());
        remove.addActionListener(e -> removeProduct());
        form.add(save); form.add(update); form.add(remove);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(form, BorderLayout.CENTER);
        bottom.add(new JLabel("Low stock means quantity on hand is at or below the reorder level."), BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);
        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillProductFields();
        });
        return panel;
    }

    private JPanel salesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(salesTable), BorderLayout.CENTER);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.add(new JLabel("Order ID")); form.add(orderIdField);
        form.add(new JLabel("Date yyyy-MM-dd")); form.add(dateField);
        form.add(new JLabel("Client")); form.add(clientField);
        form.add(new JLabel("Order total LKR")); form.add(totalField);
        JButton add = new JButton("Record sale");
        JButton remove = new JButton("Remove selected");
        add.addActionListener(e -> addSale());
        remove.addActionListener(e -> removeSale());
        form.add(add); form.add(remove);
        panel.add(form, BorderLayout.SOUTH);
        salesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillSaleFields();
        });
        return panel;
    }

    private JPanel reportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Report month (yyyy-MM)")); controls.add(monthField);
        JButton generate = new JButton("Generate report");
        JButton export = new JButton("Export CSV");
        generate.addActionListener(e -> generateReport());
        export.addActionListener(e -> exportReport());
        controls.add(generate); controls.add(export);
        panel.add(controls, BorderLayout.NORTH);
        reportArea.setEditable(false);
        reportArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 14));
        reportArea.setText("Choose a month and select Generate report.\n\n"
                + "The revenue summary uses sales recorded for that month.\n"
                + "Low-stock alerts use the current inventory quantities.");
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return panel;
    }

    private void loadData() {
        try {
            inventory.addAll(repository.loadInventory());
            sales.addAll(repository.loadSales());
            refreshTables();
        } catch (IOException | RuntimeException ex) {
            showError("Could not load saved data. Check the CSV files in the data folder.\n" + ex.getMessage());
        }
    }

    private void addProduct() {
        try {
            ProductStock p = readProduct();
            if (inventory.stream().anyMatch(existing -> existing.sku().equalsIgnoreCase(p.sku()))) {
                throw new IllegalArgumentException("That SKU already exists. Select it to update the product.");
            }
            inventory.add(p);
            persistInventory(); clearProductFields();
        } catch (RuntimeException ex) { showError(ex.getMessage()); }
    }

    private void updateProduct() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) { showError("Select a product to update."); return; }
        row = inventoryTable.convertRowIndexToModel(row);
        try {
            ProductStock updated = readProduct();
            for (int i = 0; i < inventory.size(); i++) {
                if (i != row && inventory.get(i).sku().equalsIgnoreCase(updated.sku()))
                    throw new IllegalArgumentException("Another product already uses that SKU.");
            }
            inventory.set(row, updated); persistInventory();
        } catch (RuntimeException ex) { showError(ex.getMessage()); }
    }

    private void removeProduct() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) { showError("Select a product to remove."); return; }
        row = inventoryTable.convertRowIndexToModel(row);
        inventory.remove(row); persistInventory(); clearProductFields();
    }

    private ProductStock readProduct() {
        return new ProductStock(skuField.getText(), productField.getText(),
                Integer.parseInt(quantityField.getText().trim()), Integer.parseInt(reorderField.getText().trim()),
                new BigDecimal(priceField.getText().trim()));
    }

    private void fillProductFields() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0 || row >= inventory.size()) return;
        row = inventoryTable.convertRowIndexToModel(row);
        if (row >= inventory.size()) return;
        ProductStock p = inventory.get(row);
        skuField.setText(p.sku()); productField.setText(p.name()); quantityField.setText(String.valueOf(p.quantity()));
        reorderField.setText(String.valueOf(p.reorderLevel())); priceField.setText(p.unitPrice().toPlainString());
    }

    private void clearProductFields() {
        skuField.setText(""); productField.setText(""); quantityField.setText("");
        reorderField.setText(""); priceField.setText(""); inventoryTable.clearSelection();
    }

    private void addSale() {
        try {
            Sale sale = new Sale(orderIdField.getText(), LocalDate.parse(dateField.getText().trim()),
                    clientField.getText(), new BigDecimal(totalField.getText().trim()));
            if (sales.stream().anyMatch(existing -> existing.orderId().equalsIgnoreCase(sale.orderId())))
                throw new IllegalArgumentException("That order ID already exists.");
            sales.add(sale); persistSales();
            orderIdField.setText(""); clientField.setText(""); totalField.setText("");
        } catch (DateTimeParseException ex) { showError("Enter the sale date as yyyy-MM-dd."); }
        catch (RuntimeException ex) { showError(ex.getMessage()); }
    }

    private void removeSale() {
        int row = salesTable.getSelectedRow();
        if (row < 0) { showError("Select a sale to remove."); return; }
        row = salesTable.convertRowIndexToModel(row);
        sales.remove(row); persistSales();
    }

    private void fillSaleFields() {
        int row = salesTable.getSelectedRow();
        if (row < 0 || row >= sales.size()) return;
        row = salesTable.convertRowIndexToModel(row);
        if (row >= sales.size()) return;
        Sale s = sales.get(row);
        orderIdField.setText(s.orderId()); dateField.setText(s.date().toString());
        clientField.setText(s.clientName()); totalField.setText(s.total().toPlainString());
    }

    private void generateReport() {
        try {
            YearMonth month = YearMonth.parse(monthField.getText().trim());
            latestReport = MonthlyReport.create(month, sales, inventory);
            StringBuilder text = new StringBuilder();
            text.append("ECONEST MONTHLY SALES AND INVENTORY REPORT\n");
            text.append("Report month: ").append(month).append("\n\n");
            text.append("SALES SUMMARY\n");
            text.append("Completed orders: ").append(latestReport.orderCount()).append('\n');
            text.append("Total revenue:     LKR ").append(money(latestReport.revenue())).append('\n');
            text.append("Average per order: LKR ").append(money(latestReport.averageOrderValue())).append("\n\n");
            text.append("LOW-STOCK ALERTS (current inventory)\n");
            if (latestReport.lowStockProducts().isEmpty()) {
                text.append("No products are at or below their reorder level.\n");
            } else {
                text.append(String.format("%-14s %-28s %10s %14s%n", "SKU", "Product", "On hand", "Reorder at"));
                text.append("-".repeat(72)).append('\n');
                for (ProductStock p : latestReport.lowStockProducts()) {
                    text.append(String.format("%-14s %-28s %10d %14d%n", p.sku(), truncate(p.name(), 28), p.quantity(), p.reorderLevel()));
                }
            }
            reportArea.setText(text.toString()); reportArea.setCaretPosition(0);
        } catch (DateTimeParseException ex) {
            showError("Enter a report month in yyyy-MM format, for example 2026-11.");
        }
    }

    private void exportReport() {
        if (latestReport == null) { showError("Generate a report before exporting it."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("econest-report-" + latestReport.month() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            List<String> lines = new ArrayList<>();
            lines.add("section,month,metric,value");
            lines.add("Sales," + latestReport.month() + ",Completed orders," + latestReport.orderCount());
            lines.add("Sales," + latestReport.month() + ",Total revenue LKR," + latestReport.revenue().toPlainString());
            lines.add("Sales," + latestReport.month() + ",Average order value LKR," + latestReport.averageOrderValue().toPlainString());
            for (ProductStock p : latestReport.lowStockProducts()) {
                lines.add("Inventory," + latestReport.month() + "," + csv("Low stock SKU " + p.sku()) + ","
                        + csv(p.name() + " | quantity=" + p.quantity() + " | reorder=" + p.reorderLevel()));
            }
            if (latestReport.lowStockProducts().isEmpty()) lines.add("Inventory," + latestReport.month() + ",Low-stock products,0");
            Files.write(chooser.getSelectedFile().toPath(), lines, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Report exported successfully.", "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) { showError("Could not export the report: " + ex.getMessage()); }
    }

    private void persistInventory() {
        try { repository.saveInventory(inventory); refreshInventoryTable(); }
        catch (IOException ex) { showError("Could not save inventory: " + ex.getMessage()); }
    }

    private void persistSales() {
        try { repository.saveSales(sales); refreshSalesTable(); }
        catch (IOException ex) { showError("Could not save sales: " + ex.getMessage()); }
    }

    private void refreshTables() { refreshInventoryTable(); refreshSalesTable(); }

    private void refreshInventoryTable() {
        inventoryModel.setRowCount(0);
        for (ProductStock p : inventory) inventoryModel.addRow(new Object[]{p.sku(), p.name(), p.quantity(), p.reorderLevel(), p.unitPrice()});
    }

    private void refreshSalesTable() {
        salesModel.setRowCount(0);
        for (Sale s : sales) salesModel.addRow(new Object[]{s.orderId(), s.date(), s.clientName(), s.total()});
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        return table;
    }

    private static String money(BigDecimal amount) { return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
    private static String truncate(String text, int max) { return text.length() <= max ? text : text.substring(0, max - 3) + "..."; }
    private static String csv(String text) { return "\"" + text.replace("\"", "\"\"") + "\""; }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Please check the entered values." : message,
                "EcoNest", JOptionPane.ERROR_MESSAGE);
    }
}
