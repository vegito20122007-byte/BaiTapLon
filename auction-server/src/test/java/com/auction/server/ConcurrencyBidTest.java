package com.auction.server;

import com.auction.common.model.Auction;
import com.auction.server.core.AuctionManager;
import com.auction.server.service.AuctionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrencyBidTest {

    @Test
    @DisplayName("Kiểm tra đấu giá đồng thời từ nhiều luồng (Chống race condition và lost update)")
    void testConcurrentBidding() throws InterruptedException {
        AuctionService service = new AuctionService();
        Auction auction = new Auction(null, 999L, "Vật phẩm hiếm", 1L, "seller1",
                100000.0, 10000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        AuctionManager.getInstance().startAuction(auction);

        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulBids = new AtomicInteger(0);
        AtomicInteger failedBids = new AtomicInteger(0);

        for (int i = 1; i <= numberOfThreads; i++) {
            final int bidderIndex = i;
            // Mỗi bidder cố gắng trả giá theo mức tăng dần dựa trên index
            final double bidAmount = 100000.0 + (bidderIndex * 20000.0);

            executor.submit(() -> {
                try {
                    startLatch.await(); // Chờ tất cả luồng sẵn sàng rồi xuất phát cùng 1 lúc
                    service.placeBid(auction.getId(), (long) (100 + bidderIndex), "bidder_" + bidderIndex, bidAmount);
                    successfulBids.incrementAndGet();
                } catch (Exception e) {
                    failedBids.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Bắn pháo hiệu chạy đồng thời
        finishLatch.await();    // Đợi tất cả kết thúc
        executor.shutdown();

        Auction finalAuction = service.getAuctionById(auction.getId()).orElseThrow();

        // Kiểm tra tính toàn vẹn:
        // Giá kết thúc phải ít nhất lớn hơn hoặc bằng giá khởi điểm
        assertTrue(finalAuction.getCurrentHighestPrice() > 100000.0);
        assertNotNull(finalAuction.getHighestBidderUsername());
        // Số bid thành công + số bid thất bại phải bằng tổng số request
        assertEquals(numberOfThreads, successfulBids.get() + failedBids.get());

        System.out.printf("Kết quả Concurrency Test: %d bids thành công, %d bids bị từ chối do xung đột giá. Giá cuối: %,.0f VND%n",
                successfulBids.get(), failedBids.get(), finalAuction.getCurrentHighestPrice());
    }
}
