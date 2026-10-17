package com.auction.server.core;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.protocol.BroadcastEvent;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.impl.InMemoryAuctionDao;
import com.auction.server.observer.RealtimeNotifier;
import com.auction.server.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton Pattern: Quản lý tập trung toàn bộ các phiên đấu giá, điều phối luồng,
 * quản lý scheduler đếm ngược kết thúc phiên và kích hoạt Anti-sniping.
 */
public class AuctionManager {
    private static final Logger log = LoggerFactory.getLogger(AuctionManager.class);

    private static volatile AuctionManager instance;

    private final AuctionDao auctionDao;
    private final AuctionLockService lockService;
    private final RealtimeNotifier notifier;
    private final AutoBidEngine autoBidEngine;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // Cấu hình thuật toán Anti-sniping:
    // Nếu có bid trong 30 giây cuối cùng, tự động gia hạn thêm 60 giây
    public static final int ANTI_SNIPING_WINDOW_SECONDS = 30;
    public static final int ANTI_SNIPING_EXTEND_SECONDS = 60;

    private AuctionManager() {
        this.auctionDao = new InMemoryAuctionDao();
        this.lockService = new AuctionLockService();
        this.notifier = new RealtimeNotifier();
        this.autoBidEngine = new AutoBidEngine();
        this.scheduler = Executors.newScheduledThreadPool(4);

        // Khôi phục bộ hẹn giờ kết thúc cho các phiên RUNNING đã lưu từ file
        for (Auction auction : auctionDao.findByStatus(AuctionStatus.RUNNING)) {
            scheduleAuctionEnding(auction);
        }
    }

    /**
     * Double-checked locking Thread-safe Singleton.
     */
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public AuctionDao getAuctionDao() {
        return auctionDao;
    }

    public RealtimeNotifier getNotifier() {
        return notifier;
    }

    public AutoBidEngine getAutoBidEngine() {
        return autoBidEngine;
    }

    /**
     * Bắt đầu một phiên đấu giá và thiết lập lịch tự động đóng khi hết thời gian.
     */
    public void startAuction(Auction auction) {
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDao.save(auction);

        scheduleAuctionEnding(auction);

        BroadcastEvent event = new BroadcastEvent(BroadcastEvent.EventType.STATUS_CHANGED,
                auction.getId(), JsonUtils.toJson(auction));
        notifier.notifySubscribers(event);
    }

