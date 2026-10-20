package com.auction.common.enums;

/**
 * Các loại lệnh (commands) được gửi giữa Client và Server qua Socket.
 */
public enum CommandType {
    LOGIN,
    REGISTER,
    GET_AUCTIONS,
    GET_AUCTION_DETAIL,
    CREATE_AUCTION,
    PLACE_BID,
    SET_AUTO_BID,
    CANCEL_AUTO_BID,
    SUBSCRIBE_AUCTION,
    UNSUBSCRIBE_AUCTION,
    LOGOUT,
    // Quản lý sản phẩm cho Người bán (Seller) và Admin
    UPDATE_AUCTION,
    DELETE_SELLER_AUCTION,
    PAY_AUCTION,
    // Các lệnh dành riêng cho Quản trị viên (Admin)
    GET_ALL_USERS,
    DELETE_USER,
    CANCEL_AUCTION,
    DELETE_AUCTION
}
