# EcoNest Task 6 Delivery Management

A Java Swing desktop application for assigning delivery agents to client orders, scheduling deliveries, and tracking delivery status. This is a Maven project that Apache NetBeans can open directly.

## Open and run in Apache NetBeans

1. Install a JDK 17 or newer and Apache NetBeans with Maven support.
2. In NetBeans, choose **File → Open Project** and select this `EcoNestTask6` folder.
3. Allow Maven to resolve the Jakarta Mail dependency.
4. Run `EcoNestTask6App.java` or right-click the project and choose **Run**.

The application starts with sample orders and delivery agents so the assignment and status workflow can be explored. Changes are stored in the user's `.econest/task6-data.ser` file and remain available on the next launch.

## Email notification setup

When a delivery is assigned, the application sends an email to the assigned agent if SMTP settings are configured. Set these environment variables before launching NetBeans:

- `ECONEST_SMTP_HOST` (for example, your mail provider's SMTP host)
- `ECONEST_SMTP_PORT` (commonly `587` for STARTTLS)
- `ECONEST_SMTP_USERNAME`
- `ECONEST_SMTP_PASSWORD`
- `ECONEST_SMTP_FROM` (sender email address)

Without these settings, assignments are still saved and the application reports that email was not sent. Use an app password or a dedicated project account rather than a personal account password.

## Task 6 scope

- Assign a delivery agent to an order and choose a scheduled date.
- Email the agent when a new assignment is made, when SMTP is configured.
- Update and track delivery status for each order.

Sample records are for demonstration. The project does not include the other project tasks such as catalogue, client, inventory, sales reporting, or client dispatch emails.
