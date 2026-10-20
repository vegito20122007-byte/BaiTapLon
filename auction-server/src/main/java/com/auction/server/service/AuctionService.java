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
        a1.setDescription(item1.getDescription());
        a1.setItem(item1);
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
        a2.setDescription(item2.getDescription());
        a2.setItem(item2);
        auctionManager.startAuction(a2);

        // Tạo sản phẩm 3: Phương tiện (Vehicle)
        Map<String, Object> p3 = new HashMap<>();
        p3.put("manufacturer", "Toyota");
        p3.put("odometer", 12000);
        Item item3 = ItemFactory.createItem("VEHICLE", 3L, "Toyota Camry 2.5Q 2023",
                "Sedan hạng D nhập khẩu nguyên chiếc, bảo dưỡng hãng đầy đủ", 850000000.0, 1L, p3);

        Auction a3 = new Auction(null, item3.getId(), item3.getName(), 1L, "seller1",
                item3.getStartingPrice(), 10000000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(120));
        a3.setDescription(item3.getDescription());
        a3.setItem(item3);
        auctionManager.startAuction(a3);
    }

    public Auction createAuction(Long sellerId, String sellerUsername, String category, String itemName,
                                 String description, double startingPrice, double increment,
                                 int durationMinutes, Map<String, Object> extraParams) {

        Item item = ItemFactory.createItem(category, null, itemName, description, startingPrice, sellerId, extraParams);

        Auction auction = new Auction(null, item.getId(), item.getName(), sellerId, sellerUsername,
                startingPrice, increment, LocalDateTime.now(), LocalDateTime.now().plusMinutes(durationMinutes));
        auction.setDescription(description);
        auction.setItem(item);

        auctionManager.startAuction(auction);
        return auction;
    }

    public BidTransaction placeBid(Long auctionId, Long bidderId, String bidderUsername, double amount)
            throws InvalidBidException, AuctionClosedException {
        return auctionManager.placeBid(auctionId, bidderId, bidderUsername, amount);
    }

    public void registerAutoBid(Long auctionId, Long bidderId, String bidderUsername, double maxBid, double increment) {
        auctionManager.registerAutoBid(auctionId, bidderId, bidderUsername, maxBid, increment);
    }

    public void cancelAutoBid(Long auctionId, Long bidderId) {
        auctionManager.cancelAutoBid(auctionId, bidderId);
    }

    public boolean updateAuction(Long auctionId, Long sellerId, String itemName, String description,
                                 double startingPrice, double increment, int durationMinutes) throws InvalidBidException {
        return auctionManager.updateAuction(auctionId, sellerId, itemName, description, startingPrice, increment, durationMinutes);
    }

    public boolean deleteSellerAuction(Long auctionId, Long sellerId) throws InvalidBidException {
        return auctionManager.deleteSellerAuction(auctionId, sellerId);
    }

    public boolean payAuction(Long auctionId, Long userId) throws InvalidBidException {
        return auctionManager.payAuction(auctionId, userId);
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
