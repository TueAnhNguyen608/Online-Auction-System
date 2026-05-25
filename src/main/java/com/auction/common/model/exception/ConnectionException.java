package com.auction.common.model.exception;

public class ConnectionException extends Exception {
    private String host;
    private int port;

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, String host, int port) {
        super(message);
        this.host = host;
        this.port = port;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
}