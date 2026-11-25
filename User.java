// ============================================================
// User.java
// A simple data-holder class for the currently logged-in user.
// AuthService creates and returns this object after a successful login.
// Main.java passes it to AdminPanel or CustomerPanel.
// ============================================================

public class User {

    private int    userId;
    private String name;
    private String email;
    private String phone;
    private String role;       // "admin" or "customer"

    // Constructor -- called by AuthService after reading the DB row
    public User(int userId, String name, String email, String phone, String role) {
        this.userId = userId;
        this.name   = name;
        this.email  = email;
        this.phone  = phone;
        this.role   = role;
    }

    // Getters
    public int    getUserId() { return userId; }
    public String getName()   { return name;   }
    public String getEmail()  { return email;  }
    public String getPhone()  { return phone;  }
    public String getRole()   { return role;   }

    // Convenience check methods used in AdminPanel / CustomerPanel
    public boolean isAdmin()    { return "admin".equals(role);    }
    public boolean isCustomer() { return "customer".equals(role); }

    @Override
    public String toString() {
        return String.format("User[id=%d, name=%s, role=%s]", userId, name, role);
    }
}
