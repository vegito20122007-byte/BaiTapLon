package com.auction.client.network;

import com.auction.common.protocol.BroadcastEvent;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.auction.common.util.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Quản lý kết nối Socket và luồng giao tiếp với Server.
 * Hỗ trợ nhận phản hồi đồng bộ và nhận sự kiện Realtime bất đồng bộ.
 */
public class SocketClient {
    private static SocketClient instance;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = JsonUtils.getGson();

    private final BlockingQueue<ResponseMessage> responseQueue = new LinkedBlockingQueue<>();
    private Consumer<BroadcastEvent> eventListener;
    private volatile boolean isRunning = false;

    private SocketClient() {
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void setEventListener(Consumer<BroadcastEvent> listener) {
        this.eventListener = listener;
    }

    public synchronized void connect(String host, int port) throws IOException {
        if (socket != null && !socket.isClosed()) {
            return;
        }

        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        isRunning = true;

        // Bắt đầu luồng nền liên tục lắng nghe thông điệp từ server
        Thread listenThread = new Thread(this::listenLoop, "SocketClient-Listener");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void listenLoop() {
        try {
            String line;
            while (isRunning && (line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Phân biệt thông điệp phản hồi (ResponseMessage) hay sự kiện đẩy (BroadcastEvent)
                JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                if (obj.has("eventType")) {
                    BroadcastEvent event = gson.fromJson(line, BroadcastEvent.class);
                    if (eventListener != null) {
                        eventListener.accept(event);
                    }
                } else {
                    ResponseMessage res = gson.fromJson(line, ResponseMessage.class);
                    responseQueue.offer(res);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("Mất kết nối với Server: " + e.getMessage());
            }
        }
    }

    public synchronized ResponseMessage sendRequest(RequestMessage request) throws Exception {
        if (writer == null) {
            throw new IllegalStateException("Chưa kết nối tới Server!");
        }

        writer.println(gson.toJson(request));
        return responseQueue.take(); // Đợi phản hồi tương ứng từ server
    }

    public synchronized boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected() && writer != null;
    }

    public synchronized void disconnect() {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        } finally {
            socket = null;
            reader = null;
            writer = null;
        }
    }
}
