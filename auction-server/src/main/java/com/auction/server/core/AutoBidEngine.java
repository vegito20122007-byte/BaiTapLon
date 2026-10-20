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

    public AutoBidConfig getAutoBid(Long auctionId, Long bidderId) {
        Map<Long, AutoBidConfig> map = configsByAuction.get(auctionId);
        return map != null ? map.get(bidderId) : null;
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

    /**
     * Thuật toán tự động trả giá thay người dùng (Proxy Auto-Bidding):
     * Khi có một lượt bid mới từ đối thủ hoặc khi người dùng đăng ký auto-bid:
     * 1. Lọc ra các auto-bidder hợp lệ (không phải người đang dẫn đầu và maxBid >= giá tối thiểu tiếp theo)
     * 2. Sử dụng PriorityQueue để chọn auto-bidder có độ ưu tiên cao nhất
     * 3. Tự động đặt mức giá tối thiểu cần thiết để vượt đối thủ
     * 4. Lặp lại cho đến khi các auto-bidder đạt trạng thái cân bằng hoặc chạm trần maxBid
     */
    public void processAutoBids(Long auctionId, AuctionManager auctionManager) {
        int maxRounds = 30; // Giới hạn số vòng để đảm bảo hiệu năng
        int round = 0;

        while (round++ < maxRounds) {
            java.util.Optional<com.auction.common.model.Auction> opt = auctionManager.getAuctionDao().findById(auctionId);
            if (opt.isEmpty()) {
                break;
            }
            com.auction.common.model.Auction auction = opt.get();
            if (!auction.isActive() || auction.getStatus() != com.auction.common.enums.AuctionStatus.RUNNING) {
                break;
            }

            // Lấy danh sách auto-bid được sắp xếp theo PriorityQueue
            PriorityQueue<AutoBidConfig> pq = getSortedAutoBids(auctionId);
            if (pq.isEmpty()) {
                break;
            }

            // Tìm auto-bidder hợp lệ (không phải là người đang dẫn đầu)
            AutoBidConfig candidate = null;
            while (!pq.isEmpty()) {
                AutoBidConfig top = pq.poll();
                if (auction.getHighestBidderId() == null || !top.getBidderId().equals(auction.getHighestBidderId())) {
                    candidate = top;
                    break;
                }
            }

            if (candidate == null) {
                break; // Người dẫn đầu hiện tại chính là auto-bidder cao nhất
            }

            // Tính giá cần đặt tiếp theo
            double step = Math.max(auction.getMinBidIncrement(), candidate.getIncrement());
            double nextBid = auction.getCurrentHighestPrice() + step;
            if (auction.getHighestBidderId() == null) {
                nextBid = auction.getStartingPrice();
            }

            // Nếu giá vượt quá maxBid của người này thì dừng
            if (nextBid > candidate.getMaxBid()) {
                break;
            }

            try {
                // Đặt giá tự động thay người dùng
                auctionManager.placeBidInternal(auctionId, candidate.getBidderId(),
                        candidate.getBidderUsername(), nextBid, true);
            } catch (Exception e) {
                break;
            }
        }
    }
}
