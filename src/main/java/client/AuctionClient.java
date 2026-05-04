package client;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import common.*;

public class AuctionClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private boolean listening = false;
    private Consumer<ResponseMessage> realtimeCallback;
    private BlockingQueue<ResponseMessage> responseQueue = new LinkedBlockingQueue<>();
    private Thread listenerThread;

    public AuctionClient() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000); // 5 second timeout
        socket.setSoTimeout(30000); // 30 second read timeout
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("✅ Connected to server");
    }

    public void startRealtimeListener(Consumer<ResponseMessage> callback) {
        if (listening) return;
        this.realtimeCallback = callback;
        listening = true;

        listenerThread = new Thread(() -> {
            try {
                String line;
                while (listening && (line = in.readLine()) != null) {
                    try {
                        ResponseMessage msg = gson.fromJson(line, ResponseMessage.class);
                        if (msg != null) {
                            if (callback != null && "realtime_update".equals(msg.getMessage())) {
                                callback.accept(msg);
                            } else {
                                responseQueue.offer(msg);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Parse error: " + e.getMessage());
                    }
                }
            } catch (SocketException e) {
                if (listening) {
                    System.out.println("❌ Connection lost: " + e.getMessage());
                }
            } catch (IOException e) {
                if (listening) {
                    System.out.println("❌ Listener stopped: " + e.getMessage());
                }
            } finally {
                listening = false;
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public ResponseMessage sendRequest(CommandType command, Object data) throws IOException, InterruptedException {
        if (!listening) {
            throw new IOException("Not connected to server");
        }

        Request request = new Request(command, data);
        String jsonRequest = gson.toJson(request);
        System.out.println("📤 Sending: " + jsonRequest);
        out.println(jsonRequest);
        out.flush();

        ResponseMessage response = responseQueue.poll(10, TimeUnit.SECONDS);
        if (response == null) {
            throw new IOException("Timeout waiting for response from server");
        }
        return response;
    }

    public void sendAsync(CommandType command, Object data) {
        if (!listening) return;
        Request request = new Request(command, data);
        out.println(gson.toJson(request));
        out.flush();
    }

    public ResponseMessage login(String username, String password) throws IOException, InterruptedException {
        return sendRequest(CommandType.LOGIN, new Object[]{username, password});
    }

    public ResponseMessage register(User user) throws IOException, InterruptedException {
        return sendRequest(CommandType.REGISTER, user);
    }

    public ResponseMessage getActiveItems() throws IOException, InterruptedException {
        return sendRequest(CommandType.GET_ACTIVE_ITEMS, null);
    }

    public ResponseMessage createItem(Item item) throws IOException, InterruptedException {
        return sendRequest(CommandType.CREATE_ITEM, item);
    }

    public ResponseMessage placeBid(Bid bid) throws IOException, InterruptedException {
        return sendRequest(CommandType.PLACE_BID, bid);
    }

    public ResponseMessage getBidHistory(String itemId) throws IOException, InterruptedException {
        return sendRequest(CommandType.GET_BID_HISTORY, itemId);
    }

    public void subscribeItem(String itemId) {
        sendAsync(CommandType.SUBSCRIBE_ITEM, itemId);
        System.out.println("📡 Subscribed to item: " + itemId);
    }

    public void unsubscribeItem(String itemId) {
        sendAsync(CommandType.UNSUBSCRIBE_ITEM, itemId);
    }

    public void disconnect() throws IOException {
        listening = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        sendAsync(CommandType.LOGOUT, null);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public boolean isConnected() {
        return listening && socket != null && socket.isConnected();
    }
}