package com.auction.common.exception;

/**
 * Ngoại lệ ném ra khi cố tình đặt giá vào một phiên đã đóng hoặc bị hủy.
 */
public class AuctionClosedException extends AuctionException {
    private static final long serialVersionUID = 1L;

    public AuctionClosedException(String message) {
        super(message);
    }
}
