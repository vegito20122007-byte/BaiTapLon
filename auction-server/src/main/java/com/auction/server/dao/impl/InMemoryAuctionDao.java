package com.auction.server.dao.impl;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.Auction;
import com.auction.common.util.JsonUtils;
import com.auction.server.dao.AuctionDao;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread-safe Persistent DAO cho Auction (lưu trữ bộ nhớ và tự động đồng bộ file data/auctions.json).
 */
public class InMemoryAuctionDao implements AuctionDao {
    private static final Logger log = LoggerFactory.getLogger(InMemoryAuctionDao.class);
    private static final String DATA_FILE = "data/auctions.json";

    private final Map<Long, Auction> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public InMemoryAuctionDao() {
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        try {
            Path path = Paths.get(DATA_FILE);
            if (Files.exists(path)) {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                if (content != null && !content.trim().isEmpty()) {
                    Type listType = new TypeToken<List<Auction>>() {}.getType();
                    List<Auction> list = JsonUtils.getGson().fromJson(content, listType);
                    if (list != null) {
                        long maxId = 0;
                        for (Auction a : list) {
                            if (a.getId() != null) {
                                storage.put(a.getId(), a);
                                if (a.getId() > maxId) {
                                    maxId = a.getId();
                                }
                            }
                        }
                        idGenerator.set(maxId + 1);
                        log.info("Đã nạp {} phiên đấu giá từ {}", storage.size(), DATA_FILE);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Không thể đọc dữ liệu đấu giá từ {}: {}", DATA_FILE, e.getMessage());
        }
    }

    private synchronized void persistToFile() {
        try {
            Path path = Paths.get(DATA_FILE);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            List<Auction> list = new ArrayList<>(storage.values());
            String json = JsonUtils.toJson(list);
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Lỗi lưu dữ liệu đấu giá vào {}: {}", DATA_FILE, e.getMessage());
        }
    }

    @Override
    public Auction save(Auction auction) {
        if (auction.getId() == null) {
            auction.setId(idGenerator.getAndIncrement());
        }
        storage.put(auction.getId(), auction);
        persistToFile();
        return auction;
    }

    @Override
    public Optional<Auction> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Auction> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Auction> findByStatus(AuctionStatus status) {
        return storage.values().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Auction> findBySellerId(Long sellerId) {
        return storage.values().stream()
                .filter(a -> a.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(Long id) {
        boolean removed = storage.remove(id) != null;
        if (removed) {
            persistToFile();
        }
        return removed;
    }
}
