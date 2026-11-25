// ============================================================
// DBConnection.java
// This class creates and returns a connection to the MySQL database.
// All other Java files use this class to get the connection.
// ============================================================

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // ----- Change these three values to match your MySQL setup -----
    private static final String URL      = "jdbc:mysql://localhost:3306/eventsphere";
    private static final String USERNAME = "root";   // your MySQL username
    private static final String PASSWORD = "root";   // your MySQL password
    // ---------------------------------------------------------------

    /**
     * Returns a live connection to the eventsphere database.
     * Returns null if the connection fails.
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Create and return the connection
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);

        } catch (ClassNotFoundException e) {
            // This happens when the MySQL JDBC driver JAR is missing
            System.out.println("Error: MySQL JDBC Driver not found.");
            System.out.println("Download mysql-connector-j-8.x.x.jar and add it to the classpath.");
            e.printStackTrace();

        } catch (SQLException e) {
            // This happens when the URL, username, or password is wrong,
            // or when MySQL is not running.
            System.out.println("Error: Cannot connect to the database.");
            System.out.println("Make sure MySQL is running and the credentials are correct.");
            e.printStackTrace();
        }
        return conn;
    }
}
