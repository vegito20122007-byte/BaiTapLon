package com.auction.server.network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý danh sách các kết nối ClientHandler đang trực tuyến.
 */
public class ClientSessionManager {
    private final Set<ClientHandler> activeHandlers = ConcurrentHashMap.newKeySet();

    public void register(ClientHandler handler) {
        activeHandlers.add(handler);
    }

    public void unregister(ClientHandler handler) {
        activeHandlers.remove(handler);
    }

    public int getActiveCount() {
        return activeHandlers.size();
    }
}
