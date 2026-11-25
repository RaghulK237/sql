// ============================================================
// AuthService.java
// Handles user registration, login, and password hashing.
//
// IMPORTANT -- How password hashing works:
//   1. When a user registers, we hash their password with SHA-256
//      and store ONLY the hash in the database.
//   2. The original password is NEVER stored anywhere.
//   3. When they log in, we hash the entered password again and
//      compare the two hashes. If they match -> login succeeds.
//   4. SHA-256 is a one-way function -- you cannot un-hash it.
//   5. Java's MessageDigest produces the same output as MySQL's
//      SHA2('password', 256), so the hashes are compatible.
// ============================================================

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class AuthService {

    // ----------------------------------------------------------
    // hashPassword()
    // Converts a plain-text password into a 64-character hex string.
    // Example: "Pass@123" -> "a3f1b2c4..." (64 hex chars)
    // ----------------------------------------------------------
    public static String hashPassword(String plainPassword) {
        try {
            // MessageDigest is Java's built-in hashing class
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Convert password string to bytes, then hash it
            byte[] hashBytes = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array to a hex string (0-9, a-f)
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));  // %02x = 2-digit hex
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in standard Java -- this should never happen
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // ----------------------------------------------------------
    // register()
    // Inserts a new CUSTOMER user into the database.
    // Admins are created only via sample_data.sql -- customers
    // CANNOT choose the 'admin' role during registration.
    //
    // Returns: true if registration succeeded, false otherwise.
    // ----------------------------------------------------------
    public boolean register(String name, String email, String password, String phone) {

        // Basic validation
        if (name == null || name.trim().isEmpty()) {
            System.out.println("  Error: Name cannot be empty.");
            return false;
        }
        if (email == null || !email.contains("@")) {
            System.out.println("  Error: Please enter a valid email address.");
            return false;
        }
        if (password == null || password.length() < 6) {
            System.out.println("  Error: Password must be at least 6 characters.");
            return false;
        }

        String sql = "INSERT INTO Users (name, email, password_hash, phone, role) " +
                     "VALUES (?, ?, ?, ?, 'customer')";  // role is ALWAYS 'customer' here

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email.toLowerCase().trim());  // store emails in lowercase
            ps.setString(3, hashPassword(password));
            ps.setString(4, phone);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            // Error code 1062 = Duplicate entry (email already exists)
            if (e.getErrorCode() == 1062) {
                System.out.println("  Error: An account with this email already exists.");
            } else {
                System.out.println("  Error registering user: " + e.getMessage());
            }
            return false;
        }
    }

    // ----------------------------------------------------------
    // login()
    // Checks email + password. Returns a User object on success,
    // or null if credentials are wrong.
    // ----------------------------------------------------------
    public User login(String email, String password) {

        String sql = "SELECT user_id, name, email, phone, role " +
                     "FROM Users " +
                     "WHERE email = ? AND password_hash = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.toLowerCase().trim());
            ps.setString(2, hashPassword(password));  // hash the entered password

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Credentials matched -- return the user object
                    User user = new User(
                        rs.getInt   ("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role")
                    );
                    // Log the login action
                    logAction(user.getUserId(), "LOGIN",
                              user.getRole() + " " + user.getName() + " logged in");
                    return user;
                }
            }

        } catch (SQLException e) {
            System.out.println("  Error during login: " + e.getMessage());
        }

        return null;  // login failed
    }

    // ----------------------------------------------------------
    // logAction()
    // Inserts a row into AuditLogs to record what a user did.
    // Called automatically on login, logout, booking, cancellation.
    // ----------------------------------------------------------
    public static void logAction(int userId, String action, String details) {
        String sql = "INSERT INTO AuditLogs (user_id, action, details, created_at) " +
                     "VALUES (?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt   (1, userId);
            ps.setString(2, action);
            ps.setString(3, details != null && details.length() > 490
                            ? details.substring(0, 490) : details);
            ps.executeUpdate();

        } catch (SQLException e) {
            // Audit logging failure should not crash the app
            System.out.println("  Warning: Could not write to audit log.");
        }
    }
}
