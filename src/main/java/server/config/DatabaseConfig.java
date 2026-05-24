// server/config/DatabaseConfig.java
package server.config;

public class DatabaseConfig {

    private String url = "jdbc:mysql://localhost:3306/auction_db";
    private String username = "root";
    private String password = "Nhom7123456";
    private int maxPoolSize = 10;
    private int minPoolSize = 5;

    private static volatile DatabaseConfig instance;

    private DatabaseConfig() {}

    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (DatabaseConfig.class) {
                if (instance == null) {
                    instance = new DatabaseConfig();
                }
            }
        }
        return instance;
    }

    // Getters & Setters
    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getMinPoolSize() { return minPoolSize; }
}
