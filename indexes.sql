-- ============================================================
-- EventSphere – Event Management System
-- indexes.sql
--
-- Run AFTER database.sql.
-- Creates indexes to speed up common queries.
--
-- WHY INDEXES MATTER:
-- Without an index, MySQL reads EVERY row in the table
-- to find matching records. This is called a "full table scan".
-- With an index, MySQL jumps directly to matching rows —
-- just like the index at the back of a textbook.
--
-- Downside: Indexes slow down INSERT/UPDATE slightly
-- (MySQL must update both the data and the index).
-- So we only create indexes on columns that are frequently
-- searched or filtered.
-- ============================================================

USE eventsphere;

-- ============================================================
-- Events table indexes
-- ============================================================

-- Search events by name keyword (used in LIKE '%keyword%' queries)
CREATE INDEX idx_event_name   ON Events(event_name);

-- Filter events by date (used in "upcoming events" and "filter by date")
CREATE INDEX idx_event_date   ON Events(event_date);

-- Filter events by status (used constantly: WHERE status = 'published')
CREATE INDEX idx_event_status ON Events(status);

-- Filter events by category (used in "filter by category")
CREATE INDEX idx_event_cat    ON Events(category_id);

-- ============================================================
-- Bookings table indexes
-- ============================================================

-- Find all bookings for a specific user (customer "My Bookings")
CREATE INDEX idx_booking_user  ON Bookings(user_id);

-- Find all bookings for a specific event (admin view)
CREATE INDEX idx_booking_event ON Bookings(event_id);

-- Filter by booking status (count confirmed bookings for capacity)
CREATE INDEX idx_booking_status ON Bookings(booking_status);

-- ============================================================
-- Tickets table indexes
-- ============================================================

-- Find all tickets for a booking (admin/customer ticket view)
CREATE INDEX idx_ticket_booking ON Tickets(booking_id);

-- Search ticket by its unique code (for validation at venue)
-- Note: ticket_code already has a UNIQUE constraint which
-- creates an implicit index, so no extra index is needed.

-- ============================================================
-- AuditLogs table indexes
-- ============================================================

-- Find all actions by a specific user (admin audit view)
CREATE INDEX idx_log_user   ON AuditLogs(user_id);

-- Filter logs by action type (e.g. show all LOGIN events)
CREATE INDEX idx_log_action ON AuditLogs(action);
