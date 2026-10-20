package com.auction.server;

import com.auction.common.model.Auction;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.AutoBidEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

public class AutoBidTest {

    private AuctionManager auctionManager;
    private Auction auction;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
        auction = new Auction(null, 500L, "Sản phẩm Auto-Bid Test", 999L, "seller_test",
                100000.0, 10000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(15));
        auctionManager.startAuction(auction);
    }

    @Test
    @DisplayName("AutoBidEngine: Sắp xếp cấu hình theo PriorityQueue (ưu tiên maxBid cao hơn)")
    void testPriorityQueueOrdering() {
        AutoBidEngine engine = new AutoBidEngine();
        Long aucId = 9999L;

        AutoBidEngine.AutoBidConfig cfg1 = new AutoBidEngine.AutoBidConfig(1L, "user1", 200000.0, 10000.0);
        AutoBidEngine.AutoBidConfig cfg2 = new AutoBidEngine.AutoBidConfig(2L, "user2", 500000.0, 20000.0);
        AutoBidEngine.AutoBidConfig cfg3 = new AutoBidEngine.AutoBidConfig(3L, "user3", 300000.0, 15000.0);

        engine.registerAutoBid(aucId, cfg1);
        engine.registerAutoBid(aucId, cfg2);
        engine.registerAutoBid(aucId, cfg3);

        PriorityQueue<AutoBidEngine.AutoBidConfig> pq = engine.getSortedAutoBids(aucId);
        assertEquals(3, pq.size());

        // Phần tử đầu tiên phải là cfg2 (maxBid = 500,000 cao nhất)
        AutoBidEngine.AutoBidConfig top = pq.poll();
        assertNotNull(top);
        assertEquals(2L, top.getBidderId());
        assertEquals(500000.0, top.getMaxBid());

        // Phần tử thứ 2 phải là cfg3 (maxBid = 300,000)
        assertEquals(3L, pq.poll().getBidderId());

        // Phần tử cuối là cfg1 (maxBid = 200,000)
        assertEquals(1L, pq.poll().getBidderId());
    }

    @Test
    @DisplayName("Auto-Bidding: Tự động trả giá khi đối thủ đặt giá mới")
    void testAutoBidCounterBid() throws Exception {
        // Bidder 10 bật Auto-Bid với giá trần 300,000 VND và bước giá 10,000 VND
        auctionManager.registerAutoBid(auction.getId(), 10L, "auto_bidder_10", 300000.0, 10000.0);

        Auction updated = auctionManager.getAuctionDao().findById(auction.getId()).orElseThrow();
        // Ngay khi bật Auto-Bid vào phiên chưa có người trả giá, nó tự động đặt mức khởi điểm 100,000 VND
        assertEquals(10L, updated.getHighestBidderId());
        assertEquals(100000.0, updated.getCurrentHighestPrice());

        // Đối thủ (Bidder 20) vào đặt giá thủ công 150,000 VND
        auctionManager.placeBid(auction.getId(), 20L, "manual_bidder_20", 150000.0);

        // Auto-Bidder 10 phải tự động phản hồi lại và dẫn đầu với giá 160,000 VND (150,000 + 10,000)
        Auction afterOpponent = auctionManager.getAuctionDao().findById(auction.getId()).orElseThrow();
        assertEquals(10L, afterOpponent.getHighestBidderId());
        assertEquals(160000.0, afterOpponent.getCurrentHighestPrice());
        assertEquals("auto_bidder_10", afterOpponent.getHighestBidderUsername());
    }

    @Test
    @DisplayName("Auto-Bidding: Dừng trả giá khi giá vượt quá maxBid")
    void testAutoBidCeilingLimit() throws Exception {
        // Auto-bidder chỉ trả tối đa 200,000 VND
        auctionManager.registerAutoBid(auction.getId(), 11L, "limited_bidder", 200000.0, 10000.0);

        // Đối thủ đặt hẳn 250,000 VND (vượt quá maxBid)
        auctionManager.placeBid(auction.getId(), 22L, "rich_bidder", 250000.0);

        // Auto-bidder không được tự động nâng giá quá trần
        Auction finalAuction = auctionManager.getAuctionDao().findById(auction.getId()).orElseThrow();
        assertEquals(22L, finalAuction.getHighestBidderId());
        assertEquals(250000.0, finalAuction.getCurrentHighestPrice());
    }
}
