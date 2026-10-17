package com.auction.server.network;

import com.auction.server.service.AuctionService;
import com.auction.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Socket Server đa luồng lắng nghe các kết nối từ Client.
 */
public class SocketServer {
    private static final Logger log = LoggerFactory.getLogger(SocketServer.class);

    private final int port;
    private final UserService userService;
    private final AuctionService auctionService;
    private final ClientSessionManager sessionManager;
    private final ExecutorService threadPool;

    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;

    public SocketServer(int port) {
        this.port = port;
        this.userService = new UserService();
        this.auctionService = new AuctionService();
        this.sessionManager = new ClientSessionManager();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            log.info("=== HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - SERVER KHỞI ĐỘNG TẠI PORT {} ===", port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, userService, auctionService, sessionManager);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            if (isRunning) {
                log.error("Lỗi SocketServer: {}", e.getMessage(), e);
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        threadPool.shutdown();
        log.info("Server đã dừng hoạt động an toàn.");
    }
}
