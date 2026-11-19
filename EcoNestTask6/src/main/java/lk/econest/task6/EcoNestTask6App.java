package lk.econest.task6;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EcoNestTask6App extends JFrame {
    private final AppData data = DataStore.load();
    private final OrderTableModel tableModel = new OrderTableModel(data.orders, data.agents);
    private final JTable ordersTable = new JTable(tableModel);

    public EcoNestTask6App() {
        super("EcoNest | Delivery Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 540);
        setLocationRelativeTo(null);
        buildUi();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 14, 18));
        root.setBackground(new Color(247, 249, 245));

        JLabel heading = new JLabel("Delivery Management");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 23f));
        heading.setForeground(new Color(37, 82, 58));
        JLabel subtitle = new JLabel("Assign agents, schedule deliveries, and keep order status up to date.");
        JPanel titlePanel = new JPanel(new BorderLayout(0, 5));
        titlePanel.setOpaque(false);
        titlePanel.add(heading, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.SOUTH);
        root.add(titlePanel, BorderLayout.NORTH);

        ordersTable.setRowHeight(30);
        ordersTable.setFillsViewportHeight(true);
        ordersTable.setAutoCreateRowSorter(true);
        root.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        JButton assignButton = new JButton("Assign Agent / Schedule");
        JButton statusButton = new JButton("Update Delivery Status");
        JButton refreshButton = new JButton("Refresh");
        assignButton.addActionListener(event -> assignSelectedOrder());
        statusButton.addActionListener(event -> updateSelectedStatus());
        refreshButton.addActionListener(event -> tableModel.fireTableDataChanged());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(assignButton);
        actions.add(statusButton);
        actions.add(refreshButton);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private Order selectedOrder() {
        int viewRow = ordersTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first.", "No order selected", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return data.orders.get(ordersTable.convertRowIndexToModel(viewRow));
    }

    private void assignSelectedOrder() {
        Order order = selectedOrder();
        if (order == null) return;
        if (data.agents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one delivery agent before assigning an order.", "No agents available", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<DeliveryAgent> agentPicker = new JComboBox<>(data.agents.toArray(DeliveryAgent[]::new));
        JTextField dateField = new JTextField(LocalDate.now().plusDays(1).toString(), 12);
        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Delivery agent:"));
        form.add(agentPicker);
        form.add(new JLabel("Scheduled date (YYYY-MM-DD):"));
        form.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, form, "Assign order " + order.orderId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        LocalDate scheduledDate;
        try {
            scheduledDate = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Enter the date as YYYY-MM-DD.", "Invalid date", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (scheduledDate.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "The scheduled date cannot be in the past.", "Invalid date", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DeliveryAgent agent = (DeliveryAgent) agentPicker.getSelectedItem();
        order.agentId = agent.agentId;
        order.scheduledDate = scheduledDate.toString();
        order.deliveryStatus = DeliveryStatus.SCHEDULED;
        DataStore.save(data);
        tableModel.fireTableDataChanged();
        sendAssignmentEmail(order, agent);
    }

    private void updateSelectedStatus() {
        Order order = selectedOrder();
        if (order == null) return;
        if (order.agentId == null) {
            JOptionPane.showMessageDialog(this, "Assign an agent before updating delivery status.", "Order is unassigned", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DeliveryStatus[] options = DeliveryStatus.values();
        JComboBox<DeliveryStatus> statusPicker = new JComboBox<>(options);
        statusPicker.setSelectedItem(order.deliveryStatus);
        int result = JOptionPane.showConfirmDialog(this, statusPicker, "Update status for order " + order.orderId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            order.deliveryStatus = (DeliveryStatus) statusPicker.getSelectedItem();
            DataStore.save(data);
            tableModel.fireTableDataChanged();
        }
    }

    private void sendAssignmentEmail(Order order, DeliveryAgent agent) {
        MailSettings settings = MailSettings.fromEnvironment();
        if (!settings.isConfigured()) {
            JOptionPane.showMessageDialog(this,
                    "The delivery assignment was saved. Email was not sent because SMTP settings are missing. See README.md to configure email.",
                    "Assignment saved", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new SwingWorker<Boolean, Void>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    EmailNotifier.sendAssignment(settings, agent, order);
                    return true;
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(EcoNestTask6App.this,
                                "Order " + order.orderId + " was assigned and an email was sent to " + agent.name + ".",
                                "Assignment complete", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(EcoNestTask6App.this,
                                "The assignment was saved, but the email could not be sent. Check the SMTP settings.\n" + errorMessage,
                                "Email could not be sent", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EcoNestTask6App.this, "Assignment saved, but email status could not be confirmed.",
                            "Email status", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use Swing's default look and feel if the system theme is unavailable.
            }
            new EcoNestTask6App().setVisible(true);
        });
    }

    private static final class OrderTableModel extends AbstractTableModel {
        private final String[] columns = {"Order ID", "Client", "Order Details", "Delivery Agent", "Scheduled Date", "Delivery Status"};
        private final List<Order> orders;
        private final List<DeliveryAgent> agents;

        private OrderTableModel(List<Order> orders, List<DeliveryAgent> agents) {
            this.orders = orders;
            this.agents = agents;
        }

        @Override public int getRowCount() { return orders.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Order order = orders.get(rowIndex);
            DeliveryAgent agent = findAgent(agents, order.agentId);
            return switch (columnIndex) {
                case 0 -> order.orderId;
                case 1 -> order.clientName;
                case 2 -> order.orderDetails;
                case 3 -> agent == null ? "Unassigned" : agent.name;
                case 4 -> order.scheduledDate == null ? "—" : order.scheduledDate;
                case 5 -> order.deliveryStatus;
                default -> "";
            };
        }
    }

    private enum DeliveryStatus { PENDING, SCHEDULED, IN_TRANSIT, DELIVERED, FAILED }

    private static final class DeliveryAgent implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int agentId;
        private final String name;
        private final String email;
        private final String phone;
        private final String vehicleDetails;

        private DeliveryAgent(int agentId, String name, String email, String phone, String vehicleDetails) {
            this.agentId = agentId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.vehicleDetails = vehicleDetails;
        }

        @Override public String toString() { return name + " — " + vehicleDetails; }
    }

    private static final class Order implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int orderId;
        private final String clientName;
        private final String clientEmail;
        private final String orderDetails;
        private Integer agentId;
        private String scheduledDate;
        private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

        private Order(int orderId, String clientName, String clientEmail, String orderDetails) {
            this.orderId = orderId;
            this.clientName = clientName;
            this.clientEmail = clientEmail;
            this.orderDetails = orderDetails;
        }
    }

    private static final class AppData implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Order> orders = new ArrayList<>();
        private List<DeliveryAgent> agents = new ArrayList<>();

        private static AppData sample() {
            AppData sample = new AppData();
            sample.agents.add(new DeliveryAgent(1, "Nimal Perera", "nimal@example.com", "077-123-4567", "Van WP CAB-1234"));
            sample.agents.add(new DeliveryAgent(2, "Ayesha Silva", "ayesha@example.com", "071-234-5678", "Bike WP BAA-5678"));
            sample.agents.add(new DeliveryAgent(3, "Kasun Fernando", "kasun@example.com", "076-345-6789", "Van WP CAD-9012"));
            sample.orders.add(new Order(1001, "Green Office Ltd", "office@example.com", "Indoor plants and planters"));
            sample.orders.add(new Order(1002, "M. Jayasinghe", "customer@example.com", "Balcony gardening kit"));
            sample.orders.add(new Order(1003, "Lakeview Apartments", "manager@example.com", "12 indoor plants"));
            return sample;
        }
    }

    private static final class DataStore {
        private static final Path FILE = Path.of(System.getProperty("user.home"), ".econest", "task6-data.ser");

        private static AppData load() {
            if (Files.exists(FILE)) {
                try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(FILE))) {
                    Object value = input.readObject();
                    if (value instanceof AppData appData) return appData;
                } catch (Exception ex) {
                    System.err.println("Could not load saved EcoNest data; using sample records: " + ex.getMessage());
                }
            }
            AppData sample = AppData.sample();
            save(sample);
            return sample;
        }

        private static void save(AppData data) {
            try {
                Files.createDirectories(FILE.getParent());
                try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(FILE))) {
                    output.writeObject(data);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Could not save changes: " + ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private record MailSettings(String host, String port, String username, String password, String from) {
        private static MailSettings fromEnvironment() {
            return new MailSettings(System.getenv("ECONEST_SMTP_HOST"), System.getenv("ECONEST_SMTP_PORT"),
                    System.getenv("ECONEST_SMTP_USERNAME"), System.getenv("ECONEST_SMTP_PASSWORD"),
                    System.getenv("ECONEST_SMTP_FROM"));
        }

        private boolean isConfigured() {
            return hasText(host) && hasText(port) && hasText(username) && hasText(password) && hasText(from);
        }

        private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    }

    private static final class EmailNotifier {
        private static void sendAssignment(MailSettings settings, DeliveryAgent agent, Order order) throws Exception {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", settings.host());
            properties.put("mail.smtp.port", settings.port());
            properties.put("mail.smtp.connectiontimeout", "10000");
            properties.put("mail.smtp.timeout", "10000");
            properties.put("mail.smtp.writetimeout", "10000");
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(settings.username(), settings.password());
                }
            });
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settings.from()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(agent.email));
            message.setSubject("EcoNest delivery assigned - Order " + order.orderId);
            message.setText("Hello " + agent.name + ",\n\nA new EcoNest delivery has been assigned to you.\n"
                    + "Order: " + order.orderId + "\nClient: " + order.clientName + "\n"
                    + "Delivery: " + order.orderDetails + "\nScheduled date: " + order.scheduledDate + "\n\nEcoNest");
            Transport.send(message);
        }
    }

    private static DeliveryAgent findAgent(List<DeliveryAgent> agents, Integer agentId) {
        if (agentId == null) return null;
        return agents.stream().filter(agent -> agent.agentId == agentId).findFirst().orElse(null);
    }
}
