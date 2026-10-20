package com.auction.server;

import com.auction.server.core.AuctionManager;
import com.auction.server.network.SocketServer;

/**
 * Điểm khởi chạy chính của Server.
 */
public class ServerApp {
    public static final int DEFAULT_PORT = 8888;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        SocketServer server = new SocketServer(port);

        // Đăng ký hook tắt ứng dụng an toàn khi bấm Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Đang tắt hệ thống đấu giá...");
            server.stop();
            AuctionManager.getInstance().shutdown();
        }));

        server.start();
    }
}
