package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionServer {
    public static final int PORT = 9999;
    private static DatabaseManager dbManager;

    public static void main(String[] args) {
        try {
            dbManager = new DatabaseManager();
            System.out.println("[" + getTime() + "] ✅ Server started on port " + PORT);

            // Scheduler kết thúc đấu giá mỗi 10 giây
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    dbManager.endExpiredAuctions();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }, 0, 10, TimeUnit.SECONDS);

            ServerSocket serverSocket = new ServerSocket(PORT);
            ExecutorService threadPool = Executors.newCachedThreadPool();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[" + getTime() + "] 📱 Client connected");
                threadPool.execute(new ClientHandler(clientSocket, dbManager));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }
}