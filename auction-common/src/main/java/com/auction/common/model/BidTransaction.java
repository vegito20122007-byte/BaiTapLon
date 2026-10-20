package com.auction.common.model;

import java.time.LocalDateTime;

/**
 * Giao dịch đặt giá (BidTransaction) lưu lịch sử từng lượt bid.
 */
public class BidTransaction extends Entity {
    private static final long serialVersionUID = 1L;

    private Long auctionId;
    private Long bidderId;
    private String bidderUsername;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction() {
        super();
        this.bidTime = LocalDateTime.now();
    }

    public BidTransaction(Long id, Long auctionId, Long bidderId, String bidderUsername, double bidAmount) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public void setBidderUsername(String bidderUsername) {
        this.bidderUsername = bidderUsername;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    @Override
    public String getSummary() {
        return String.format("[%s] %s trả giá %,.0f VND", bidTime, bidderUsername, bidAmount);
    }
}
