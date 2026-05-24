// server/config/ServerConfig.java
package server.config;

import common.constant.AuctionConstants;

public class ServerConfig {

    // Server settings
    private int port = AuctionConstants.DEFAULT_SERVER_PORT;
    private int maxThreadPoolSize = 50;
    private int connectionTimeoutSeconds = 30;

    // Auction settings
    private int snipingWindowSeconds = AuctionConstants.SNIPING_WINDOW_SECONDS;
    private int extensionSeconds = AuctionConstants.EXTENSION_SECONDS;

    // Singleton
    private static volatile ServerConfig instance;

    private ServerConfig() {}

    public static ServerConfig getInstance() {
        if (instance == null) {
            synchronized (ServerConfig.class) {
                if (instance == null) {
                    instance = new ServerConfig();
                }
            }
        }
        return instance;
    }

    // Getters
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getMaxThreadPoolSize() { return maxThreadPoolSize; }
    public void setMaxThreadPoolSize(int size) { this.maxThreadPoolSize = size; }

    public int getSnipingWindowSeconds() { return snipingWindowSeconds; }
    public int getExtensionSeconds() { return extensionSeconds; }
}