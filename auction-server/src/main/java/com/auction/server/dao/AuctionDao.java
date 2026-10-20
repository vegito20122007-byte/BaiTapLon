package com.auction.server.dao;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.Auction;
import java.util.List;
import java.util.Optional;

public interface AuctionDao {
    Auction save(Auction auction);
    Optional<Auction> findById(Long id);
    List<Auction> findAll();
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findBySellerId(Long sellerId);
    boolean deleteById(Long id);
}
