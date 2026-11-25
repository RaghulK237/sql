-- ============================================================
-- EventSphere – Event Management System
-- sample_data.sql
--
-- Run LAST (after database.sql, procedures.sql, triggers.sql, indexes.sql).
-- Inserts test data so you can run the application immediately.
--
-- DEFAULT CREDENTIALS:
--   Admin   : admin@eventsphere.com  / Admin@123
--   Customer: arjun@example.com      / Pass@123
--   Customer: priya@example.com      / Pass@123
--   (All sample customers share the same password for easy testing)
--
-- IMPORTANT: Passwords are stored as SHA-256 hashes.
-- MySQL's SHA2('text', 256) produces the same hash as
-- Java's MessageDigest.getInstance("SHA-256").
-- ============================================================

USE eventsphere;

-- ============================================================
-- USERS
-- ============================================================

-- Admin account
-- SHA2('Admin@123', 256) is the hashed form of "Admin@123"
INSERT INTO Users (name, email, password_hash, phone, role) VALUES
('Admin User', 'admin@eventsphere.com',
 SHA2('Admin@123', 256), '9000000000', 'admin');

-- Sample customer accounts (password: Pass@123)
INSERT INTO Users (name, email, password_hash, phone, role) VALUES
('Arjun Sharma', 'arjun@example.com',  SHA2('Pass@123', 256), '9876543210', 'customer'),
('Priya Mehta',  'priya@example.com',  SHA2('Pass@123', 256), '9812345678', 'customer'),
('Rohan Das',    'rohan@example.com',  SHA2('Pass@123', 256), '9898765432', 'customer'),
('Sneha Patel',  'sneha@example.com',  SHA2('Pass@123', 256), '9765432109', 'customer'),
('Karan Verma',  'karan@example.com',  SHA2('Pass@123', 256), '9654321098', 'customer');

-- ============================================================
-- VENUES
-- ============================================================
INSERT INTO Venues (venue_name, location, capacity) VALUES
('City Convention Hall',  'Mumbai, Maharashtra',  500),
('Tech Park Auditorium',  'Bengaluru, Karnataka', 300),
('Green Lawns Ground',    'Delhi, NCR',          1000),
('College Seminar Hall',  'Pune, Maharashtra',    150),
('Grand Ballroom',        'Hyderabad, Telangana', 800);

-- ============================================================
-- CATEGORIES
-- ============================================================
INSERT INTO Categories (category_name) VALUES
('Technology'),
('Music'),
('Sports'),
('Workshop'),
('Cultural');

-- ============================================================
-- EVENTS
-- Mix of published and draft events.
-- Dates are set in the future (2027) so they always appear
-- as "upcoming" when you test the application.
-- ============================================================
INSERT INTO Events
    (event_name, description, category_id, venue_id,
     event_date, start_time, end_time, ticket_price, capacity, status)
VALUES
-- Published events (visible to customers)
('AI Summit 2027',
 'A one-day summit on Artificial Intelligence and Machine Learning trends.',
 1, 2, '2027-08-15', '09:00:00', '18:00:00', 499.00, 200, 'published'),

('Rock Night Live',
 'Live rock music concert featuring 5 popular indie bands.',
 2, 1, '2027-09-20', '18:00:00', '23:00:00', 799.00, 300, 'published'),

('Annual Sports Meet',
 'Inter-college sports competition with athletics, cricket, and basketball.',
 3, 3, '2027-10-05', '08:00:00', '17:00:00', 0.00, 500, 'published'),

('Python for Beginners Workshop',
 'Hands-on workshop to learn Python programming from scratch.',
 4, 4, '2027-07-30', '10:00:00', '16:00:00', 299.00, 100, 'published'),

('Diwali Cultural Festival',
 'Traditional dance performances, music, food stalls, and fireworks.',
 5, 1, '2027-10-20', '15:00:00', '22:00:00', 199.00, 400, 'published'),

-- Draft event (only admin can see it; customers cannot book)
('Cloud Computing Bootcamp',
 'Two-day intensive bootcamp covering AWS, Azure, and GCP basics.',
 1, 5, '2027-11-10', '09:00:00', '17:00:00', 999.00, 250, 'draft');

-- ============================================================
-- BOOKINGS, TICKETS, and PAYMENTS
-- We insert these directly (bypassing the procedure) so we
-- can insert historical sample data with past dates.
-- ============================================================

-- Booking 1: Arjun books 2 tickets for AI Summit
INSERT INTO Bookings (user_id, event_id, booking_date, ticket_quantity, total_amount, booking_status)
VALUES (2, 1, '2027-07-01 10:00:00', 2, 998.00, 'confirmed');

INSERT INTO Tickets (booking_id, ticket_code, ticket_status) VALUES
(1, 'TKT-001-0001-01', 'valid'),
(1, 'TKT-001-0001-02', 'valid');

INSERT INTO Payments (booking_id, amount, payment_method, payment_status, payment_date)
VALUES (1, 998.00, 'online', 'paid', '2027-07-01 10:01:00');

