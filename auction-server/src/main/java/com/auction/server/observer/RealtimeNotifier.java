package com.auction.server.observer;

import com.auction.common.protocol.BroadcastEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Subject trong mô hình Observer Pattern.
 * Quản lý danh sách các Observer đăng ký theo dõi phiên đấu giá cụ thể và gửi thông báo thread-safe.
 */
public class RealtimeNotifier {
    private static final Logger log = LoggerFactory.getLogger(RealtimeNotifier.class);

    // Map: auctionId -> Set of Observers
    private final ConcurrentHashMap<Long, Set<AuctionObserver>> subscribers = new ConcurrentHashMap<>();

    // Danh sách quan sát viên toàn cục (ví dụ: màn hình danh sách dashboard)
    private final Set<AuctionObserver> globalSubscribers = ConcurrentHashMap.newKeySet();

    public void subscribe(Long auctionId, AuctionObserver observer) {
        subscribers.computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet()).add(observer);
        log.debug("Observer đã đăng ký theo dõi phiên đấu giá #{}", auctionId);
    }

    public void unsubscribe(Long auctionId, AuctionObserver observer) {
        Set<AuctionObserver> set = subscribers.get(auctionId);
        if (set != null) {
            set.remove(observer);
        }
    }

    public void subscribeGlobal(AuctionObserver observer) {
        globalSubscribers.add(observer);
    }

    public void unsubscribeGlobal(AuctionObserver observer) {
        globalSubscribers.remove(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        globalSubscribers.remove(observer);
        subscribers.values().forEach(set -> set.remove(observer));
    }

    /**
     * Thông báo cho tất cả observers đang theo dõi phiên đấu giá này (Thread-safe notify).
     */
    public void notifySubscribers(BroadcastEvent event) {
        if (event == null) {
            return;
        }

        // Báo cho global observers (ví dụ cập nhật trạng thái ở dashboard)
        for (AuctionObserver observer : globalSubscribers) {
            try {
                observer.onAuctionEvent(event);
            } catch (Exception e) {
                log.error("Lỗi khi notify global observer: {}", e.getMessage());
            }
        }

        // Báo cho các client đang mở trực tiếp phòng đấu giá này
        if (event.getAuctionId() != null) {
            Set<AuctionObserver> roomObservers = subscribers.getOrDefault(event.getAuctionId(), Collections.emptySet());
            for (AuctionObserver observer : roomObservers) {
                try {
                    observer.onAuctionEvent(event);
                } catch (Exception e) {
                    log.error("Lỗi khi notify room observer: {}", e.getMessage());
                }
            }
        }
    }
}
