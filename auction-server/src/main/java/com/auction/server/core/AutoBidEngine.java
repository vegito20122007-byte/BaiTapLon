package com.auction.server.core;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Động cơ Đấu giá tự động (Auto-Bidding Engine) áp dụng PriorityQueue theo đúng yêu cầu đề bài:
 * - So sánh nhiều auto-bid cùng lúc
 * - Ưu tiên theo maxBid và thời điểm đăng ký (FIFO)
 * - Không vượt quá maxBid
 */
public class AutoBidEngine {

    public static class AutoBidConfig {
        private final Long bidderId;
        private final String bidderUsername;
        private final double maxBid;
        private final double increment;
        private final LocalDateTime registeredTime;

        public AutoBidConfig(Long bidderId, String bidderUsername, double maxBid, double increment) {
            this.bidderId = bidderId;
            this.bidderUsername = bidderUsername;
            this.maxBid = maxBid;
            this.increment = increment;
            this.registeredTime = LocalDateTime.now();
        }

        public Long getBidderId() {
            return bidderId;
        }

        public String getBidderUsername() {
            return bidderUsername;
        }

        public double getMaxBid() {
            return maxBid;
        }

        public double getIncrement() {
            return increment;
        }

        public LocalDateTime getRegisteredTime() {
            return registeredTime;
        }
    }

    // Map: auctionId -> Map<bidderId, AutoBidConfig>
    private final Map<Long, Map<Long, AutoBidConfig>> configsByAuction = new ConcurrentHashMap<>();

    public void registerAutoBid(Long auctionId, AutoBidConfig config) {
        configsByAuction.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>())
                .put(config.getBidderId(), config);
    }

    public void removeAutoBid(Long auctionId, Long bidderId) {
        Map<Long, AutoBidConfig> map = configsByAuction.get(auctionId);
        if (map != null) {
            map.remove(bidderId);
        }
    }

    /**
     * Dùng PriorityQueue để lấy ra cấu hình auto-bid tối ưu nhất:
     * 1. Ưu tiên maxBid cao nhất
     * 2. Nếu bằng maxBid thì ưu tiên thời điểm đăng ký sớm hơn (FIFO)
     */
    public PriorityQueue<AutoBidConfig> getSortedAutoBids(Long auctionId) {
        Map<Long, AutoBidConfig> map = configsByAuction.get(auctionId);
        PriorityQueue<AutoBidConfig> pq = new PriorityQueue<>(
                Comparator.comparingDouble(AutoBidConfig::getMaxBid).reversed()
                        .thenComparing(AutoBidConfig::getRegisteredTime)
        );

        if (map != null) {
            pq.addAll(map.values());
        }
        return pq;
    }
}