-- Booking 2: Priya books 1 ticket for AI Summit
INSERT INTO Bookings (user_id, event_id, booking_date, ticket_quantity, total_amount, booking_status)
VALUES (3, 1, '2027-07-02 11:00:00', 1, 499.00, 'confirmed');

INSERT INTO Tickets (booking_id, ticket_code, ticket_status) VALUES
(2, 'TKT-001-0002-01', 'valid');

INSERT INTO Payments (booking_id, amount, payment_method, payment_status, payment_date)
VALUES (2, 499.00, 'online', 'paid', '2027-07-02 11:01:00');

-- Booking 3: Rohan books 1 ticket for Rock Night
INSERT INTO Bookings (user_id, event_id, booking_date, ticket_quantity, total_amount, booking_status)
VALUES (4, 2, '2027-07-03 14:00:00', 1, 799.00, 'confirmed');

INSERT INTO Tickets (booking_id, ticket_code, ticket_status) VALUES
(3, 'TKT-002-0003-01', 'valid');

INSERT INTO Payments (booking_id, amount, payment_method, payment_status, payment_date)
VALUES (3, 799.00, 'online', 'paid', '2027-07-03 14:01:00');

-- Booking 4: Sneha books 3 FREE tickets for Sports Meet
INSERT INTO Bookings (user_id, event_id, booking_date, ticket_quantity, total_amount, booking_status)
VALUES (5, 3, '2027-07-04 09:00:00', 3, 0.00, 'confirmed');

INSERT INTO Tickets (booking_id, ticket_code, ticket_status) VALUES
(4, 'TKT-003-0004-01', 'valid'),
(4, 'TKT-003-0004-02', 'valid'),
(4, 'TKT-003-0004-03', 'valid');

INSERT INTO Payments (booking_id, amount, payment_method, payment_status, payment_date)
VALUES (4, 0.00, 'free', 'free', '2027-07-04 09:01:00');

-- Booking 5: Arjun books 1 ticket for Python Workshop
INSERT INTO Bookings (user_id, event_id, booking_date, ticket_quantity, total_amount, booking_status)
VALUES (2, 4, '2027-07-05 16:00:00', 1, 299.00, 'confirmed');

INSERT INTO Tickets (booking_id, ticket_code, ticket_status) VALUES
(5, 'TKT-004-0005-01', 'valid');

INSERT INTO Payments (booking_id, amount, payment_method, payment_status, payment_date)
VALUES (5, 299.00, 'online', 'paid', '2027-07-05 16:01:00');

-- Booking 6: Karan books 2 tickets for Diwali Fest — then CANCELLED
INSERT INTO Bookings (user_id, event_id, booking_date, ticket_quantity, total_amount, booking_status)
VALUES (6, 5, '2027-07-06 12:00:00', 2, 398.00, 'cancelled');

INSERT INTO Tickets (booking_id, ticket_code, ticket_status) VALUES
(6, 'TKT-005-0006-01', 'cancelled'),
(6, 'TKT-005-0006-02', 'cancelled');

INSERT INTO Payments (booking_id, amount, payment_method, payment_status, payment_date)
VALUES (6, 398.00, 'online', 'refunded', '2027-07-06 12:01:00');

-- ============================================================
-- SAMPLE AUDIT LOG ENTRIES
-- ============================================================
INSERT INTO AuditLogs (user_id, action, details, created_at) VALUES
(2, 'LOGIN',        'Customer Arjun logged in',                          '2027-07-01 09:55:00'),
(2, 'BOOK_EVENT',   'Booking #1 for event "AI Summit 2027" | Qty: 2',    '2027-07-01 10:01:00'),
(3, 'LOGIN',        'Customer Priya logged in',                          '2027-07-02 10:50:00'),
(3, 'BOOK_EVENT',   'Booking #2 for event "AI Summit 2027" | Qty: 1',    '2027-07-02 11:01:00'),
(6, 'BOOK_EVENT',   'Booking #6 for event "Diwali Cultural Festival"',   '2027-07-06 12:01:00'),
(6, 'CANCEL_BOOKING','Booking #6 cancelled by customer',                 '2027-07-07 09:00:00');

-- ============================================================
-- VERIFICATION QUERIES
-- Run these in MySQL Workbench to confirm data loaded correctly.
-- ============================================================

-- Check user count and roles
-- SELECT role, COUNT(*) FROM Users GROUP BY role;

-- Check event statuses
-- SELECT status, COUNT(*) FROM Events GROUP BY status;

-- Check available seats for each published event
-- SELECT e.event_name, e.capacity,
--        COALESCE(SUM(CASE WHEN b.booking_status='confirmed' THEN b.ticket_quantity ELSE 0 END), 0) AS booked,
--        e.capacity - COALESCE(SUM(CASE WHEN b.booking_status='confirmed' THEN b.ticket_quantity ELSE 0 END), 0) AS available
-- FROM Events e
-- LEFT JOIN Bookings b ON e.event_id = b.event_id
-- WHERE e.status = 'published'
-- GROUP BY e.event_id, e.event_name, e.capacity;
