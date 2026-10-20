package com.auction.server;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.server.core.AuctionManager;
import com.auction.server.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionServiceTest {

    private AuctionService auctionService;
    private Auction testAuction;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService();
        testAuction = new Auction(null, 100L, "Vật phẩm test", 999L, "seller_test",
                100000.0, 10000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(10));
        AuctionManager.getInstance().startAuction(testAuction);
    }

    @Test
    @DisplayName("Đặt giá hợp lệ thành công và cập nhật người dẫn đầu")
    void testPlaceValidBid() throws Exception {
        BidTransaction tx = auctionService.placeBid(testAuction.getId(), 200L, "bidder_1", 110000.0);

        assertNotNull(tx);
        assertEquals(110000.0, tx.getBidAmount());
        assertEquals("bidder_1", tx.getBidderUsername());

        Auction updated = auctionService.getAuctionById(testAuction.getId()).orElseThrow();
        assertEquals(110000.0, updated.getCurrentHighestPrice());
        assertEquals("bidder_1", updated.getHighestBidderUsername());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi đặt giá thấp hơn giá hiện tại")
    void testPlaceBidLowerThanCurrentThrowsException() throws Exception {
        auctionService.placeBid(testAuction.getId(), 200L, "bidder_1", 150000.0);

        // Đặt giá thấp hơn 150.000 + 10.000 bước giá
        assertThrows(InvalidBidException.class, () -> {
            auctionService.placeBid(testAuction.getId(), 201L, "bidder_2", 140000.0);
        });
    }

    @Test
    @DisplayName("Ném ngoại lệ khi người bán tự đấu giá sản phẩm của chính mình")
    void testSellerCannotBidOwnItem() {
        assertThrows(InvalidBidException.class, () -> {
            auctionService.placeBid(testAuction.getId(), 999L, "seller_test", 200000.0);
        });
    }

    @Test
    @DisplayName("Ném ngoại lệ khi đặt giá vào phiên đã kết thúc")
    void testBidOnFinishedAuctionThrowsException() {
        AuctionManager.getInstance().closeAuction(testAuction.getId());

        assertThrows(AuctionClosedException.class, () -> {
            auctionService.placeBid(testAuction.getId(), 200L, "bidder_1", 200000.0);
        });
    }
}
