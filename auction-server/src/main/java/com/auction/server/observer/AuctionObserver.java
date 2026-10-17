package com.auction.server.observer;

import com.auction.common.protocol.BroadcastEvent;

/**
 * Interface Observer đại diện cho đối tượng lắng nghe sự kiện từ phiên đấu giá.
 */
public interface AuctionObserver {
    void onAuctionEvent(BroadcastEvent event);
}
