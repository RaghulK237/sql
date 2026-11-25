// ============================================================
// CustomerPanel.java
// Displays the Customer menu and handles all customer operations.
// Called by Main.java after a successful customer login.
//
// Menu options:
//  1. View All Available Events     7.  View My Bookings
//  2. Search Events by Name         8.  View My Tickets
//  3. Filter by Category            9.  Cancel My Booking
//  4. Filter by Date               10.  View My Payment History
//  5. View Event Details           11.  Logout
//  6. Book Tickets
// ============================================================

import java.util.List;
import java.util.Scanner;

public class CustomerPanel {

    private User       customer;
    private Scanner    sc;
    private EventDAO   eventDAO;
    private BookingDAO bookingDAO;

    public CustomerPanel(User customer, Scanner sc) {
        this.customer   = customer;
        this.sc         = sc;
        this.eventDAO   = new EventDAO();
        this.bookingDAO = new BookingDAO();
    }

    // ----------------------------------------------------------
    // Main loop -- runs until the customer logs out
    // ----------------------------------------------------------
    public void show() {
        System.out.println("\n  Welcome, " + customer.getName() + "!");
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("  Choice: ");
            switch (choice) {
                case  1: viewAvailableEvents();  break;
                case  2: searchEvents();         break;
                case  3: filterByCategory();     break;
                case  4: filterByDate();         break;
                case  5: viewEventDetails();     break;
                case  6: bookTickets();          break;
                case  7: viewMyBookings();       break;
                case  8: viewMyTickets();        break;
                case  9: cancelMyBooking();      break;
                case 10: viewMyPayments();       break;
                case 11:
                    AuthService.logAction(customer.getUserId(), "LOGOUT",
                        "Customer " + customer.getName() + " logged out");
                    System.out.println("\n  Logged out. See you at the next event, " +
                                       customer.getName() + "!");
                    running = false;
                    break;
                default:
                    System.out.println("  Invalid choice. Enter 1-11.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n  +------------------------------------------+");
        System.out.println("  |      EVENTSPHERE  CUSTOMER  PANEL        |");
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  DISCOVER EVENTS                         |");
        System.out.println("  |   1. View All Available Events           |");
        System.out.println("  |   2. Search Events by Name               |");
        System.out.println("  |   3. Filter Events by Category           |");
        System.out.println("  |   4. Filter Events by Date               |");
        System.out.println("  |   5. View Event Details                  |");
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  MY BOOKINGS                             |");
        System.out.println("  |   6. Book Tickets                        |");
        System.out.println("  |   7. View My Bookings                    |");
        System.out.println("  |   8. View My Tickets                     |");
        System.out.println("  |   9. Cancel My Booking                   |");
        System.out.println("  |  10. View My Payment History             |");
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  11. Logout                              |");
        System.out.println("  +------------------------------------------+");
    }

    // ==========================================================
    // 1. VIEW ALL AVAILABLE EVENTS
    // Shows only published, upcoming events with seats available
    // ==========================================================
    private void viewAvailableEvents() {
        System.out.println("\n  --- AVAILABLE EVENTS ---");
        List<Event> events = eventDAO.viewAvailableEvents();
        if (events.isEmpty()) {
            System.out.println("  No upcoming events are available right now.");
            return;
        }
        printEventListHeader();
        for (Event e : events) {
            System.out.println(e.toListRow());
        }
        System.out.println("\n  Total: " + events.size() + " event(s) available.");
    }

    // ==========================================================
    // 2. SEARCH EVENTS BY NAME
    // ==========================================================
    private void searchEvents() {
        System.out.println("\n  --- SEARCH EVENTS ---");
        System.out.print("  Enter search keyword: ");
        String keyword = sc.nextLine().trim();
        if (keyword.isEmpty()) { System.out.println("  Please enter a keyword."); return; }

        List<Event> events = eventDAO.searchByName(keyword);
        // Show only published upcoming events to the customer
        printEventListHeader();
        boolean found = false;
        for (Event e : events) {
            if ("published".equals(e.getStatus())) {
                System.out.println(e.toListRow());
                found = true;
            }
        }
        if (!found) System.out.println("  No published events found matching: " + keyword);
    }

    // ==========================================================
    // 3. FILTER BY CATEGORY
    // ==========================================================
    private void filterByCategory() {
        System.out.println("\n  --- FILTER BY CATEGORY ---");
        eventDAO.listCategories();
        int catId = readInt("  Enter Category ID: ");
        List<Event> events = eventDAO.filterByCategory(catId);
        printEventListHeader();
        if (events.isEmpty()) {
            System.out.println("  No events found in this category.");
        } else {
            for (Event e : events) System.out.println(e.toListRow());
        }
    }

    // ==========================================================
    // 4. FILTER BY DATE
    // ==========================================================
    private void filterByDate() {
        System.out.println("\n  --- FILTER BY DATE ---");
        System.out.print("  Enter date (yyyy-MM-dd): ");
        String date = sc.nextLine().trim();
        List<Event> events = eventDAO.filterByDate(date);
        printEventListHeader();
        if (events.isEmpty()) {
            System.out.println("  No events found on " + date);
        } else {
            for (Event e : events) System.out.println(e.toListRow());
        }
    }

    // ==========================================================
    // 5. VIEW EVENT DETAILS
    // ==========================================================
    private void viewEventDetails() {
        System.out.println("\n  --- EVENT DETAILS ---");
        int id = readInt("  Enter Event ID: ");
        Event e = eventDAO.searchById(id);
        if (e == null || !"published".equals(e.getStatus())) {
            System.out.println("  Event not found or not available.");
            return;
        }
        System.out.println(e);
    }

    // ==========================================================
    // 6. BOOK TICKETS
    // ==========================================================
    private void bookTickets() {
        System.out.println("\n  --- BOOK TICKETS ---");

        // First show available events
        List<Event> events = eventDAO.viewAvailableEvents();
        if (events.isEmpty()) {
            System.out.println("  No events available for booking right now.");
            return;
        }
        printEventListHeader();
        for (Event e : events) System.out.println(e.toListRow());

        int eventId = readInt("\n  Enter Event ID to book: ");
        Event selected = eventDAO.searchById(eventId);

        if (selected == null) {
            System.out.println("  Event not found.");
            return;
        }
        if (!"published".equals(selected.getStatus())) {
            System.out.println("  This event is not available for booking.");
            return;
        }
        if (selected.getAvailableSeats() <= 0) {
            System.out.println("  Sorry, this event is fully booked.");
            return;
        }

        // Show event summary
        System.out.println("\n  Event    : " + selected.getEventName());
        System.out.println("  Date     : " + selected.getEventDate() + "  " +
                           selected.getStartTime() + " - " + selected.getEndTime());
        System.out.println("  Venue    : " + selected.getVenueName());
        String priceDisplay = selected.getTicketPrice() == 0
            ? "FREE"
            : "Rs. " + String.format("%.2f", selected.getTicketPrice()) + " per ticket";
        System.out.println("  Price    : " + priceDisplay);
        System.out.println("  Available: " + selected.getAvailableSeats() + " seat(s)");

        int qty = readInt("  How many tickets do you want? ");
        if (qty <= 0) {
            System.out.println("  Quantity must be at least 1.");
            return;
        }
        if (qty > selected.getAvailableSeats()) {
            System.out.println("  Only " + selected.getAvailableSeats() + " seat(s) available.");
            return;
        }

        // Show cost confirmation
        double totalCost = selected.getTicketPrice() * qty;
        if (totalCost > 0) {
            System.out.printf("%n  Total Cost: Rs. %.2f (%d ticket(s) * Rs. %.2f)%n",
                totalCost, qty, selected.getTicketPrice());
        } else {
            System.out.println("\n  This is a FREE event. No payment required.");
        }
        System.out.print("  Confirm booking? (yes/no): ");
        if (!sc.nextLine().equalsIgnoreCase("yes")) {
            System.out.println("  Booking cancelled.");
            return;
        }

        // Call the stored procedure through BookingDAO
        bookingDAO.bookEvent(customer.getUserId(), eventId, qty);
    }

    // ==========================================================
    // 7. VIEW MY BOOKINGS
    // ==========================================================
    private void viewMyBookings() {
        System.out.println("\n  --- MY BOOKINGS ---");
        bookingDAO.viewMyBookings(customer.getUserId());
    }

    // ==========================================================
    // 8. VIEW MY TICKETS
    // ==========================================================
    private void viewMyTickets() {
        System.out.println("\n  --- MY TICKETS ---");
        bookingDAO.viewMyTickets(customer.getUserId());
    }

    // ==========================================================
    // 9. CANCEL MY BOOKING
    // ==========================================================
    private void cancelMyBooking() {
        System.out.println("\n  --- CANCEL BOOKING ---");

        // Show the customer's own active bookings first
        bookingDAO.viewMyBookings(customer.getUserId());

        int bookingId = readInt("  Enter Booking ID to cancel: ");
        System.out.print("  Type YES to confirm cancellation: ");
        if (!sc.nextLine().equalsIgnoreCase("YES")) {
            System.out.println("  Cancellation aborted.");
            return;
        }

        boolean ok = bookingDAO.cancelBooking(bookingId, customer.getUserId());
        if (ok) {
            System.out.println("  [OK] Booking #" + bookingId + " cancelled.");
            System.out.println("  Your tickets have been invalidated and your payment refunded.");
        }
    }

    // ==========================================================
    // 10. VIEW MY PAYMENT HISTORY
    // ==========================================================
    private void viewMyPayments() {
        System.out.println("\n  --- MY PAYMENT HISTORY ---");
        bookingDAO.viewMyPayments(customer.getUserId());
    }

    // ==========================================================
    // PRIVATE HELPERS
    // ==========================================================

    private void printEventListHeader() {
        System.out.printf("%n  %-5s %-28s %-12s %-22s %-10s %-8s %s%n",
            "ID", "Event Name", "Date", "Venue", "Category", "Price", "Seats");
        System.out.println("  ----------------------------------------------------------------------------------------------------");
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  Please enter a whole number."); }
        }
    }
}
