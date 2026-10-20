package com.auction.server;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.Auction;
import com.auction.server.core.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionLifecycleTest {

    private AuctionManager auctionManager;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
    }

    @Test
    @DisplayName("Vòng đời phiên đấu giá: OPEN -> RUNNING -> FINISHED -> PAID")
    void testAuctionFullLifecycle() throws Exception {
        Auction auction = new Auction(null, 100L, "Xe máy SH", 88L, "seller_sh",
                70000000.0, 1000000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));

        // 1. OPEN ban đầu
        assertEquals(AuctionStatus.OPEN, auction.getStatus());

        // 2. RUNNING khi bắt đầu
        auctionManager.startAuction(auction);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());

        // Đặt giá hợp lệ
        auctionManager.placeBid(auction.getId(), 500L, "buyer_sh", 75000000.0);

        // 3. FINISHED khi đóng phiên
        auctionManager.closeAuction(auction.getId());
        Auction closed = auctionManager.getAuctionDao().findById(auction.getId()).orElseThrow();
        assertEquals(AuctionStatus.FINISHED, closed.getStatus());
        assertEquals("buyer_sh", closed.getHighestBidderUsername());

        // 4. PAID khi hoàn tất thanh toán
        boolean paid = auctionManager.payAuction(auction.getId(), 500L);
        assertTrue(paid);
        Auction paidAuction = auctionManager.getAuctionDao().findById(auction.getId()).orElseThrow();
        assertEquals(AuctionStatus.PAID, paidAuction.getStatus());
    }

    @Test
    @DisplayName("Hủy phiên đấu giá chuyển sang trạng thái CANCELED")
    void testCancelAuctionLifecycle() {
        Auction auction = new Auction(null, 101L, "Laptop hỏng", 88L, "seller_sh",
                5000000.0, 100000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(10));
        auctionManager.startAuction(auction);

        boolean canceled = auctionManager.cancelAuction(auction.getId());
        assertTrue(canceled);

        Auction canceledAuction = auctionManager.getAuctionDao().findById(auction.getId()).orElseThrow();
        assertEquals(AuctionStatus.CANCELED, canceledAuction.getStatus());
    }

    @Test
    @DisplayName("Anti-sniping: Tự động gia hạn thời gian kết thúc khi có bid ở 30 giây cuối")
    void testAntiSnipingExtension() throws Exception {
        // Tạo phiên có thời gian kết thúc chỉ còn 20 giây nữa
        LocalDateTime nearEndTime = LocalDateTime.now().plusSeconds(20);
        Auction auction = new Auction(null, 102L, "Đồng hồ cổ", 88L, "seller_sh",
                1000000.0, 50000.0,
                LocalDateTime.now().minusMinutes(5), nearEndTime);
        auctionManager.startAuction(auction);

        // Đặt giá trong cửa sổ 20 giây cuối (<= 30 giây)
        auctionManager.placeBid(auction.getId(), 501L, "sniper_bidder", 1100000.0);

        Auction updated = auctionManager.getAuctionDao().findById(auction.getId()).orElseThrow();
        // Thời gian kết thúc phải được dời thêm ít nhất 50 giây (20 + 60 = 80 giây từ thời điểm bắt đầu)
        long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), updated.getEndTime()).getSeconds();
        assertTrue(secondsRemaining > 30, "Thời gian kết thúc phải được gia hạn lớn hơn 30 giây");
    }

    @Test
    @DisplayName("Seller CRUD: Chỉnh sửa và xóa sản phẩm thành công khi chưa có ai đặt giá")
    void testSellerEditAndDeleteAuction() throws Exception {
        Auction auction = new Auction(null, 103L, "Bàn ghế gỗ", 77L, "seller_wood",
                3000000.0, 100000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        auctionManager.startAuction(auction);

        // Seller chỉnh sửa
        boolean updated = auctionManager.updateAuction(auction.getId(), 77L, "Bàn ghế gỗ Hương cao cấp",
                "Gỗ nguyên khối cực đẹp", 3500000.0, 200000.0, 45);
        assertTrue(updated);

        Auction updatedAuc = auctionManager.getAuctionDao().findById(auction.getId()).orElseThrow();
        assertEquals("Bàn ghế gỗ Hương cao cấp", updatedAuc.getItemName());
        assertEquals(3500000.0, updatedAuc.getStartingPrice());

        // Seller xóa sản phẩm
        boolean deleted = auctionManager.deleteSellerAuction(auction.getId(), 77L);
        assertTrue(deleted);
        assertTrue(auctionManager.getAuctionDao().findById(auction.getId()).isEmpty());
    }

    @Test
    @DisplayName("Seller không được phép sửa sản phẩm khi đã có người đặt giá")
    void testSellerCannotEditWhenBidExists() throws Exception {
        Auction auction = new Auction(null, 104L, "Điện thoại", 77L, "seller_wood",
                2000000.0, 50000.0,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        auctionManager.startAuction(auction);

        auctionManager.placeBid(auction.getId(), 502L, "buyer_wood", 2100000.0);

        assertThrows(InvalidBidException.class, () -> {
            auctionManager.updateAuction(auction.getId(), 77L, "Tên mới", "Mô tả", 2500000.0, 100000.0, 30);
        });
    }
}