    /**
     * Xử lý đặt giá an toàn đồng thời (Thread-safe Concurrent Bidding):
     * Khóa đúng phòng đấu giá qua ReentrantLock.
     */
    public BidTransaction placeBid(Long auctionId, Long bidderId, String bidderUsername, double bidAmount)
            throws InvalidBidException, AuctionClosedException {

        ReentrantLock lock = lockService.getLock(auctionId);
        lock.lock();
        try {
            Optional<Auction> opt = auctionDao.findById(auctionId);
            if (opt.isEmpty()) {
                throw new InvalidBidException("Không tìm thấy phiên đấu giá #" + auctionId);
            }
            Auction auction = opt.get();

            // 1. Kiểm tra trạng thái phiên
            if (!auction.isActive() || auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá #" + auctionId + " đã kết thúc hoặc chưa mở!");
            }

            // 2. Không cho phép Seller tự đấu giá đồ của mình
            if (auction.getSellerId().equals(bidderId)) {
                throw new InvalidBidException("Người bán không được phép tự đấu giá sản phẩm của mình!");
            }

            // 3. Kiểm tra tính hợp lệ của giá đấu
            double minAllowed = auction.getCurrentHighestPrice() + auction.getMinBidIncrement();
            if (auction.getHighestBidderId() == null) {
                minAllowed = auction.getStartingPrice();
            }

            if (bidAmount < minAllowed) {
                throw new InvalidBidException(String.format(
                        "Giá đấu không hợp lệ! Tối thiểu phải là %,.0f VND (Giá hiện tại: %,.0f + Bước giá: %,.0f)",
                        minAllowed, auction.getCurrentHighestPrice(), auction.getMinBidIncrement()));
            }

            // 4. Cập nhật trạng thái phiên đấu giá
            auction.setCurrentHighestPrice(bidAmount);
            auction.setHighestBidderId(bidderId);
            auction.setHighestBidderUsername(bidderUsername);

            BidTransaction tx = new BidTransaction(null, auctionId, bidderId, bidderUsername, bidAmount);
            auction.addBidTransaction(tx);
            auctionDao.save(auction);

            log.info("Đặt giá thành công: Auction #{} | Người dẫn đầu: {} | Giá: {}",
                    auctionId, bidderUsername, bidAmount);

            // 5. Thuật toán Anti-sniping: Gia hạn nếu bid vào những giây cuối
            checkAndApplyAntiSniping(auction);

            // 6. Phát sự kiện realtime thông báo cho các Client (Observer Pattern)
            BroadcastEvent bidEvent = new BroadcastEvent(BroadcastEvent.EventType.NEW_BID,
                    auctionId, JsonUtils.toJson(tx));
            notifier.notifySubscribers(bidEvent);

            return tx;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Thuật toán Anti-sniping Algorithm:
     * Nếu có bid trong X giây cuối -> tự động gia hạn thêm Y giây.
     */
    private void checkAndApplyAntiSniping(Auction auction) {
        if (auction.getEndTime() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = java.time.Duration.between(now, auction.getEndTime()).getSeconds();

        if (secondsRemaining <= ANTI_SNIPING_WINDOW_SECONDS && secondsRemaining > 0) {
            auction.setEndTime(auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTEND_SECONDS));
            auctionDao.save(auction);

            log.info("Kích hoạt Anti-sniping cho Auction #{}: Gia hạn thêm {} giây! Hạn chót mới: {}",
                    auction.getId(), ANTI_SNIPING_EXTEND_SECONDS, auction.getEndTime());

            // Lập lại lịch kết thúc mới
            scheduleAuctionEnding(auction);

            BroadcastEvent extendEvent = new BroadcastEvent(BroadcastEvent.EventType.AUCTION_EXTENDED,
                    auction.getId(), JsonUtils.toJson(auction));
            notifier.notifySubscribers(extendEvent);
        }
    }

    /**
     * Lập lịch tự động đóng phiên khi hết thời gian
     */
    private void scheduleAuctionEnding(Auction auction) {
        if (auction.getEndTime() == null) {
            return;
        }

        ScheduledFuture<?> oldTask = scheduledTasks.remove(auction.getId());
        if (oldTask != null) {
            oldTask.cancel(false);
        }

        long delaySeconds = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (delaySeconds <= 0) {
            closeAuction(auction.getId());
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> closeAuction(auction.getId()),
                delaySeconds, TimeUnit.SECONDS);
        scheduledTasks.put(auction.getId(), future);
    }

    /**
     * Tự động đóng phiên và công bố người thắng cuộc
     */
    public void closeAuction(Long auctionId) {
        ReentrantLock lock = lockService.getLock(auctionId);
        lock.lock();
        try {
            Optional<Auction> opt = auctionDao.findById(auctionId);
            if (opt.isPresent()) {
                Auction auction = opt.get();
                if (auction.getStatus() == AuctionStatus.RUNNING || auction.getStatus() == AuctionStatus.OPEN) {
                    auction.setStatus(AuctionStatus.FINISHED);
                    auctionDao.save(auction);

                    log.info("Phiên đấu giá #{} ĐÃ KẾT THÚC. Người thắng cuộc: {} với giá: %,.0f VND",
                            auctionId,
                            auction.getHighestBidderUsername() != null ? auction.getHighestBidderUsername() : "Không ai đặt giá",
                            auction.getCurrentHighestPrice());

                    BroadcastEvent endEvent = new BroadcastEvent(BroadcastEvent.EventType.AUCTION_ENDED,
                            auctionId, JsonUtils.toJson(auction));
                    notifier.notifySubscribers(endEvent);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Quản trị viên (Admin) hủy phiên đấu giá vi phạm.
     */
    public boolean cancelAuction(Long auctionId) {
        ReentrantLock lock = lockService.getLock(auctionId);
        lock.lock();
        try {
            Optional<Auction> opt = auctionDao.findById(auctionId);
            if (opt.isPresent()) {
                Auction auction = opt.get();
                auction.setStatus(AuctionStatus.CANCELED);
                auctionDao.save(auction);

                ScheduledFuture<?> future = scheduledTasks.remove(auctionId);
                if (future != null) {
                    future.cancel(false);
                }

                BroadcastEvent cancelEvent = new BroadcastEvent(BroadcastEvent.EventType.STATUS_CHANGED,
                        auctionId, JsonUtils.toJson(auction));
                notifier.notifySubscribers(cancelEvent);
                log.info("Phiên đấu giá #{} ĐÃ BỊ HỦY BỎ BỞI ADMIN.", auctionId);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Quản trị viên (Admin) xóa vĩnh viễn phiên đấu giá khỏi hệ thống.
     */
    public boolean deleteAuction(Long auctionId) {
        ReentrantLock lock = lockService.getLock(auctionId);
        lock.lock();
        try {
            ScheduledFuture<?> future = scheduledTasks.remove(auctionId);
            if (future != null) {
                future.cancel(false);
            }
            boolean deleted = auctionDao.deleteById(auctionId);
            if (deleted) {
                log.info("Phiên đấu giá #{} ĐÃ BỊ XÓA BỞI ADMIN.", auctionId);
            }
            return deleted;
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
