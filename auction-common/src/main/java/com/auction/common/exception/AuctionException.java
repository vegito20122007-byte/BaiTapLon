package com.auction.common.exception;

/**
 * Base custom exception cho hệ thống đấu giá.
 */
public class AuctionException extends Exception {
    private static final long serialVersionUID = 1L;

    public AuctionException(String message) {
        super(message);
    }

    public AuctionException(String message, Throwable cause) {
        super(message, cause);
    }
}
