package com.auction.common.model;

/**
 * Lớp sản phẩm tác phẩm nghệ thuật kế thừa Item.
 */
public class ArtItem extends Item {
    private static final long serialVersionUID = 1L;

    private String artist;
    private int yearCreated;

    public ArtItem() {
        super();
        this.category = "Art";
    }

    public ArtItem(Long id, String name, String description, double startingPrice,
                   Long sellerId, String artist, int yearCreated) {
        super(id, name, description, startingPrice, sellerId);
        this.category = "Art";
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }

    @Override
    public String getCategory() {
        return "Art";
    }

    @Override
    public void printInfo() {
        System.out.printf("[Tác phẩm nghệ thuật] %s | Tác giả: %s (%d) | Khởi điểm: %,.0f VND%n",
                name, artist, yearCreated, startingPrice);
    }
}
