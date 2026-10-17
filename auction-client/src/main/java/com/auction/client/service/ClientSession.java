package com.auction.client.service;

import com.auction.common.model.User;

/**
 * Singleton quản lý trạng thái phiên đăng nhập của Client.
 */
public class ClientSession {
    private static ClientSession instance;

    private String token;
    private User currentUser;

    private ClientSession() {
    }

    public static synchronized ClientSession getInstance() {
        if (instance == null) {
            instance = new ClientSession();
        }
        return instance;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isLoggedIn() {
        return token != null && currentUser != null;
    }

    public void clear() {
        this.token = null;
        this.currentUser = null;
    }
}
