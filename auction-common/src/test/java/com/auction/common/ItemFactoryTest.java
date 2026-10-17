package com.auction.common;

import com.auction.common.factory.ItemFactory;
import com.auction.common.model.ArtItem;
import com.auction.common.model.ElectronicsItem;
import com.auction.common.model.Item;
import com.auction.common.model.VehicleItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ItemFactoryTest {

    @Test
    @DisplayName("Kiểm tra Factory Method tạo ElectronicsItem thành công")
    void testCreateElectronicsItem() {
        Map<String, Object> params = new HashMap<>();
        params.put("brand", "Apple");
        params.put("warranty", 24);

        Item item = ItemFactory.createItem("ELECTRONICS", 1L, "MacBook Pro", "Laptop xịn", 30000000.0, 10L, params);

        assertNotNull(item);
        assertTrue(item instanceof ElectronicsItem);
        assertEquals("Electronics", item.getCategory());
        assertEquals("MacBook Pro", item.getName());
        assertEquals("Apple", ((ElectronicsItem) item).getBrand());
        assertEquals(24, ((ElectronicsItem) item).getWarrantyMonths());
    }

    @Test
    @DisplayName("Kiểm tra Factory Method tạo ArtItem thành công")
    void testCreateArtItem() {
        Map<String, Object> params = new HashMap<>();
        params.put("artist", "Leonardo da Vinci");
        params.put("year", 1503);

        Item item = ItemFactory.createItem("ART", 2L, "Mona Lisa", "Tranh sơn dầu", 500000000.0, 11L, params);

        assertNotNull(item);
        assertTrue(item instanceof ArtItem);
        assertEquals("Art", item.getCategory());
        assertEquals("Leonardo da Vinci", ((ArtItem) item).getArtist());
    }

    @Test
    @DisplayName("Kiểm tra Factory Method tạo VehicleItem thành công")
    void testCreateVehicleItem() {
        Map<String, Object> params = new HashMap<>();
        params.put("manufacturer", "Toyota");
        params.put("odometer", 15000);

        Item item = ItemFactory.createItem("VEHICLE", 3L, "Camry 2.5Q", "Xe lướt đẹp", 800000000.0, 12L, params);

        assertNotNull(item);
        assertTrue(item instanceof VehicleItem);
        assertEquals("Vehicle", item.getCategory());
        assertEquals(15000, ((VehicleItem) item).getOdometerKm());
    }

    @Test
    @DisplayName("Kiểm tra ngoại lệ khi truyền loại sản phẩm không hỗ trợ")
    void testCreateUnknownItemThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("UNKNOWN_CATEGORY", 4L, "Unknown", "Desc", 1000.0, 1L, null);
        });
    }
}
