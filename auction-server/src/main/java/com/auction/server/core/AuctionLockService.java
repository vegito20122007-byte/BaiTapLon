package com.auction.server.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Quản lý Fine-grained Concurrency Locking theo từng Auction ID.
 * Đảm bảo các luồng đặt giá vào CÙNG một phiên sẽ được tuần tự hóa an toàn (Atomic),
 * trong khi các phiên KHÁC NHAU vẫn chạy song song tối đa hiệu năng.
 * Tránh triệt để: Race condition, Lost update, Double winners.
 */
public class AuctionLockService {

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock getLock(Long auctionId) {
        return locks.computeIfAbsent(auctionId, id -> new ReentrantLock(true)); // Fair lock
    }

    public void removeLock(Long auctionId) {
        locks.remove(auctionId);
    }
}
