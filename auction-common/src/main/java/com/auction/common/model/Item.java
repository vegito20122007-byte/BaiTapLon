package com.auction.common.model;

/**
 * Lớp sản phẩm đấu giá trừu tượng kế thừa Entity.
 */
public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    protected String name;
    protected String description;
    protected double startingPrice;
    protected Long sellerId;

    public Item() {
        super();
    }

    public Item(Long id, String name, String description, double startingPrice, Long sellerId) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    /**
     * Trả về danh mục sản phẩm (Electronics, Art, Vehicle, ...).
     */
    public abstract String getCategory();

    /**
     * Phương thức đa hình in chi tiết sản phẩm.
     */
    public abstract void printInfo();

    @Override
    public String getSummary() {
        return String.format("[%s] %s | Khởi điểm: %,.0f VND", getCategory(), name, startingPrice);
    }
}
