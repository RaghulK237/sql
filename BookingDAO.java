// ============================================================
// BookingDAO.java
// All database operations for Bookings, Tickets, and Payments.
// Replaces the old RegistrationDAO.java completely.
//
// Methods:
//   bookEvent()            -- calls the BookEvent stored procedure
//   viewBookingsForEvent() -- admin view of all bookings for one event
//   viewAllBookings()      -- admin view of ALL bookings
//   viewMyBookings()       -- customer's own bookings
//   viewMyTickets()        -- customer's individual ticket codes
//   cancelBooking()        -- customer cancels their own booking
//   viewAllPayments()      -- admin payment ledger
//   viewRevenueReport()    -- COUNT, SUM, AVG, GROUP BY, HAVING
//   viewRegistrationStats()-- COUNT, GROUP BY for college report
//   viewAllCustomers()     -- admin list of all customers
// ============================================================

import java.sql.*;

public class BookingDAO {

    // ----------------------------------------------------------
    // 1. BOOK EVENT
    // Calls the stored procedure BookEvent(userId, eventId, qty).
    // The procedure handles the transaction (Booking + Tickets + Payment).
    // Returns true if booking succeeded.
    // ----------------------------------------------------------
    public boolean bookEvent(int userId, int eventId, int quantity) {
        String sql = "CALL BookEvent(?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            ps.setInt(3, quantity);

            // executeQuery() retrieves the SELECT result returned by the procedure
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int    bookingId     = rs.getInt   ("booking_id");
                    int    ticketCount   = rs.getInt   ("ticket_count");
                    double totalAmount   = rs.getDouble("total_amount");
                    String payStatus     = rs.getString("payment_status");

                    System.out.println("\n  +------------------------------------------+");
                    System.out.println("  |         BOOKING CONFIRMED!               |");
                    System.out.println("  +------------------------------------------+");
                    System.out.printf ("  Booking ID    : %d%n", bookingId);
                    System.out.printf ("  Tickets       : %d%n", ticketCount);
                    if (totalAmount == 0) {
                        System.out.println("  Amount        : FREE");
                    } else {
                        System.out.printf("  Amount Paid   : Rs. %.2f%n", totalAmount);
                    }
                    System.out.printf ("  Payment Status: %s%n", payStatus.toUpperCase());
                    System.out.println("\n  Your ticket codes:");

                    // Fetch and display the ticket codes just created
                    viewTicketsForBooking(bookingId);
                    return true;
                }
            }

        } catch (SQLException e) {
            // The procedure SIGNALs errors -- e.getMessage() has the exact text
            System.out.println("\n  Booking failed: " + e.getMessage());
        }
        return false;
    }

    // ----------------------------------------------------------
    // 1b. Helper: show ticket codes for one booking
    // ----------------------------------------------------------
    private void viewTicketsForBooking(int bookingId) {
        String sql = "SELECT ticket_id, ticket_code, ticket_status " +
                     "FROM Tickets WHERE booking_id = ? ORDER BY ticket_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("    [%d] %s  (%s)%n",
                        rs.getInt("ticket_id"),
                        rs.getString("ticket_code"),
                        rs.getString("ticket_status").toUpperCase());
                }
            }

        } catch (SQLException e) {
            System.out.println("  Could not fetch ticket codes: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 2. VIEW bookings for a specific event (admin use)
    // ----------------------------------------------------------
    public void viewBookingsForEvent(int eventId) {
        String sql =
            "SELECT b.booking_id, u.name AS customer, u.email, " +
            "       b.booking_date, b.ticket_quantity, b.total_amount, b.booking_status " +
            "FROM Bookings b " +
            "JOIN Users u ON b.user_id = u.user_id " +
            "WHERE b.event_id = ? " +
            "ORDER BY b.booking_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                printSeparator(80);
                System.out.printf("%-6s %-18s %-22s %-12s %4s %10s %-10s%n",
                    "BkgID", "Customer", "Email", "Date", "Qty", "Amount", "Status");
                printLine(80);
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-6d %-18s %-22s %-12s %4d %10.2f %-10s%n",
                        rs.getInt   ("booking_id"),
                        rs.getString("customer"),
                        rs.getString("email"),
                        rs.getString("booking_date").substring(0, 10),
                        rs.getInt   ("ticket_quantity"),
                        rs.getDouble("total_amount"),
                        rs.getString("booking_status").toUpperCase());
                }
                if (!found) System.out.println("  No bookings found for this event.");
                printSeparator(80);
            }

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 3. VIEW ALL bookings across all events (admin dashboard)
    // ----------------------------------------------------------
    public void viewAllBookings() {
        String sql =
            "SELECT b.booking_id, u.name AS customer, e.event_name, " +
            "       b.booking_date, b.ticket_quantity, b.total_amount, b.booking_status " +
            "FROM Bookings b " +
            "JOIN Users  u ON b.user_id  = u.user_id " +
            "JOIN Events e ON b.event_id = e.event_id " +
            "ORDER BY b.booking_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printSeparator(90);
            System.out.printf("%-6s %-18s %-25s %-12s %4s %10s %-10s%n",
                "ID", "Customer", "Event", "Date", "Qty", "Amount", "Status");
            printLine(90);
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-6d %-18s %-25s %-12s %4d %10.2f %-10s%n",
                    rs.getInt   ("booking_id"),
                    rs.getString("customer"),
                    rs.getString("event_name"),
                    rs.getString("booking_date").substring(0, 10),
                    rs.getInt   ("ticket_quantity"),
                    rs.getDouble("total_amount"),
                    rs.getString("booking_status").toUpperCase());
            }
            if (!found) System.out.println("  No bookings found.");
            printSeparator(90);

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 4. VIEW customer's own bookings
    // ----------------------------------------------------------
    public void viewMyBookings(int userId) {
        String sql =
            "SELECT b.booking_id, e.event_name, e.event_date, " +
            "       b.booking_date, b.ticket_quantity, b.total_amount, b.booking_status " +
            "FROM Bookings b " +
            "JOIN Events e ON b.event_id = e.event_id " +
            "WHERE b.user_id = ? " +
            "ORDER BY b.booking_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                printSeparator(85);
                System.out.printf("%-6s %-25s %-12s %-12s %4s %10s %-10s%n",
                    "ID", "Event Name", "Event Date", "Booked On", "Qty", "Amount", "Status");
                printLine(85);
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-6d %-25s %-12s %-12s %4d %10.2f %-10s%n",
                        rs.getInt   ("booking_id"),
                        rs.getString("event_name"),
                        rs.getString("event_date"),
                        rs.getString("booking_date").substring(0, 10),
                        rs.getInt   ("ticket_quantity"),
                        rs.getDouble("total_amount"),
                        rs.getString("booking_status").toUpperCase());
                }
                if (!found) System.out.println("  You have no bookings yet.");
                printSeparator(85);
            }

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 5. VIEW customer's individual ticket codes
    // ----------------------------------------------------------
    public void viewMyTickets(int userId) {
        String sql =
            "SELECT t.ticket_id, t.ticket_code, t.ticket_status, " +
            "       e.event_name, e.event_date, b.booking_id " +
            "FROM Tickets t " +
            "JOIN Bookings b ON t.booking_id = b.booking_id " +
            "JOIN Events   e ON b.event_id   = e.event_id " +
            "WHERE b.user_id = ? " +
            "ORDER BY b.booking_id, t.ticket_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                printSeparator(75);
                System.out.printf("%-6s %-20s %-12s %-10s %-10s%n",
                    "TktID", "Ticket Code", "Event Date", "Status", "Event");
                printLine(75);
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-6d %-20s %-12s %-10s %s%n",
                        rs.getInt   ("ticket_id"),
                        rs.getString("ticket_code"),
                        rs.getString("event_date"),
                        rs.getString("ticket_status").toUpperCase(),
                        rs.getString("event_name"));
                }
                if (!found) System.out.println("  No tickets found.");
                printSeparator(75);
            }

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 6. CANCEL a booking
    // Checks that the booking belongs to this user.
    // The trg_after_booking_cancel trigger automatically cancels
    // tickets and refunds the payment.
    // ----------------------------------------------------------
    public boolean cancelBooking(int bookingId, int userId) {
        // First verify this booking belongs to the requesting user
        String checkSql =
            "SELECT booking_status, event_id FROM Bookings " +
            "WHERE booking_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setInt(1, bookingId);
            check.setInt(2, userId);

            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("  Booking not found or does not belong to your account.");
                    return false;
                }
                if ("cancelled".equals(rs.getString("booking_status"))) {
                    System.out.println("  This booking is already cancelled.");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
            return false;
        }

        // Proceed with cancellation
        // The trigger trg_after_booking_cancel handles Tickets + Payments automatically
        String cancelSql =
            "UPDATE Bookings SET booking_status = 'cancelled' " +
            "WHERE booking_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(cancelSql)) {

            ps.setInt(1, bookingId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                AuthService.logAction(userId, "CANCEL_BOOKING",
                    "Booking #" + bookingId + " cancelled");
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.out.println("  Error cancelling booking: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------
    // 7. VIEW all payments (admin payment ledger)
    // ----------------------------------------------------------
    public void viewAllPayments() {
        String sql =
            "SELECT p.payment_id, p.booking_id, u.name AS customer, " +
            "       e.event_name, p.amount, p.payment_method, " +
            "       p.payment_status, p.payment_date " +
            "FROM Payments p " +
            "JOIN Bookings b ON p.booking_id = b.booking_id " +
            "JOIN Users    u ON b.user_id    = u.user_id " +
            "JOIN Events   e ON b.event_id   = e.event_id " +
            "ORDER BY p.payment_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printSeparator(90);
            System.out.printf("%-5s %-5s %-16s %-22s %10s %-8s %-9s%n",
                "PayID", "BkgID", "Customer", "Event", "Amount", "Method", "Status");
            printLine(90);
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-5d %-16s %-22s %10.2f %-8s %-9s%n",
                    rs.getInt   ("payment_id"),
                    rs.getInt   ("booking_id"),
                    rs.getString("customer"),
                    rs.getString("event_name"),
                    rs.getDouble("amount"),
                    rs.getString("payment_method").toUpperCase(),
                    rs.getString("payment_status").toUpperCase());
            }
            if (!found) System.out.println("  No payment records found.");
            printSeparator(90);

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 8. REVENUE REPORT
    // Demonstrates: SUM, COUNT, AVG, GROUP BY, HAVING
    // Shows only events with at least one paid booking.
    // ----------------------------------------------------------
    public void viewRevenueReport() {
        String sql =
            "SELECT e.event_name, " +
            "       COUNT(b.booking_id)        AS total_bookings, " +
            "       SUM(b.ticket_quantity)      AS total_tickets, " +
            "       SUM(p.amount)              AS total_revenue, " +
            "       AVG(p.amount)              AS avg_booking_value " +
            "FROM Events e " +
            "JOIN Bookings b ON e.event_id   = b.event_id " +
            "JOIN Payments p ON b.booking_id = p.booking_id " +
            "WHERE b.booking_status = 'confirmed' " +
            "  AND p.payment_status = 'paid' " +
            "GROUP BY e.event_id, e.event_name " +
            "HAVING SUM(p.amount) > 0 " +       // only events with paid revenue
            "ORDER BY total_revenue DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n  REVENUE REPORT (paid bookings only)");
            printSeparator(75);
            System.out.printf("%-25s %8s %7s %12s %15s%n",
                "Event Name", "Bookings", "Tickets", "Revenue(Rs)", "Avg/Booking(Rs)");
            printLine(75);

            double grandTotal = 0;
            boolean found = false;
            while (rs.next()) {
                found = true;
                double rev = rs.getDouble("total_revenue");
                grandTotal += rev;
                System.out.printf("%-25s %8d %7d %12.2f %15.2f%n",
                    rs.getString("event_name"),
                    rs.getInt   ("total_bookings"),
                    rs.getInt   ("total_tickets"),
                    rev,
                    rs.getDouble("avg_booking_value"));
            }
            printLine(75);
            if (found) {
                System.out.printf("%-25s %8s %7s %12.2f%n",
                    "GRAND TOTAL", "", "", grandTotal);
            } else {
                System.out.println("  No paid revenue recorded yet.");
            }
            printSeparator(75);

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 9. REGISTRATION STATISTICS (for college viva)
    // Demonstrates: COUNT, AVG, GROUP BY, HAVING
    // ----------------------------------------------------------
    public void viewRegistrationStats() {
        String sql =
            "SELECT e.event_id, e.event_name, e.capacity, " +
            "       COUNT(b.booking_id)         AS total_bookings, " +
            "       COALESCE(SUM(CASE WHEN b.booking_status='confirmed' " +
            "                    THEN b.ticket_quantity ELSE 0 END), 0) AS confirmed_tickets, " +
            "       e.capacity - COALESCE(SUM(CASE WHEN b.booking_status='confirmed' " +
            "                    THEN b.ticket_quantity ELSE 0 END), 0) AS available_seats, " +
            "       ROUND(100.0 * COALESCE(SUM(CASE WHEN b.booking_status='confirmed' " +
            "                    THEN b.ticket_quantity ELSE 0 END), 0) / e.capacity, 1) AS occupancy_pct " +
            "FROM Events e " +
            "LEFT JOIN Bookings b ON e.event_id = b.event_id " +
            "GROUP BY e.event_id, e.event_name, e.capacity " +
            "HAVING e.capacity > 0 " +
            "ORDER BY occupancy_pct DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n  REGISTRATION STATISTICS");
            printSeparator(80);
            System.out.printf("%-4s %-25s %8s %8s %8s %8s %8s%n",
                "ID", "Event", "Capacity", "Bookings", "Tickets", "Avail", "Occup%");
            printLine(80);
            while (rs.next()) {
                System.out.printf("%-4d %-25s %8d %8d %8d %8d %7.1f%%%n",
                    rs.getInt   ("event_id"),
                    rs.getString("event_name"),
                    rs.getInt   ("capacity"),
                    rs.getInt   ("total_bookings"),
                    rs.getInt   ("confirmed_tickets"),
                    rs.getInt   ("available_seats"),
                    rs.getDouble("occupancy_pct"));
            }
            printSeparator(80);

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 10. VIEW customer's payment history
    // ----------------------------------------------------------
    public void viewMyPayments(int userId) {
        String sql =
            "SELECT p.payment_id, e.event_name, p.amount, " +
            "       p.payment_method, p.payment_status, p.payment_date " +
            "FROM Payments p " +
            "JOIN Bookings b ON p.booking_id = b.booking_id " +
            "JOIN Events   e ON b.event_id   = e.event_id " +
            "WHERE b.user_id = ? " +
            "ORDER BY p.payment_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                printSeparator(75);
                System.out.printf("%-5s %-25s %10s %-8s %-9s %-12s%n",
                    "ID", "Event Name", "Amount", "Method", "Status", "Date");
                printLine(75);
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-5d %-25s %10.2f %-8s %-9s %-12s%n",
                        rs.getInt   ("payment_id"),
                        rs.getString("event_name"),
                        rs.getDouble("amount"),
                        rs.getString("payment_method").toUpperCase(),
                        rs.getString("payment_status").toUpperCase(),
                        rs.getString("payment_date").substring(0, 10));
                }
                if (!found) System.out.println("  No payment records found.");
                printSeparator(75);
            }

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 11. VIEW all customers (admin use)
    // ----------------------------------------------------------
    public void viewAllCustomers() {
        String sql =
            "SELECT u.user_id, u.name, u.email, u.phone, u.created_at, " +
            "       COUNT(b.booking_id) AS total_bookings " +
            "FROM Users u " +
            "LEFT JOIN Bookings b ON u.user_id = b.user_id " +
            "WHERE u.role = 'customer' " +
            "GROUP BY u.user_id, u.name, u.email, u.phone, u.created_at " +
            "ORDER BY u.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            printSeparator(80);
            System.out.printf("%-5s %-18s %-28s %-13s %8s%n",
                "ID", "Name", "Email", "Phone", "Bookings");
            printLine(80);
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-18s %-28s %-13s %8d%n",
                    rs.getInt   ("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("phone") != null ? rs.getString("phone") : "-",
                    rs.getInt   ("total_bookings"));
            }
            if (!found) System.out.println("  No customers registered yet.");
            printSeparator(80);

        } catch (SQLException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // Private display helpers
    // ----------------------------------------------------------
    private void printSeparator(int width) {
        StringBuilder sb = new StringBuilder("\n  ");
        for (int i = 0; i < width; i++) sb.append('=');
        System.out.println(sb.toString());
    }
    private void printLine(int width) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < width; i++) sb.append('-');
        System.out.println(sb.toString());
    }
}
