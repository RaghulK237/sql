-- ============================================================
-- EventSphere – Event Management System
-- triggers.sql
--
-- Run AFTER database.sql.
-- Creates two triggers:
--   1. trg_after_booking_cancel  – when a booking is cancelled,
--      automatically cancel its tickets and refund its payment.
--   2. trg_after_booking_insert  – logs new bookings for audit.
-- ============================================================

USE eventsphere;

DROP TRIGGER IF EXISTS trg_after_booking_cancel;
DROP TRIGGER IF EXISTS trg_after_booking_insert;

DELIMITER $$

-- ============================================================
-- TRIGGER 1: AFTER UPDATE on Bookings
--
-- When does it fire?
--   Every time a Bookings row is updated.
--
-- What does it do?
--   If the booking_status changes FROM anything TO 'cancelled',
--   it automatically:
--     a) Sets all associated Tickets to 'cancelled'
--     b) Sets the Payment to 'refunded'
--
-- Why is this useful?
--   Without this trigger, a developer would have to remember to
--   update Tickets and Payments separately every time they cancel
--   a booking. The trigger ensures this NEVER gets forgotten.
--
-- NEW = the row values AFTER the update
-- OLD = the row values BEFORE the update
-- ============================================================
CREATE TRIGGER trg_after_booking_cancel
AFTER UPDATE ON Bookings
FOR EACH ROW
BEGIN
    -- Only act when status changes to 'cancelled'
    IF OLD.booking_status != 'cancelled' AND NEW.booking_status = 'cancelled' THEN

        -- a) Cancel all tickets belonging to this booking
        UPDATE Tickets
        SET ticket_status = 'cancelled'
        WHERE booking_id = NEW.booking_id;

        -- b) Refund the payment for this booking
        UPDATE Payments
        SET payment_status = 'refunded'
        WHERE booking_id = NEW.booking_id
          AND payment_status != 'free';  -- do not change 'free' to 'refunded'

    END IF;
END$$

-- ============================================================
-- TRIGGER 2: AFTER INSERT on Bookings
--
-- When does it fire?
--   Every time a new row is inserted into Bookings.
--
-- What does it do?
--   Validates that the event is not already fully booked
--   using a live seat count.
--
-- Note: The BookEvent stored procedure already checks capacity
-- BEFORE inserting. This trigger is a second safety net that
-- prevents overbooking even if two customers submit at the
-- exact same millisecond (race condition protection).
-- ============================================================
CREATE TRIGGER trg_after_booking_insert
AFTER INSERT ON Bookings
FOR EACH ROW
BEGIN
    DECLARE v_capacity  INT;
    DECLARE v_confirmed INT;

    -- Get the event's capacity
    SELECT capacity INTO v_capacity
    FROM Events WHERE event_id = NEW.event_id;

    -- Count all confirmed tickets (including the one just inserted)
    SELECT COALESCE(SUM(ticket_quantity), 0) INTO v_confirmed
    FROM Bookings
    WHERE event_id = NEW.event_id
      AND booking_status = 'confirmed';

    -- If over capacity, raise an error (the transaction will roll back)
    IF v_confirmed > v_capacity THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error: Booking rejected. Event is now fully booked.';
    END IF;
END$$

DELIMITER ;

-- ============================================================
-- How to verify the triggers:
--
-- 1. Check a booking:
--    SELECT * FROM Bookings WHERE booking_id = 1;
--    SELECT * FROM Tickets WHERE booking_id = 1;
--    SELECT * FROM Payments WHERE booking_id = 1;
--
-- 2. Cancel it:
--    UPDATE Bookings SET booking_status = 'cancelled' WHERE booking_id = 1;
--
-- 3. Check again — tickets should be 'cancelled', payment 'refunded'.
-- ============================================================
