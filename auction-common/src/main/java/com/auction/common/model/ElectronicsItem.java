package com.auction.common.model;

/**
 * Lớp sản phẩm thiết bị điện tử kế thừa Item.
 */
public class ElectronicsItem extends Item {
    private static final long serialVersionUID = 1L;

    private String brand;
    private int warrantyMonths;

    public ElectronicsItem() {
        super();
        this.category = "Electronics";
    }

    public ElectronicsItem(Long id, String name, String description, double startingPrice,
                           Long sellerId, String brand, int warrantyMonths) {
        super(id, name, description, startingPrice, sellerId);
        this.category = "Electronics";
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getCategory() {
        return "Electronics";
    }

    @Override
    public void printInfo() {
        System.out.printf("[Thiết bị điện tử] %s | Thương hiệu: %s | Bảo hành: %d tháng | Khởi điểm: %,.0f VND%n",
                name, brand, warrantyMonths, startingPrice);
    }
}
