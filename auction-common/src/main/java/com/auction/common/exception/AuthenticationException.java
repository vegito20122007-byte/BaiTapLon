package com.auction.common.exception;

/**
 * Ngoại lệ ném ra khi sai tài khoản, mật khẩu hoặc không có quyền truy cập.
 */
public class AuthenticationException extends AuctionException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
