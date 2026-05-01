package common;

public class Admin extends User {
    public Admin() {
        super();
    }

    public Admin(String username, String password, String role, String fullName) {
        super(username, password, role, fullName);
    }
}