-- ============================================================
-- EventSphere – Event Management System
-- procedures.sql
--
-- Run AFTER database.sql.
-- Creates the stored procedure: BookEvent
--
-- BookEvent wraps three inserts (Booking + Tickets + Payment)
-- in a single transaction. If any insert fails, EVERYTHING
-- is rolled back — no partial data is ever saved.
-- ============================================================

USE eventsphere;

DROP PROCEDURE IF EXISTS BookEvent;

DELIMITER $$

-- ============================================================
-- PROCEDURE: BookEvent
-- Parameters:
--   p_user_id  – ID of the customer making the booking
--   p_event_id – ID of the event being booked
--   p_quantity – number of tickets requested
--
-- What it does (in order):
--   1. Validates inputs
--   2. Checks the event is published and in the future
--   3. Calculates live available seats (no stale counter)
--   4. Checks for enough seats
--   5. Starts a transaction
--   6. Inserts one Booking row
--   7. Loops and inserts one Ticket row per ticket
--   8. Inserts one Payment row
--   9. Commits the transaction
--  10. Returns booking summary
-- ============================================================
CREATE PROCEDURE BookEvent(
    IN p_user_id   INT,
    IN p_event_id  INT,
    IN p_quantity  INT
)
BEGIN
    -- ── Variables ───────────────────────────────────────────
    DECLARE v_event_status  VARCHAR(15);
    DECLARE v_event_date    DATE;
    DECLARE v_event_name    VARCHAR(200);
    DECLARE v_capacity      INT;
    DECLARE v_confirmed     INT DEFAULT 0;
    DECLARE v_available     INT;
    DECLARE v_ticket_price  DECIMAL(10,2);
    DECLARE v_total_amount  DECIMAL(10,2);
    DECLARE v_booking_id    INT;
    DECLARE v_ticket_num    INT DEFAULT 1;
    DECLARE v_ticket_code   VARCHAR(30);
    DECLARE v_pay_method    VARCHAR(20);
    DECLARE v_pay_status    VARCHAR(15);
    DECLARE v_user_role     VARCHAR(10);

    -- ── Error handler: roll back on ANY SQL error ────────────
    -- EXIT HANDLER fires immediately on error, rolls back,
    -- then re-throws the error so Java can display it.
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    -- ── Check 1: quantity must be at least 1 ────────────────
    IF p_quantity <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error: Ticket quantity must be 1 or more.';
    END IF;

    -- ── Check 2: user must exist and be a customer ───────────
    SELECT role INTO v_user_role
    FROM Users WHERE user_id = p_user_id;

    IF v_user_role IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error: User not found.';
    END IF;

    IF v_user_role != 'customer' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error: Only customers can book tickets.';
    END IF;

    -- ── Check 3: event must exist, be published, be upcoming ─
    SELECT event_name, status, event_date, capacity, ticket_price
    INTO v_event_name, v_event_status, v_event_date, v_capacity, v_ticket_price
    FROM Events WHERE event_id = p_event_id;

    IF v_event_name IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error: Event not found.';
    END IF;

    IF v_event_status != 'published' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error: This event is not available for booking.';
    END IF;

    IF v_event_date < CURDATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error: This event has already passed.';
    END IF;

    -- ── Check 4: available seats (calculated LIVE, not from a counter) ──
    -- We sum ticket_quantity from all CONFIRMED bookings.
    -- This is always accurate — no trigger can make it wrong.
    SELECT COALESCE(SUM(ticket_quantity), 0) INTO v_confirmed
    FROM Bookings
    WHERE event_id = p_event_id AND booking_status = 'confirmed';

    SET v_available = v_capacity - v_confirmed;

    IF v_available < p_quantity THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error: Not enough seats available. Please reduce the quantity.';
    END IF;

    -- ── All checks passed. Start the transaction. ────────────
    START TRANSACTION;

    -- Calculate total amount
    SET v_total_amount = v_ticket_price * p_quantity;

    -- ── Step 1: Insert the Booking ───────────────────────────
    INSERT INTO Bookings (user_id, event_id, booking_date, ticket_quantity, total_amount, booking_status)
    VALUES (p_user_id, p_event_id, NOW(), p_quantity, v_total_amount, 'confirmed');

    SET v_booking_id = LAST_INSERT_ID();

    -- ── Step 2: Insert one Ticket row per ticket ─────────────
    -- Ticket code format: TKT-{eventId 3 digits}-{bookingId 4 digits}-{ticketNum 2 digits}
    -- Example: TKT-001-0003-02 = Event 1, Booking 3, Ticket 2
    WHILE v_ticket_num <= p_quantity DO
        SET v_ticket_code = CONCAT(
            'TKT-',
            LPAD(p_event_id,   3, '0'), '-',
            LPAD(v_booking_id, 4, '0'), '-',
            LPAD(v_ticket_num, 2, '0')
        );

        INSERT INTO Tickets (booking_id, ticket_code, ticket_status)
        VALUES (v_booking_id, v_ticket_code, 'valid');

        SET v_ticket_num = v_ticket_num + 1;
    END WHILE;

    -- ── Step 3: Insert the Payment ───────────────────────────
    IF v_ticket_price = 0 THEN
        -- Free event: no payment needed
        SET v_pay_method = 'free';
        SET v_pay_status = 'free';
    ELSE
        -- Paid event: simulate an online payment
        SET v_pay_method = 'online';
        SET v_pay_status = 'paid';
    END IF;

    INSERT INTO Payments (booking_id, amount, payment_method, payment_status, payment_date)
    VALUES (v_booking_id, v_total_amount, v_pay_method, v_pay_status, NOW());

    -- ── Log the booking action ────────────────────────────────
    INSERT INTO AuditLogs (user_id, action, details, created_at)
    VALUES (p_user_id, 'BOOK_EVENT',
            CONCAT('Booking #', v_booking_id, ' for event "', v_event_name,
                   '" | Qty: ', p_quantity, ' | Total: Rs.', v_total_amount),
            NOW());

    -- ── Commit everything ─────────────────────────────────────
    COMMIT;

    -- ── Return a result row for Java to read ──────────────────
    SELECT
        v_booking_id    AS booking_id,
        p_quantity      AS ticket_count,
        v_total_amount  AS total_amount,
        v_pay_status    AS payment_status;

END$$

DELIMITER ;

-- ============================================================
-- How to call from MySQL Workbench (for testing):
--   CALL BookEvent(2, 1, 2);
--   This books 2 tickets for user_id=2 on event_id=1.
-- ============================================================
