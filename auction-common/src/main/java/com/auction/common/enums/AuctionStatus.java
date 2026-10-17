package com.auction.common.enums;

/**
 * Các trạng thái của một phiên đấu giá theo yêu cầu đề bài:
 * OPEN -> RUNNING -> FINISHED -> PAID / CANCELED
 */
public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}
