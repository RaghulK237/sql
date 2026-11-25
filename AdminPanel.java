// ============================================================
// AdminPanel.java
// Displays the Admin menu and handles all admin operations.
// Called by Main.java after a successful admin login.
//
// Menu options:
//  1. View Dashboard          9.  Manage Venues
//  2. Add New Event          10.  Manage Categories
//  3. View All Events        11.  View All Customers
//  4. Update Event           12.  View All Bookings
//  5. Schedule/Reschedule    13.  View Bookings for Event
//  6. Publish Event          14.  View Payment Records
//  7. Cancel Event           15.  View Revenue Reports
//  8. Delete Event           16.  View Registration Statistics
//                            17.  Logout
// ============================================================

import java.sql.*;
import java.util.List;
import java.util.Scanner;

public class AdminPanel {

    private User       admin;
    private Scanner    sc;
    private EventDAO   eventDAO;
    private BookingDAO bookingDAO;

    public AdminPanel(User admin, Scanner sc) {
        this.admin      = admin;
        this.sc         = sc;
        this.eventDAO   = new EventDAO();
        this.bookingDAO = new BookingDAO();
    }

    // ----------------------------------------------------------
    // Main loop -- runs until admin logs out
    // ----------------------------------------------------------
    public void show() {
        System.out.println("\n  Welcome, " + admin.getName() + "!");
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("  Choice: ");
            switch (choice) {
                case  1: viewDashboard();           break;
                case  2: addEvent();                break;
                case  3: viewAllEvents();           break;
                case  4: updateEvent();             break;
                case  5: scheduleEvent();           break;
                case  6: publishEvent();            break;
                case  7: cancelEvent();             break;
                case  8: deleteEvent();             break;
                case  9: manageVenues();            break;
                case 10: manageCategories();        break;
                case 11: viewAllCustomers();        break;
                case 12: viewAllBookings();         break;
                case 13: viewBookingsForEvent();    break;
                case 14: viewPaymentRecords();      break;
                case 15: viewRevenueReports();      break;
                case 16: viewRegistrationStats();   break;
                case 17:
                    AuthService.logAction(admin.getUserId(), "LOGOUT",
                        "Admin " + admin.getName() + " logged out");
                    System.out.println("\n  Logged out. Goodbye, " + admin.getName() + "!");
                    running = false;
                    break;
                default:
                    System.out.println("  Invalid choice. Enter 1-17 or 17 to logout.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n  +------------------------------------------+");
        System.out.println("  |       EVENTSPHERE  ADMIN  PANEL          |");
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  EVENT MANAGEMENT                        |");
        System.out.println("  |   1. View Dashboard                      |");
        System.out.println("  |   2. Add New Event                       |");
        System.out.println("  |   3. View All Events                     |");
        System.out.println("  |   4. Update Event Details                |");
        System.out.println("  |   5. Schedule / Reschedule Event         |");
        System.out.println("  |   6. Publish Event                       |");
        System.out.println("  |   7. Cancel Event                        |");
        System.out.println("  |   8. Delete Event                        |");
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  MASTER DATA                             |");
        System.out.println("  |   9. Manage Venues                       |");
        System.out.println("  |  10. Manage Categories                   |");
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  CUSTOMERS & BOOKINGS                    |");
        System.out.println("  |  11. View All Customers                  |");
        System.out.println("  |  12. View All Bookings                   |");
        System.out.println("  |  13. View Bookings for a Specific Event  |");
        System.out.println("  |  14. View Payment Records                |");
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  REPORTS                                 |");
        System.out.println("  |  15. Revenue Report                      |");
        System.out.println("  |  16. Registration Statistics             |");
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  17. Logout                              |");
        System.out.println("  +------------------------------------------+");
    }

    // ==========================================================
    // 1. DASHBOARD
    // ==========================================================
    private void viewDashboard() {
        System.out.println("\n  +------ ADMIN DASHBOARD ------+");
        String sql =
            "SELECT " +
            "  (SELECT COUNT(*) FROM Events)                               AS total_events, " +
            "  (SELECT COUNT(*) FROM Events WHERE status='published')      AS published, " +
            "  (SELECT COUNT(*) FROM Events WHERE status='draft')          AS draft, " +
            "  (SELECT COUNT(*) FROM Events WHERE status='cancelled')      AS cancelled, " +
            "  (SELECT COUNT(*) FROM Events WHERE status='published' " +
            "                              AND event_date >= CURDATE())    AS upcoming, " +
            "  (SELECT COUNT(*) FROM Users  WHERE role='customer')         AS total_customers, " +
            "  (SELECT COUNT(*) FROM Bookings WHERE booking_status='confirmed') AS total_bookings, " +
            "  (SELECT COALESCE(SUM(ticket_quantity),0) FROM Bookings " +
            "   WHERE booking_status='confirmed')                          AS total_tickets, " +
            "  (SELECT COALESCE(SUM(amount),0) FROM Payments " +
            "   WHERE payment_status='paid')                               AS total_revenue, " +
            "  (SELECT COALESCE(SUM(e.capacity - " +
            "           COALESCE((SELECT SUM(b.ticket_quantity) FROM Bookings b " +
            "                     WHERE b.event_id=e.event_id AND b.booking_status='confirmed'),0)),0) " +
            "   FROM Events e WHERE e.status='published' AND e.event_date>=CURDATE()) AS avail_seats";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                System.out.printf("  %-32s : %d%n", "Total Events",          rs.getInt("total_events"));
                System.out.printf("  %-32s : %d%n", "Published Events",      rs.getInt("published"));
                System.out.printf("  %-32s : %d%n", "Draft Events",          rs.getInt("draft"));
                System.out.printf("  %-32s : %d%n", "Cancelled Events",      rs.getInt("cancelled"));
                System.out.printf("  %-32s : %d%n", "Upcoming Events",       rs.getInt("upcoming"));
                System.out.println("  ----------------------------------------");
                System.out.printf("  %-32s : %d%n", "Registered Customers",  rs.getInt("total_customers"));
                System.out.printf("  %-32s : %d%n", "Total Bookings",        rs.getInt("total_bookings"));
                System.out.printf("  %-32s : %d%n", "Total Tickets Sold",    rs.getInt("total_tickets"));
                System.out.printf("  %-32s : Rs. %.2f%n", "Total Revenue",   rs.getDouble("total_revenue"));
                System.out.printf("  %-32s : %d%n", "Available Seats (Upcoming)", rs.getInt("avail_seats"));
            }
        } catch (SQLException e) {
            System.out.println("  Error loading dashboard: " + e.getMessage());
        }
        System.out.println("  +-----------------------------+");
    }

    // ==========================================================
    // 2. ADD EVENT
    // ==========================================================
    private void addEvent() {
        System.out.println("\n  --- ADD NEW EVENT ---");

        // Show venues and categories from DB (NOT hardcoded)
        eventDAO.listVenues();
        int venueId = readInt("  Select Venue ID: ");

        eventDAO.listCategories();
        int categoryId = readInt("  Select Category ID: ");

        System.out.print("  Event Name       : "); String name = sc.nextLine();
        System.out.print("  Description      : "); String desc = sc.nextLine();
        System.out.print("  Event Date (yyyy-MM-dd)  : "); String date = sc.nextLine();
        System.out.print("  Start Time (HH:mm)       : "); String start = sc.nextLine();
        System.out.print("  End Time   (HH:mm)       : "); String end = sc.nextLine();

        // Validate: end time must be after start time
        if (end.compareTo(start) <= 0) {
            System.out.println("  Error: End time must be after start time.");
            return;
        }

        int    capacity = readPositiveInt("  Capacity (max attendees): ");
        double price    = readNonNegativeDouble("  Ticket Price (Rs, 0 for free): ");

        // Check venue capacity
        int venueCapacity = getVenueCapacity(venueId);
        if (venueCapacity > 0 && capacity > venueCapacity) {
            System.out.println("  Warning: Capacity (" + capacity +
                               ") exceeds venue capacity (" + venueCapacity + ").");
            System.out.print("  Continue anyway? (yes/no): ");
            if (!sc.nextLine().equalsIgnoreCase("yes")) return;
        }

        // Check for venue time conflict
        if (eventDAO.checkVenueConflict(venueId, date, start + ":00", end + ":00", 0)) {
            System.out.println("  Error: This venue already has an event at that time.");
            return;
        }

        boolean ok = eventDAO.addEvent(name, desc, categoryId, venueId,
                                       date, start + ":00", end + ":00", price, capacity);
        if (ok) {
            System.out.println("  [OK] Event '" + name + "' added as DRAFT. Use option 6 to publish.");
        } else {
            System.out.println("  [FAILED] Failed to add event.");
        }
    }

    // ==========================================================
    // 3. VIEW ALL EVENTS
    // ==========================================================
    private void viewAllEvents() {
        System.out.println("\n  --- ALL EVENTS ---");
        List<Event> events = eventDAO.viewAllEvents();
        if (events.isEmpty()) {
            System.out.println("  No events found.");
            return;
        }
        System.out.printf("%n  %-4s %-28s %-12s %-22s %-12s %-8s %s%n",
            "ID", "Name", "Date", "Venue", "Category", "Price", "Status");
        System.out.println("  ----------------------------------------------------------------------------------------------------");
        for (Event e : events) {
            String price = e.getTicketPrice() == 0 ? "FREE"
                         : String.format("Rs.%.0f", e.getTicketPrice());
            System.out.printf("  %-4d %-28s %-12s %-22s %-12s %-8s %s%n",
                e.getEventId(), e.getEventName(), e.getEventDate(),
                e.getVenueName(), e.getCategoryName(), price,
                e.getStatus().toUpperCase());
        }
        System.out.println("  Total: " + events.size() + " event(s)");
    }

    // ==========================================================
    // 4. UPDATE EVENT
    // ==========================================================
    private void updateEvent() {
        System.out.println("\n  --- UPDATE EVENT ---");
        int id = readInt("  Enter Event ID: ");
        Event e = eventDAO.searchById(id);
        if (e == null) { System.out.println("  Event not found."); return; }

        System.out.println(e);
        System.out.println("\n  Press Enter to keep the current value.");

        String name = promptOrKeep("  New name       [" + e.getEventName()   + "]: ", e.getEventName());
        String desc = promptOrKeep("  New description[" + e.getDescription() + "]: ", e.getDescription());
        String date = promptOrKeep("  New date       [" + e.getEventDate()   + "]: ", e.getEventDate());
        String start= promptOrKeep("  New start time [" + e.getStartTime()   + "]: ", e.getStartTime());
        String end  = promptOrKeep("  New end time   [" + e.getEndTime()     + "]: ", e.getEndTime());

        System.out.print("  New capacity   [" + e.getCapacity()   + "]: ");
        String capStr = sc.nextLine().trim();
        int capacity = capStr.isEmpty() ? e.getCapacity() : Integer.parseInt(capStr);

        System.out.print("  New price      [" + e.getTicketPrice()+ "]: ");
        String priceStr = sc.nextLine().trim();
        double price = priceStr.isEmpty() ? e.getTicketPrice() : Double.parseDouble(priceStr);

        boolean ok = eventDAO.updateEvent(id, name, desc, date, start, end, capacity, price);
        System.out.println(ok ? "  [OK] Event updated." : "  [FAILED] Update failed.");
    }

    // ==========================================================
    // 5. SCHEDULE / RESCHEDULE
    // ==========================================================
    private void scheduleEvent() {
        System.out.println("\n  --- RESCHEDULE EVENT ---");
        int id = readInt("  Enter Event ID: ");
        Event e = eventDAO.searchById(id);
        if (e == null) { System.out.println("  Event not found."); return; }

        System.out.println("  Current: " + e.getEventDate() + "  " + e.getStartTime() + " - " + e.getEndTime());
        System.out.print("  New Date       (yyyy-MM-dd): "); String date  = sc.nextLine();
        System.out.print("  New Start Time (HH:mm)     : "); String start = sc.nextLine();
        System.out.print("  New End Time   (HH:mm)     : "); String end   = sc.nextLine();

        if (end.compareTo(start) <= 0) {
            System.out.println("  Error: End time must be after start time.");
            return;
        }
        if (eventDAO.checkVenueConflict(e.getVenueId(), date, start + ":00", end + ":00", id)) {
            System.out.println("  Error: Venue conflict. Choose a different time.");
            return;
        }
        boolean ok = eventDAO.rescheduleEvent(id, date, start + ":00", end + ":00");
        System.out.println(ok ? "  [OK] Event rescheduled." : "  [FAILED] Reschedule failed.");
    }

    // ==========================================================
    // 6. PUBLISH EVENT
    // ==========================================================
    private void publishEvent() {
        System.out.println("\n  --- PUBLISH EVENT ---");
        int id = readInt("  Enter Event ID to publish: ");
        boolean ok = eventDAO.publishEvent(id);
        System.out.println(ok ? "  [OK] Event published. Customers can now see and book it."
                              : "  [FAILED] Could not publish. Event may not exist or not be in draft.");
    }

    // ==========================================================
    // 7. CANCEL EVENT
    // ==========================================================
    private void cancelEvent() {
        System.out.println("\n  --- CANCEL EVENT ---");
        int id = readInt("  Enter Event ID to cancel: ");
        Event e = eventDAO.searchById(id);
        if (e == null) { System.out.println("  Event not found."); return; }

        System.out.println("  Event: " + e.getEventName());
        System.out.print("  This will prevent new bookings. Type YES to confirm: ");
        if (!sc.nextLine().equalsIgnoreCase("YES")) {
            System.out.println("  Cancelled."); return;
        }
        boolean ok = eventDAO.cancelEvent(id);
        System.out.println(ok ? "  [OK] Event cancelled." : "  [FAILED] Could not cancel.");
    }

    // ==========================================================
    // 8. DELETE EVENT
    // ==========================================================
    private void deleteEvent() {
        System.out.println("\n  --- DELETE EVENT ---");
        int id = readInt("  Enter Event ID to delete: ");
        Event e = eventDAO.searchById(id);
        if (e == null) { System.out.println("  Event not found."); return; }

        System.out.println("  You are about to permanently delete: " + e.getEventName());
        System.out.println("  WARNING: All bookings, tickets, and payments will also be deleted.");
        System.out.print("  Type DELETE to confirm: ");
        if (!sc.nextLine().equalsIgnoreCase("DELETE")) {
            System.out.println("  Delete cancelled."); return;
        }
        boolean ok = eventDAO.deleteEvent(id);
        System.out.println(ok ? "  [OK] Event and all related records deleted."
                              : "  [FAILED] Delete failed.");
    }

    // ==========================================================
    // 9. MANAGE VENUES
    // ==========================================================
    private void manageVenues() {
        System.out.println("\n  --- MANAGE VENUES ---");
        System.out.println("  1. View all venues");
        System.out.println("  2. Add new venue");
        int choice = readInt("  Choice: ");
        if (choice == 1) {
            eventDAO.listVenues();
        } else if (choice == 2) {
            System.out.print("  Venue Name : "); String name = sc.nextLine();
            System.out.print("  Location   : "); String loc  = sc.nextLine();
            int cap = readPositiveInt("  Capacity   : ");
            boolean ok = eventDAO.addVenue(name, loc, cap);
            System.out.println(ok ? "  [OK] Venue added." : "  [FAILED] Failed.");
        }
    }

    // ==========================================================
    // 10. MANAGE CATEGORIES
    // ==========================================================
    private void manageCategories() {
        System.out.println("\n  --- MANAGE CATEGORIES ---");
        System.out.println("  1. View all categories");
        System.out.println("  2. Add new category");
        int choice = readInt("  Choice: ");
        if (choice == 1) {
            eventDAO.listCategories();
        } else if (choice == 2) {
            System.out.print("  Category Name: "); String name = sc.nextLine();
            boolean ok = eventDAO.addCategory(name);
            System.out.println(ok ? "  [OK] Category added." : "  [FAILED] Failed.");
        }
    }

    // ==========================================================
    // 11. VIEW ALL CUSTOMERS
    // ==========================================================
    private void viewAllCustomers() {
        System.out.println("\n  --- ALL CUSTOMERS ---");
        bookingDAO.viewAllCustomers();
    }

    // ==========================================================
    // 12. VIEW ALL BOOKINGS
    // ==========================================================
    private void viewAllBookings() {
        System.out.println("\n  --- ALL BOOKINGS ---");
        bookingDAO.viewAllBookings();
    }

    // ==========================================================
    // 13. VIEW BOOKINGS FOR A SPECIFIC EVENT
    // ==========================================================
    private void viewBookingsForEvent() {
        System.out.println("\n  --- BOOKINGS FOR EVENT ---");
        int eventId = readInt("  Enter Event ID: ");
        Event e = eventDAO.searchById(eventId);
        if (e != null) {
            System.out.println("  Event: " + e.getEventName() +
                               " | Available: " + e.getAvailableSeats() + "/" + e.getCapacity());
        }
        bookingDAO.viewBookingsForEvent(eventId);
    }

    // ==========================================================
    // 14. VIEW PAYMENT RECORDS
    // ==========================================================
    private void viewPaymentRecords() {
        System.out.println("\n  --- PAYMENT RECORDS ---");
        bookingDAO.viewAllPayments();
    }

    // ==========================================================
    // 15. REVENUE REPORT
    // ==========================================================
    private void viewRevenueReports() {
        bookingDAO.viewRevenueReport();
    }

    // ==========================================================
    // 16. REGISTRATION STATISTICS
    // ==========================================================
    private void viewRegistrationStats() {
        bookingDAO.viewRegistrationStats();
    }

    // ==========================================================
    // PRIVATE HELPERS
    // ==========================================================

    private int getVenueCapacity(int venueId) {
        String sql = "SELECT capacity FROM Venues WHERE venue_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, venueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("capacity");
            }
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    private String promptOrKeep(String prompt, String current) {
        System.out.print(prompt);
        String val = sc.nextLine().trim();
        return val.isEmpty() ? current : val;
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  Please enter a whole number."); }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            int v = readInt(prompt);
            if (v > 0) return v;
            System.out.println("  Value must be greater than 0.");
        }
    }

    private double readNonNegativeDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(sc.nextLine().trim());
                if (v >= 0) return v;
                System.out.println("  Price cannot be negative.");
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid number.");
            }
        }
    }
}
