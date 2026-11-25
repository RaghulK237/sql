-- ============================================================
-- EventSphere – Event Management System
-- database.sql  (v2 – complete rewrite)
--
-- Run ORDER:
--   1. database.sql      ← this file
--   2. procedures.sql
--   3. triggers.sql
--   4. indexes.sql
--   5. sample_data.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS eventsphere
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE eventsphere;

-- ============================================================
-- Drop all tables in reverse FK dependency order
-- (safe to re-run)
-- ============================================================
DROP TABLE IF EXISTS AuditLogs;
DROP TABLE IF EXISTS Payments;
DROP TABLE IF EXISTS Tickets;
DROP TABLE IF EXISTS Bookings;
DROP TABLE IF EXISTS Events;
DROP TABLE IF EXISTS Venues;
DROP TABLE IF EXISTS Categories;
DROP TABLE IF EXISTS Users;

-- ============================================================
-- TABLE 1: Users
-- Stores both admin and customer accounts.
-- role = 'admin' or 'customer'
-- password_hash = SHA-256 hex string (64 chars)
-- ============================================================
CREATE TABLE Users (
    user_id       INT           AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)   NOT NULL,          -- SHA-256 produces 64 hex chars
    phone         VARCHAR(15),
    role          VARCHAR(10)   NOT NULL DEFAULT 'customer'
                                CHECK (role IN ('admin', 'customer')),
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE 2: Venues
-- Stores information about event locations.
-- ============================================================
CREATE TABLE Venues (
    venue_id   INT           AUTO_INCREMENT PRIMARY KEY,
    venue_name VARCHAR(150)  NOT NULL,
    location   VARCHAR(200)  NOT NULL,
    capacity   INT           NOT NULL CHECK (capacity > 0)
);

-- ============================================================
-- TABLE 3: Categories
-- Stores event category names (Technology, Music, etc.)
-- ============================================================
CREATE TABLE Categories (
    category_id   INT          AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE
);

-- ============================================================
-- TABLE 4: Events
-- Core event table.
-- status: 'draft' → admin created but not visible to customers
--         'published' → visible and bookable by customers
--         'cancelled' → no new bookings allowed
--
-- NOTE: There is NO registered_count column.
-- Available seats are always calculated LIVE from Bookings:
--   capacity - SUM(ticket_quantity WHERE booking_status='confirmed')
-- This is accurate for multi-ticket bookings and cancellations.
-- ============================================================
CREATE TABLE Events (
    event_id     INT            AUTO_INCREMENT PRIMARY KEY,
    event_name   VARCHAR(200)   NOT NULL,
    description  TEXT,
    category_id  INT            NOT NULL,
    venue_id     INT            NOT NULL,
    event_date   DATE           NOT NULL,
    start_time   TIME           NOT NULL,
    end_time     TIME           NOT NULL,
    ticket_price DECIMAL(10,2)  NOT NULL DEFAULT 0.00
                                CHECK (ticket_price >= 0),
    capacity     INT            NOT NULL CHECK (capacity > 0),
    status       VARCHAR(15)    NOT NULL DEFAULT 'draft'
                                CHECK (status IN ('draft', 'published', 'cancelled')),
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_category FOREIGN KEY (category_id) REFERENCES Categories(category_id),
    CONSTRAINT fk_event_venue    FOREIGN KEY (venue_id)    REFERENCES Venues(venue_id),
    CONSTRAINT chk_event_time    CHECK (end_time > start_time)
);

-- ============================================================
-- TABLE 5: Bookings
-- One booking = one customer's purchase for one event.
-- A booking may cover multiple tickets (ticket_quantity > 1).
-- total_amount = ticket_price × ticket_quantity (calculated at time of booking)
-- booking_status: 'confirmed' or 'cancelled'
-- ============================================================
CREATE TABLE Bookings (
    booking_id     INT            AUTO_INCREMENT PRIMARY KEY,
    user_id        INT            NOT NULL,
    event_id       INT            NOT NULL,
    booking_date   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ticket_quantity INT           NOT NULL CHECK (ticket_quantity > 0),
    total_amount   DECIMAL(10,2)  NOT NULL CHECK (total_amount >= 0),
    booking_status VARCHAR(15)    NOT NULL DEFAULT 'confirmed'
                                  CHECK (booking_status IN ('confirmed', 'cancelled')),

    CONSTRAINT fk_booking_user  FOREIGN KEY (user_id)  REFERENCES Users(user_id),
    CONSTRAINT fk_booking_event FOREIGN KEY (event_id) REFERENCES Events(event_id)
);

-- ============================================================
-- TABLE 6: Tickets
-- One row per individual ticket within a booking.
-- ticket_code is unique (e.g. TKT-001-0002-01)
-- ticket_status: 'valid' or 'cancelled'
-- ============================================================
CREATE TABLE Tickets (
    ticket_id     INT          AUTO_INCREMENT PRIMARY KEY,
    booking_id    INT          NOT NULL,
    ticket_code   VARCHAR(30)  NOT NULL UNIQUE,
    ticket_status VARCHAR(15)  NOT NULL DEFAULT 'valid'
                               CHECK (ticket_status IN ('valid', 'cancelled')),

    CONSTRAINT fk_ticket_booking FOREIGN KEY (booking_id) REFERENCES Bookings(booking_id)
);

-- ============================================================
-- TABLE 7: Payments
-- One payment record per booking.
-- payment_status: 'paid', 'free', 'refunded'
-- payment_method: 'online' or 'free' (no real gateway used)
-- ============================================================
CREATE TABLE Payments (
    payment_id     INT            AUTO_INCREMENT PRIMARY KEY,
    booking_id     INT            NOT NULL UNIQUE,   -- one payment per booking
    amount         DECIMAL(10,2)  NOT NULL CHECK (amount >= 0),
    payment_method VARCHAR(20)    NOT NULL DEFAULT 'online'
                                  CHECK (payment_method IN ('online', 'free')),
    payment_status VARCHAR(15)    NOT NULL DEFAULT 'paid'
                                  CHECK (payment_status IN ('paid', 'free', 'refunded')),
    payment_date   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES Bookings(booking_id)
);

-- ============================================================
-- TABLE 8: AuditLogs
-- Tracks important user actions for security and audit trails.
-- Examples: LOGIN, LOGOUT, BOOK_EVENT, CANCEL_BOOKING
-- ============================================================
CREATE TABLE AuditLogs (
    log_id     INT           AUTO_INCREMENT PRIMARY KEY,
    user_id    INT,                                   -- NULL if action before login
    action     VARCHAR(50)   NOT NULL,
    details    VARCHAR(500),
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES Users(user_id)
                           ON DELETE SET NULL         -- keep logs even if user deleted
);
