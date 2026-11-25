// ============================================================
// EventDAO.java  (v2 -- complete rewrite)
// All database operations for Events.
//
// Key changes from v1:
//  - All SELECT queries JOIN Venues + Categories (no raw IDs shown)
//  - Available seats calculated LIVE from Bookings (no column)
//  - New methods: publishEvent, cancelEvent, scheduleEvent,
//    filterByCategory, filterByDate, viewAvailableEvents,
//    checkVenueConflict, listVenues, listCategories
//  - updateEvent now accepts all relevant fields
//  - deleteEvent cascades Tickets -> Payments -> Bookings -> Event
// ============================================================

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    // -- Shared SQL fragment: the full event SELECT with JOINs --
    // We reuse this in viewAll, searchById, searchByName, etc.
    // The subquery calculates available seats live from Bookings.
    private static final String EVENT_SELECT =
        "SELECT e.event_id, e.event_name, e.description, " +
        "       e.category_id, c.category_name, " +
        "       e.venue_id, v.venue_name, " +
        "       DATE_FORMAT(e.event_date, '%Y-%m-%d') AS event_date, " +
        "       TIME_FORMAT(e.start_time, '%H:%i') AS start_time, " +
        "       TIME_FORMAT(e.end_time,   '%H:%i') AS end_time, " +
        "       e.ticket_price, e.capacity, e.status, " +
        "       DATE_FORMAT(e.created_at, '%Y-%m-%d') AS created_at, " +
        "       COALESCE((SELECT SUM(b.ticket_quantity) " +
        "                 FROM Bookings b " +
        "                 WHERE b.event_id = e.event_id " +
        "                   AND b.booking_status = 'confirmed'), 0) AS booked_seats " +
        "FROM Events e " +
        "JOIN Categories c ON e.category_id = c.category_id " +
        "JOIN Venues     v ON e.venue_id     = v.venue_id ";

    // ----------------------------------------------------------
    // 1. ADD a new event (status starts as 'draft')
    // ----------------------------------------------------------
    public boolean addEvent(String name, String description, int categoryId,
                            int venueId, String date, String startTime,
                            String endTime, double price, int capacity) {
        String sql =
            "INSERT INTO Events (event_name, description, category_id, venue_id, " +
            "event_date, start_time, end_time, ticket_price, capacity, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'draft')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, description);
            ps.setInt   (3, categoryId);
            ps.setInt   (4, venueId);
            ps.setString(5, date);
            ps.setString(6, startTime);
            ps.setString(7, endTime);
            ps.setDouble(8, price);
            ps.setInt   (9, capacity);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("  Error adding event: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------
    // 2. VIEW ALL events (admin sees all statuses)
    // ----------------------------------------------------------
    public List<Event> viewAllEvents() {
        String sql = EVENT_SELECT + "ORDER BY e.event_date ASC";
        return fetchEventList(sql, new Object[0]);
    }

    // ----------------------------------------------------------
    // 3. VIEW only published upcoming events (customer view)
    // ----------------------------------------------------------
    public List<Event> viewAvailableEvents() {
        String sql = EVENT_SELECT +
            "WHERE e.status = 'published' " +
            "  AND e.event_date >= CURDATE() " +
            "ORDER BY e.event_date ASC";
        return fetchEventList(sql, new Object[0]);
    }

    // ----------------------------------------------------------
    // 4. SEARCH by event ID
    // ----------------------------------------------------------
    public Event searchById(int id) {
        String sql = EVENT_SELECT + "WHERE e.event_id = ?";
        List<Event> results = fetchEventList(sql, new Object[]{id});
        return results.isEmpty() ? null : results.get(0);
    }

    // ----------------------------------------------------------
    // 5. SEARCH by name keyword (LIKE '%keyword%')
    // ----------------------------------------------------------
    public List<Event> searchByName(String keyword) {
        String sql = EVENT_SELECT + "WHERE e.event_name LIKE ? ORDER BY e.event_date ASC";
        return fetchEventList(sql, new Object[]{"%" + keyword + "%"});
    }

    // ----------------------------------------------------------
    // 6. FILTER by category ID
    // ----------------------------------------------------------
    public List<Event> filterByCategory(int categoryId) {
        String sql = EVENT_SELECT +
            "WHERE e.category_id = ? AND e.status = 'published' " +
            "AND e.event_date >= CURDATE() ORDER BY e.event_date ASC";
        return fetchEventList(sql, new Object[]{categoryId});
    }

    // ----------------------------------------------------------
    // 7. FILTER by date (exact date match)
    // ----------------------------------------------------------
    public List<Event> filterByDate(String date) {
        String sql = EVENT_SELECT +
            "WHERE e.event_date = ? AND e.status = 'published' ORDER BY e.start_time ASC";
        return fetchEventList(sql, new Object[]{date});
    }

    // ----------------------------------------------------------
    // 8. UPDATE event details
    // ----------------------------------------------------------
    public boolean updateEvent(int id, String name, String description,
                               String date, String startTime, String endTime,
                               int capacity, double price) {
        String sql =
            "UPDATE Events SET event_name=?, description=?, event_date=?, " +
            "start_time=?, end_time=?, capacity=?, ticket_price=? " +
            "WHERE event_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, date);
            ps.setString(4, startTime);
            ps.setString(5, endTime);
            ps.setInt   (6, capacity);
            ps.setDouble(7, price);
            ps.setInt   (8, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("  Error updating event: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------
    // 9. PUBLISH event (draft -> published)
    // ----------------------------------------------------------
    public boolean publishEvent(int id) {
        return updateStatus(id, "published", "draft");
    }

    // ----------------------------------------------------------
    // 10. CANCEL event (published -> cancelled)
    // ----------------------------------------------------------
    public boolean cancelEvent(int id) {
        return updateStatus(id, "cancelled", null);  // null = any current status
    }

    // ----------------------------------------------------------
    // 11. RESCHEDULE -- update date and time only
    // ----------------------------------------------------------
    public boolean rescheduleEvent(int id, String newDate, String newStart, String newEnd) {
        String sql = "UPDATE Events SET event_date=?, start_time=?, end_time=? WHERE event_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newDate);
            ps.setString(2, newStart);
            ps.setString(3, newEnd);
            ps.setInt   (4, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("  Error rescheduling event: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------
    // 12. DELETE event and all dependent records
    // Order: Payments -> Tickets -> Bookings -> Event
    // This respects foreign key constraints.
    // ----------------------------------------------------------
    public boolean deleteEvent(int id) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);  // manual transaction

            // Step 1: Delete Payments for this event's bookings
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Payments WHERE booking_id IN " +
                    "(SELECT booking_id FROM Bookings WHERE event_id = ?)")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // Step 2: Delete Tickets for this event's bookings
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Tickets WHERE booking_id IN " +
                    "(SELECT booking_id FROM Bookings WHERE event_id = ?)")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // Step 3: Delete Bookings for this event
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Bookings WHERE event_id = ?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }

            // Step 4: Delete the Event itself
            int rows;
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Events WHERE event_id = ?")) {
                ps.setInt(1, id);
                rows = ps.executeUpdate();
            }

            conn.commit();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("  Error deleting event: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } }
            catch (SQLException ignored) {}
        }
    }

    // ----------------------------------------------------------
    // 13. CHECK VENUE CONFLICT
    // Returns true if a venue is already booked for an overlapping
    // time window on the same date (prevents double-booking a venue).
    // excludeEventId = 0 when adding, or the current event ID when updating.
    // ----------------------------------------------------------
    public boolean checkVenueConflict(int venueId, String date,
                                      String startTime, String endTime,
                                      int excludeEventId) {
        String sql =
            "SELECT COUNT(*) FROM Events " +
            "WHERE venue_id = ? " +
            "  AND event_date = ? " +
            "  AND status != 'cancelled' " +
            "  AND event_id != ? " +
            "  AND NOT (end_time <= ? OR start_time >= ?)";  // overlapping times

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt   (1, venueId);
            ps.setString(2, date);
            ps.setInt   (3, excludeEventId);
            ps.setString(4, startTime);  // other event ends before this one starts
            ps.setString(5, endTime);    // other event starts after this one ends

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.out.println("  Error checking venue conflict: " + e.getMessage());
        }
        return false;
    }

    // ----------------------------------------------------------
    // 14. LIST all venues (used when admin creates an event)
    // ----------------------------------------------------------
    public void listVenues() {
        String sql = "SELECT venue_id, venue_name, location, capacity FROM Venues ORDER BY venue_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n  Available Venues:");
            System.out.printf("  %-4s %-25s %-25s %8s%n", "ID", "Venue Name", "Location", "Capacity");
            System.out.println("  ------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("  %-4d %-25s %-25s %8d%n",
                    rs.getInt("venue_id"), rs.getString("venue_name"),
                    rs.getString("location"), rs.getInt("capacity"));
            }

        } catch (SQLException e) {
            System.out.println("  Error fetching venues: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 15. LIST all categories
    // ----------------------------------------------------------
    public void listCategories() {
        String sql = "SELECT category_id, category_name FROM Categories ORDER BY category_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n  Available Categories:");
            while (rs.next()) {
                System.out.printf("  [%d] %s%n",
                    rs.getInt("category_id"), rs.getString("category_name"));
            }

        } catch (SQLException e) {
            System.out.println("  Error fetching categories: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 16. ADD a new venue
    // ----------------------------------------------------------
    public boolean addVenue(String name, String location, int capacity) {
        String sql = "INSERT INTO Venues (venue_name, location, capacity) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, location);
            ps.setInt   (3, capacity);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("  Error adding venue: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------
    // 17. ADD a new category
    // ----------------------------------------------------------
    public boolean addCategory(String name) {
        String sql = "INSERT INTO Categories (category_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("  Error: Category '" + name + "' already exists.");
            } else {
                System.out.println("  Error adding category: " + e.getMessage());
            }
            return false;
        }
    }

    // ----------------------------------------------------------
    // PRIVATE HELPERS
    // ----------------------------------------------------------

    // Generic event list fetcher -- builds Event objects from ResultSet
    private List<Event> fetchEventList(String sql, Object[] params) {
        List<Event> events = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.out.println("  Error fetching events: " + e.getMessage());
        }
        return events;
    }

    // Converts a single ResultSet row into an Event object
    private Event mapRow(ResultSet rs) throws SQLException {
        return new Event(
            rs.getInt   ("event_id"),
            rs.getString("event_name"),
            rs.getString("description"),
            rs.getInt   ("category_id"),
            rs.getString("category_name"),
            rs.getInt   ("venue_id"),
            rs.getString("venue_name"),
            rs.getString("event_date"),
            rs.getString("start_time"),
            rs.getString("end_time"),
            rs.getDouble("ticket_price"),
            rs.getInt   ("capacity"),
            rs.getString("status"),
            rs.getString("created_at"),
            rs.getInt   ("booked_seats")
        );
    }

    // Updates the status field with an optional "must currently be" check
    private boolean updateStatus(int id, String newStatus, String requiredCurrentStatus) {
        String sql = requiredCurrentStatus == null
            ? "UPDATE Events SET status = ? WHERE event_id = ?"
            : "UPDATE Events SET status = ? WHERE event_id = ? AND status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt   (2, id);
            if (requiredCurrentStatus != null) {
                ps.setString(3, requiredCurrentStatus);
            }

            int rows = ps.executeUpdate();
            if (rows == 0 && requiredCurrentStatus != null) {
                System.out.println("  Event not found or not in '" + requiredCurrentStatus + "' status.");
            }
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("  Error updating event status: " + e.getMessage());
            return false;
        }
    }
}
