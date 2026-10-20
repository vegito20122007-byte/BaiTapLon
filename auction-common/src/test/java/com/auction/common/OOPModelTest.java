package com.auction.common;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.UserRole;
import com.auction.common.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class OOPModelTest {

    @Test
    @DisplayName("OOP: Kiểm tra tính kế thừa, bao đóng và đa hình của lớp Item")
    void testItemPolymorphismAndEncapsulation() {
        Item elec = new ElectronicsItem(1L, "iPhone 15", "Apple smartphone", 20000000.0, 10L, "Apple", 12);
        Item art = new ArtItem(2L, "Thiếu nữ bên hoa huệ", "Tranh sơn dầu", 100000000.0, 11L, "Tô Ngọc Vân", 1943);
        Item veh = new VehicleItem(3L, "VinFast VF8", "Xe điện SUV", 900000000.0, 12L, "VinFast", 5000);

        // Đa hình getCategory()
        assertEquals("Electronics", elec.getCategory());
        assertEquals("Art", art.getCategory());
        assertEquals("Vehicle", veh.getCategory());

        // Đa hình getSummary()
        assertTrue(elec.getSummary().contains("Electronics"));
        assertTrue(art.getSummary().contains("Art"));
        assertTrue(veh.getSummary().contains("Vehicle"));

        // Kiểm tra printInfo() không ném ngoại lệ
        assertDoesNotThrow(elec::printInfo);
        assertDoesNotThrow(art::printInfo);
        assertDoesNotThrow(veh::printInfo);

        // Kiểm tra tính bao đóng (Encapsulation)
        elec.setName("iPhone 16");
        assertEquals("iPhone 16", elec.getName());
        assertEquals(20000000.0, elec.getStartingPrice());
    }

    @Test
    @DisplayName("OOP: Kiểm tra tính kế thừa và đa hình của phân cấp User")
    void testUserPolymorphism() {
        User bidder = new Bidder(100L, "bidder1", "pass", "Nguyen Van A", "a@test.com", 50000000.0);
        User seller = new Seller(101L, "seller1", "pass", "Tran Van B", "b@test.com", 4.8);
        User admin = new Admin(102L, "admin", "pass", "Admin User", "admin@test.com", "IT");

        assertEquals(UserRole.BIDDER, bidder.getRole());
        assertEquals(UserRole.SELLER, seller.getRole());
        assertEquals(UserRole.ADMIN, admin.getRole());

        assertTrue(bidder.getSummary().contains("Bidder"));
        assertTrue(seller.getSummary().contains("Seller"));
        assertTrue(admin.getSummary().contains("Admin"));

        assertTrue(bidder instanceof Bidder);
        assertEquals(50000000.0, ((Bidder) bidder).getBalance());
        assertEquals(4.8, ((Seller) seller).getRating());
        assertEquals("IT", ((Admin) admin).getDepartment());
    }

    @Test
    @DisplayName("OOP: Kiểm tra Auction và BidTransaction")
    void testAuctionAndBidTransaction() {
        Auction auction = new Auction(1L, 10L, "Macbook Pro", 99L, "seller1", 20000000.0, 500000.0,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(20000000.0, auction.getCurrentHighestPrice());
        assertEquals(500000.0, auction.getMinBidIncrement());
        assertNull(auction.getHighestBidderId());

        BidTransaction tx = new BidTransaction(1L, 1L, 200L, "bidder1", 21000000.0);
        auction.addBidTransaction(tx);
        auction.setCurrentHighestPrice(21000000.0);
        auction.setHighestBidderId(200L);
        auction.setHighestBidderUsername("bidder1");

        assertEquals(1, auction.getBidHistory().size());
        assertEquals("bidder1", auction.getHighestBidderUsername());
        assertTrue(tx.getSummary().contains("bidder1"));
    }
}
