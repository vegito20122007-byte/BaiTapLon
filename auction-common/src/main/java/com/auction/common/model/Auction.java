package com.auction.common.model;

import com.auction.common.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp đại diện cho một Phiên đấu giá (Auction).
 */
public class Auction extends Entity {
    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String itemName;
    private Long sellerId;
    private String sellerUsername;

    private double startingPrice;
    private double currentHighestPrice;
    private double minBidIncrement; // Bước giá tối thiểu

    private Long highestBidderId;
    private String highestBidderUsername;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    private List<BidTransaction> bidHistory = new ArrayList<>();

    public Auction() {
        super();
        this.status = AuctionStatus.OPEN;
        this.minBidIncrement = 10000.0; // Mặc định 10.000 VND
    }

    public Auction(Long id, Long itemId, String itemName, Long sellerId, String sellerUsername,
                   double startingPrice, double minBidIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.itemId = itemId;
        this.itemName = itemName;
        this.sellerId = sellerId;
        this.sellerUsername = sellerUsername;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice;
        this.minBidIncrement = minBidIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentHighestPrice() {
        return currentHighestPrice;
    }

    public void setCurrentHighestPrice(double currentHighestPrice) {
        this.currentHighestPrice = currentHighestPrice;
    }

    public double getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(double minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public Long getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(Long highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public String getHighestBidderUsername() {
        return highestBidderUsername;
    }

    public void setHighestBidderUsername(String highestBidderUsername) {
        this.highestBidderUsername = highestBidderUsername;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public void addBidTransaction(BidTransaction tx) {
        this.bidHistory.add(tx);
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return (status == AuctionStatus.RUNNING || status == AuctionStatus.OPEN)
                && (startTime == null || !now.isBefore(startTime))
                && (endTime == null || now.isBefore(endTime));
    }

    @Override
    public String getSummary() {
        return String.format("Auction #%d [%s] - Sản phẩm: %s | Giá hiện tại: %,.0f VND | Người dẫn đầu: %s",
                id, status, itemName, currentHighestPrice,
                highestBidderUsername != null ? highestBidderUsername : "(Chưa có)");
    }
}
