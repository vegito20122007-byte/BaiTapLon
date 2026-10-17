package com.auction.common.exception;

/**
 * Ngoại lệ ném ra khi giá đấu không hợp lệ (thấp hơn giá hiện tại, bước giá không đủ, ...).
 */
public class InvalidBidException extends AuctionException {
    private static final long serialVersionUID = 1L;

    public InvalidBidException(String message) {
        super(message);
    }
}
