package com.auction.client;

/**
 * Lớp Launcher phụ trợ chạy ứng dụng JavaFX mà không bị lỗi Module Path trên các IDE (IntelliJ, Eclipse).
 */
public class Launcher {
    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
