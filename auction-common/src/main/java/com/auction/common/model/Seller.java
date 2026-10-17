package com.auction.common.model;

import com.auction.common.enums.UserRole;

/**
 * Lớp đại diện cho Người bán (Seller) đăng sản phẩm.
 */
public class Seller extends User {
    private static final long serialVersionUID = 1L;

    private double rating; // Đánh giá uy tín người bán

    public Seller() {
        super();
        this.role = UserRole.SELLER;
    }

    public Seller(Long id, String username, String password, String fullName, String email, double rating) {
        super(id, username, password, fullName, email, UserRole.SELLER);
        this.rating = rating;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    @Override
    public String getSummary() {
        return String.format("[Seller] %s | Uy tín: %.1f/5.0", username, rating);
    }
}
