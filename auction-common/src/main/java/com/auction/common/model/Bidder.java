package com.auction.common.model;

import com.auction.common.enums.UserRole;

/**
 * Lớp đại diện cho Người tham gia đấu giá (Bidder).
 */
public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    private double balance; // Số dư tài khoản đặt cọc/thanh toán

    public Bidder() {
        super();
        this.role = UserRole.BIDDER;
    }

    public Bidder(Long id, String username, String password, String fullName, String email, double balance) {
        super(id, username, password, fullName, email, UserRole.BIDDER);
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String getSummary() {
        return String.format("[Bidder] %s | Số dư: %,.0f VND", username, balance);
    }
}
