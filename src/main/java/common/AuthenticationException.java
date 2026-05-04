package common;

public class AuthenticationException extends Exception {
    private String username;
    private String reason;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, String username) {
        super(message);
        this.username = username;
    }

    public AuthenticationException(String message, String username, String reason) {
        super(message);
        this.username = username;
        this.reason = reason;
    }

    public String getUsername() { return username; }
    public String getReason() { return reason; }
}