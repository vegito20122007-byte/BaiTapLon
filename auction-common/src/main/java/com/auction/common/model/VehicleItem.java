package com.auction.common.model;

/**
 * Lớp sản phẩm phương tiện giao thông kế thừa Item.
 */
public class VehicleItem extends Item {
    private static final long serialVersionUID = 1L;

    private String manufacturer;
    private int odometerKm;

    public VehicleItem() {
        super();
        this.category = "Vehicle";
    }

    public VehicleItem(Long id, String name, String description, double startingPrice,
                       Long sellerId, String manufacturer, int odometerKm) {
        super(id, name, description, startingPrice, sellerId);
        this.category = "Vehicle";
        this.manufacturer = manufacturer;
        this.odometerKm = odometerKm;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public int getOdometerKm() {
        return odometerKm;
    }

    public void setOdometerKm(int odometerKm) {
        this.odometerKm = odometerKm;
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }

    @Override
    public void printInfo() {
        System.out.printf("[Phương tiện] %s | Hãng: %s | Đã đi: %,d km | Khởi điểm: %,.0f VND%n",
                name, manufacturer, odometerKm, startingPrice);
    }
}
