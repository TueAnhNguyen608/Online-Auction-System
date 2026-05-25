package com.auction.server.network;

import com.auction.common.constant.AuctionConstants;
import com.auction.server.handler.ClientConnectionHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerSocketManager {

    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final AtomicBoolean accepting = new AtomicBoolean(false);
    private int activeConnections = 0;

    public ServerSocketManager(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void startAccepting() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(AuctionConstants.SOCKET_TIMEOUT_MS);
            accepting.set(true);
            System.out.println("✅ ServerSocket listening on port " + port);
            System.out.println("   Timeout: " + AuctionConstants.SOCKET_TIMEOUT_MS + "ms");

            while (accepting.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setKeepAlive(true);
                    clientSocket.setTcpNoDelay(true);

                    activeConnections++;
                    System.out.println("📱 New client connected: " + clientSocket.getInetAddress() +
                            " (Active: " + activeConnections + ")");

                    ClientConnectionHandler handler = new ClientConnectionHandler(clientSocket);
                    threadPool.execute(handler);

                } catch (SocketTimeoutException e) {
                    continue;
                } catch (Exception e) {
                    if (accepting.get()) {
                        System.err.println("❌ Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (accepting.get()) {
                System.err.println("❌ Server error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        accepting.set(false);

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("🛑 Server socket manager stopped. Active connections: " + activeConnections);
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public boolean isRunning() {
        return accepting.get() && serverSocket != null && !serverSocket.isClosed();
    }

    public void connectionClosed() {
        activeConnections--;
        System.out.println("📱 Client disconnected. Active connections: " + activeConnections);
    }
}