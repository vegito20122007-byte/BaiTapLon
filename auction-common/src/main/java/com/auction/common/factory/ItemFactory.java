package com.auction.common.factory;

import com.auction.common.model.ArtItem;
import com.auction.common.model.ElectronicsItem;
import com.auction.common.model.Item;
import com.auction.common.model.VehicleItem;

import java.util.Map;

/**
 * Factory Method Pattern: Chuẩn hóa việc khởi tạo các phân loại Item khác nhau.
 * Giúp mở rộng thêm các danh mục mới mà không làm vỡ code client.
 */
public class ItemFactory {

    public static Item createItem(String category, Long id, String name, String description,
                                  double startingPrice, Long sellerId, Map<String, Object> extraParams) {
        if (category == null) {
            throw new IllegalArgumentException("Category không được để trống");
        }

        switch (category.trim().toUpperCase()) {
            case "ELECTRONICS":
                String brand = extraParams != null && extraParams.containsKey("brand") 
                        ? (String) extraParams.get("brand") : "Unknown Brand";
                int warranty = extraParams != null && extraParams.containsKey("warranty") 
                        ? ((Number) extraParams.get("warranty")).intValue() : 12;
                return new ElectronicsItem(id, name, description, startingPrice, sellerId, brand, warranty);

            case "ART":
                String artist = extraParams != null && extraParams.containsKey("artist") 
                        ? (String) extraParams.get("artist") : "Anonymous";
                int year = extraParams != null && extraParams.containsKey("year") 
                        ? ((Number) extraParams.get("year")).intValue() : 2000;
                return new ArtItem(id, name, description, startingPrice, sellerId, artist, year);

            case "VEHICLE":
                String manufacturer = extraParams != null && extraParams.containsKey("manufacturer") 
                        ? (String) extraParams.get("manufacturer") : "Unknown Maker";
                int odo = extraParams != null && extraParams.containsKey("odometer") 
                        ? ((Number) extraParams.get("odometer")).intValue() : 0;
                return new VehicleItem(id, name, description, startingPrice, sellerId, manufacturer, odo);

            default:
                throw new IllegalArgumentException("Loại sản phẩm không được hỗ trợ: " + category);
        }
    }
}
