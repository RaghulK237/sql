// ============================================================
// Main.java  (v2)
// Entry point of the EventSphere application.
// Shows the startup menu: Login / Register / Exit.
// After login, routes the user to the correct panel:
//   Admin    -> AdminPanel
//   Customer -> CustomerPanel
// ============================================================

import java.sql.Connection;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Print welcome banner
        System.out.println();
        System.out.println("  +----------------------------------------------+");
        System.out.println("  |     EventSphere - Event Management System    |");
        System.out.println("  |            Powered by Java + MySQL           |");
        System.out.println("  +----------------------------------------------+");

        // Test DB connection before showing any menu
        Connection testConn = DBConnection.getConnection();
        if (testConn == null) {
            System.out.println("\n  ERROR: Cannot connect to MySQL.");
            System.out.println("  Check: MySQL is running, password in DBConnection.java is correct.");
            return;
        }
        // Close the test connection immediately
        try { testConn.close(); } catch (Exception ignored) {}
        System.out.println("  Database connected successfully!\n");

        // One Scanner shared across the whole application
        Scanner sc = new Scanner(System.in);
        AuthService authService = new AuthService();

        boolean appRunning = true;
        while (appRunning) {
            printStartMenu();
            int choice = readInt(sc, "  Choice: ");

            switch (choice) {

                // -- Login ------------------------------------------
                case 1:
                    User user = doLogin(sc, authService);
                    if (user != null) {
                        if (user.isAdmin()) {
                            new AdminPanel(user, sc).show();
                        } else {
                            new CustomerPanel(user, sc).show();
                        }
                    }
                    break;

                // -- Register (customer only) -----------------------
                case 2:
                    doRegister(sc, authService);
                    break;

                // -- Exit -------------------------------------------
                case 3:
                    System.out.println("\n  Thank you for using EventSphere. Goodbye!\n");
                    appRunning = false;
                    break;

                default:
                    System.out.println("  Please enter 1, 2, or 3.");
            }
        }
        sc.close();
    }

    // ----------------------------------------------------------
    // Login flow
    // ----------------------------------------------------------
    private static User doLogin(Scanner sc, AuthService auth) {
        System.out.println("\n  --- LOGIN ---");
        System.out.print("  Email   : "); String email    = sc.nextLine().trim();
        System.out.print("  Password: "); String password = sc.nextLine().trim();

        User user = auth.login(email, password);
        if (user == null) {
            System.out.println("  Login failed. Incorrect email or password.");
            return null;
        }
        System.out.println("  Login successful!  Role: " + user.getRole().toUpperCase());
        return user;
    }

    // ----------------------------------------------------------
    // Customer registration flow
    // ----------------------------------------------------------
    private static void doRegister(Scanner sc, AuthService auth) {
        System.out.println("\n  --- CUSTOMER REGISTRATION ---");
        System.out.println("  (Admin accounts cannot be created here.)");
        System.out.print("  Full Name: "); String name  = sc.nextLine().trim();
        System.out.print("  Email    : "); String email = sc.nextLine().trim();
        System.out.print("  Password : "); String pass  = sc.nextLine().trim();
        System.out.print("  Phone    : "); String phone = sc.nextLine().trim();

        boolean ok = auth.register(name, email, pass, phone);
        if (ok) {
            System.out.println("  [OK] Registration successful! You can now log in.");
        } else {
            System.out.println("  [FAILED] Registration failed. See error above.");
        }
    }

    // ----------------------------------------------------------
    // Startup menu
    // ----------------------------------------------------------
    private static void printStartMenu() {
        System.out.println("  +------------------------------+");
        System.out.println("  |         MAIN MENU            |");
        System.out.println("  |  1. Login                    |");
        System.out.println("  |  2. Register (New Customer)  |");
        System.out.println("  |  3. Exit                     |");
        System.out.println("  +------------------------------+");
    }

    // ----------------------------------------------------------
    // Safe integer input -- retries on invalid input
    // ----------------------------------------------------------
    private static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  Please enter a number."); }
        }
    }
}
