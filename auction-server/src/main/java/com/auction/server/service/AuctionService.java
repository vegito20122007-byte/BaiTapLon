package com.auction.server.service;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.factory.ItemFactory;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.model.Item;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.AutoBidEngine;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service tầng nghiệp vụ cho các tính năng đấu giá.
 */
public class AuctionService {
    private final AuctionManager auctionManager;

    public AuctionService() {
        this.auctionManager = AuctionManager.getInstance();
        seedSampleAuctions();
    }

    public AuctionService(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
        seedSampleAuctions();
    }

    private void seedSampleAuctions() {
        if (!auctionManager.getAuctionDao().findAll().isEmpty()) {
            return;
        }

        // Tạo sản phẩm 1: Điện tử
        Map<String, Object> p1 = new HashMap<>();
        p1.put("brand", "Sony");
        p1.put("warranty", 12);
        Item item1 = ItemFactory.createItem("ELECTRONICS", 1L, "Sony PlayStation 5 Pro",
                "Máy chơi game PS5 Pro thế hệ mới nhất", 18000000.0, 1L, p1);

        Auction a1 = new Auction(null, item1.getId(), item1.getName(), 1L, "seller1",
                item1.getStartingPrice(), 500000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        auctionManager.startAuction(a1);

        // Tạo sản phẩm 2: Nghệ thuật
        Map<String, Object> p2 = new HashMap<>();
        p2.put("artist", "Bùi Xuân Phái");
        p2.put("year", 1985);
        Item item2 = ItemFactory.createItem("ART", 2L, "Tranh Phố Cổ Hà Nội",
                "Tranh sơn dầu nguyên bản có chữ ký họa sĩ", 45000000.0, 1L, p2);

        Auction a2 = new Auction(null, item2.getId(), item2.getName(), 1L, "seller1",
                item2.getStartingPrice(), 1000000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(60));
        auctionManager.startAuction(a2);
    }

    public Auction createAuction(Long sellerId, String sellerUsername, String category, String itemName,
                                 String description, double startingPrice, double increment,
                                 int durationMinutes, Map<String, Object> extraParams) {

        Item item = ItemFactory.createItem(category, null, itemName, description, startingPrice, sellerId, extraParams);

        Auction auction = new Auction(null, item.getId(), item.getName(), sellerId, sellerUsername,
                startingPrice, increment, LocalDateTime.now(), LocalDateTime.now().plusMinutes(durationMinutes));

        auctionManager.startAuction(auction);
        return auction;
    }

    public BidTransaction placeBid(Long auctionId, Long bidderId, String bidderUsername, double amount)
            throws InvalidBidException, AuctionClosedException {
        return auctionManager.placeBid(auctionId, bidderId, bidderUsername, amount);
    }

    public void registerAutoBid(Long auctionId, Long bidderId, String bidderUsername, double maxBid, double increment) {
        AutoBidEngine.AutoBidConfig config = new AutoBidEngine.AutoBidConfig(bidderId, bidderUsername, maxBid, increment);
        auctionManager.getAutoBidEngine().registerAutoBid(auctionId, config);
    }

    public List<Auction> getAllAuctions() {
        return auctionManager.getAuctionDao().findAll();
    }

    public Optional<Auction> getAuctionById(Long id) {
        return auctionManager.getAuctionDao().findById(id);
    }

    public boolean cancelAuction(Long auctionId) {
        return auctionManager.cancelAuction(auctionId);
    }

    public boolean deleteAuction(Long auctionId) {
        return auctionManager.deleteAuction(auctionId);
    }
}
