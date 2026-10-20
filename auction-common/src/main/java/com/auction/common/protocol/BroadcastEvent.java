package com.auction.common.protocol;

import java.io.Serializable;

/**
 * Sự kiện thông báo Realtime từ Server chủ động gửi (Push) tới các Client đang kết nối.
 */
public class BroadcastEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EventType {
        NEW_BID,            // Có lượt trả giá mới
        AUCTION_EXTENDED,   // Kích hoạt Anti-sniping gia hạn phiên
        AUCTION_ENDED,      // Phiên đấu giá đã kết thúc, có kết quả người thắng
        STATUS_CHANGED      // Thay đổi trạng thái OPEN -> RUNNING,...
    }

    private EventType eventType;
    private Long auctionId;
    private String eventDataJson;

    public BroadcastEvent() {
    }

    public BroadcastEvent(EventType eventType, Long auctionId, String eventDataJson) {
        this.eventType = eventType;
        this.auctionId = auctionId;
        this.eventDataJson = eventDataJson;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public String getEventDataJson() {
        return eventDataJson;
    }

    public void setEventDataJson(String eventDataJson) {
        this.eventDataJson = eventDataJson;
    }
}
