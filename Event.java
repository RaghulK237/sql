// ============================================================
// Event.java  (v2 -- complete rewrite)
// Data-holder class for one event row from the database.
// Fields now match the v2 Events table (status, start_time,
// end_time, venueName, categoryName, bookedSeats).
// ============================================================

public class Event {

    // -- Fields that come directly from the Events table ----------
    private int    eventId;
    private String eventName;
    private String description;
    private int    categoryId;
    private String categoryName;    // joined from Categories
    private int    venueId;
    private String venueName;       // joined from Venues
    private String eventDate;       // yyyy-MM-dd
    private String startTime;       // HH:mm:ss
    private String endTime;         // HH:mm:ss
    private double ticketPrice;
    private int    capacity;
    private String status;          // draft / published / cancelled
    private String createdAt;

    // -- Calculated field (not a column, computed live by SQL) ----
    private int    bookedSeats;     // SUM of confirmed ticket_quantity

    // -- Constructor ----------------------------------------------
    public Event(int eventId, String eventName, String description,
                 int categoryId, String categoryName,
                 int venueId, String venueName,
                 String eventDate, String startTime, String endTime,
                 double ticketPrice, int capacity, String status,
                 String createdAt, int bookedSeats) {
        this.eventId      = eventId;
        this.eventName    = eventName;
        this.description  = description;
        this.categoryId   = categoryId;
        this.categoryName = categoryName;
        this.venueId      = venueId;
        this.venueName    = venueName;
        this.eventDate    = eventDate;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.ticketPrice  = ticketPrice;
        this.capacity     = capacity;
        this.status       = status;
        this.createdAt    = createdAt;
        this.bookedSeats  = bookedSeats;
    }

    // -- Getters --------------------------------------------------
    public int    getEventId()      { return eventId; }
    public String getEventName()    { return eventName; }
    public String getDescription()  { return description; }
    public int    getCategoryId()   { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public int    getVenueId()      { return venueId; }
    public String getVenueName()    { return venueName; }
    public String getEventDate()    { return eventDate; }
    public String getStartTime()    { return startTime; }
    public String getEndTime()      { return endTime; }
    public double getTicketPrice()  { return ticketPrice; }
    public int    getCapacity()     { return capacity; }
    public String getStatus()       { return status; }
    public String getCreatedAt()    { return createdAt; }
    public int    getBookedSeats()  { return bookedSeats; }

    // Convenience: available seats is always derived, never stored
    public int getAvailableSeats()  { return capacity - bookedSeats; }

    // -- Setters (for updateEvent in EventDAO) ---------------------
    public void setEventName(String v)   { this.eventName   = v; }
    public void setDescription(String v) { this.description = v; }
    public void setEventDate(String v)   { this.eventDate   = v; }
    public void setStartTime(String v)   { this.startTime   = v; }
    public void setEndTime(String v)     { this.endTime     = v; }
    public void setCapacity(int v)       { this.capacity    = v; }
    public void setTicketPrice(double v) { this.ticketPrice = v; }
    public void setStatus(String v)      { this.status      = v; }

    // -- toString: compact one-line summary (for list views) -------
    public String toListRow() {
        String price = ticketPrice == 0 ? "FREE" : String.format("Rs.%.0f", ticketPrice);
        return String.format("  [%3d] %-28s | %-12s | %-22s | %-10s | %s | Seats: %d/%d",
            eventId, eventName, eventDate, venueName, categoryName, price,
            getAvailableSeats(), capacity);
    }

    // -- toString: full detail block (for View Event Details) ------
    @Override
    public String toString() {
        String price = ticketPrice == 0 ? "FREE" : String.format("Rs. %.2f", ticketPrice);
        return String.format(
            "\n  %-16s: %d"         +
            "\n  %-16s: %s"         +
            "\n  %-16s: %s"         +
            "\n  %-16s: %s"         +
            "\n  %-16s: %s"         +
            "\n  %-16s: %s  to  %s" +
            "\n  %-16s: %s"         +
            "\n  %-16s: %d  |  Booked: %d  |  Available: %d" +
            "\n  %-16s: %s"         +
            "\n  %-16s: %s",
            "Event ID",    eventId,
            "Name",        eventName,
            "Category",    categoryName,
            "Venue",       venueName,
            "Date",        eventDate,
            "Time",        startTime, endTime,
            "Ticket Price",price,
            "Capacity",    capacity, bookedSeats, getAvailableSeats(),
            "Status",      status.toUpperCase(),
            "Description", description
        );
    }
}
