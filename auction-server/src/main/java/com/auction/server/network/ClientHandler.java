package com.auction.server.network;

import com.auction.common.enums.UserRole;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.model.User;
import com.auction.common.protocol.BroadcastEvent;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.auction.server.core.AuctionManager;
import com.auction.server.observer.AuctionObserver;
import com.auction.server.service.AuctionService;
import com.auction.server.service.UserService;
import com.auction.server.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Xử lý giao tiếp song song với từng Client qua Socket (Thread-per-client).
 * Đồng thời đóng vai trò là một AuctionObserver để nhận sự kiện realtime và push về cho Client.
 */
public class ClientHandler implements Runnable, AuctionObserver {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final UserService userService;
    private final AuctionService auctionService;
    private final ClientSessionManager sessionManager;

    private BufferedReader reader;
    private PrintWriter writer;
    private boolean isRunning = true;
    private User currentUser;

    public ClientHandler(Socket socket, UserService userService, AuctionService auctionService,
                         ClientSessionManager sessionManager) {
        this.socket = socket;
        this.userService = userService;
        this.auctionService = auctionService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        sessionManager.register(this);
        log.info("Client kết nối mới từ: {}", socket.getRemoteSocketAddress());

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            String line;
            while (isRunning && (line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                handleRequest(line.trim());
            }
        } catch (IOException e) {
            log.warn("Mất kết nối với client {}: {}", socket.getRemoteSocketAddress(), e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleRequest(String rawJson) {
        try {
            RequestMessage request = JsonUtils.fromJson(rawJson, RequestMessage.class);
            if (request == null || request.getCommand() == null) {
                sendResponse(ResponseMessage.error("Thông điệp không hợp lệ"));
                return;
            }

            switch (request.getCommand()) {
                case LOGIN:
                    handleLogin(request);
                    break;
                case REGISTER:
                    handleRegister(request);
                    break;
                case GET_AUCTIONS:
                    handleGetAuctions();
                    break;
                case GET_AUCTION_DETAIL:
                    handleGetAuctionDetail(request);
                    break;
                case CREATE_AUCTION:
                    handleCreateAuction(request);
                    break;
                case PLACE_BID:
                    handlePlaceBid(request);
                    break;
                case SET_AUTO_BID:
                    handleSetAutoBid(request);
                    break;
                case CANCEL_AUTO_BID:
                    handleCancelAutoBid(request);
                    break;
                case SUBSCRIBE_AUCTION:
                    handleSubscribe(request);
                    break;
                case UNSUBSCRIBE_AUCTION:
                    handleUnsubscribe(request);
                    break;
                case LOGOUT:
                    currentUser = null;
                    sendResponse(ResponseMessage.success("Đăng xuất thành công", null));
                    break;
                case UPDATE_AUCTION:
                    handleUpdateAuction(request);
                    break;
                case DELETE_SELLER_AUCTION:
                    handleDeleteSellerAuction(request);
                    break;
                case PAY_AUCTION:
                    handlePayAuction(request);
                    break;
                case GET_ALL_USERS:
                    handleGetAllUsers();
                    break;
                case DELETE_USER:
                    handleDeleteUser(request);
                    break;
                case CANCEL_AUCTION:
                    handleCancelAuction(request);
                    break;
                case DELETE_AUCTION:
                    handleDeleteAuction(request);
                    break;
                default:
                    sendResponse(ResponseMessage.error("Lệnh chưa được hỗ trợ: " + request.getCommand()));
            }
        } catch (Exception ex) {
            log.error("Lỗi xử lý request: {}", ex.getMessage(), ex);
            sendResponse(ResponseMessage.error("Lỗi máy chủ: " + ex.getMessage()));
        }
    }

    private void handleLogin(RequestMessage request) {
        try {
            JsonObject json = JsonUtils.getGson().fromJson(request.getPayloadJson(), JsonObject.class);
            String username = json.get("username").getAsString();
            String password = json.get("password").getAsString();

            String token = userService.login(username, password);
            currentUser = userService.getUserByToken(token);

            JsonObject result = new JsonObject();
            result.addProperty("token", token);
            result.add("user", JsonUtils.getGson().toJsonTree(currentUser));

            sendResponse(ResponseMessage.success("Đăng nhập thành công!", result.toString()));
        } catch (Exception e) {
            sendResponse(ResponseMessage.error(e.getMessage()));
        }
    }

    private void handleRegister(RequestMessage request) {
        try {
            JsonObject json = JsonUtils.getGson().fromJson(request.getPayloadJson(), JsonObject.class);
            String username = json.get("username").getAsString();
            String password = json.get("password").getAsString();
            String fullName = json.get("fullName").getAsString();
            String email = json.get("email").getAsString();
            String roleStr = json.get("role").getAsString();

            User user = userService.register(username, password, fullName, email,
                    com.auction.common.enums.UserRole.valueOf(roleStr.toUpperCase()));

            sendResponse(ResponseMessage.success("Đăng ký thành công!", JsonUtils.toJson(user)));
        } catch (Exception e) {
            sendResponse(ResponseMessage.error(e.getMessage()));
        }
    }

    private void handleGetAuctions() {
        List<Auction> list = auctionService.getAllAuctions();
        sendResponse(ResponseMessage.success("OK", JsonUtils.toJson(list)));
    }

    private void handleGetAuctionDetail(RequestMessage request) {
        Long auctionId = Long.parseLong(request.getPayloadJson());
        Optional<Auction> opt = auctionService.getAuctionById(auctionId);
        if (opt.isPresent()) {
            sendResponse(ResponseMessage.success("OK", JsonUtils.toJson(opt.get())));
        } else {
            sendResponse(ResponseMessage.error("Không tìm thấy phiên đấu giá #" + auctionId));
        }
    }

    private void handleCreateAuction(RequestMessage request) {
        if (currentUser == null) {
            sendResponse(ResponseMessage.error("Bạn chưa đăng nhập!"));
            return;
        }

        if (currentUser.getRole() != UserRole.SELLER && currentUser.getRole() != UserRole.ADMIN) {
            sendResponse(ResponseMessage.error("Chỉ tài khoản Người bán (Seller) hoặc Quản trị viên mới có quyền đăng sản phẩm đấu giá!"));
            return;
        }

        try {
            JsonObject json = JsonUtils.getGson().fromJson(request.getPayloadJson(), JsonObject.class);
            String category = json.has("category") ? json.get("category").getAsString() : "ELECTRONICS";
            String itemName = json.get("itemName").getAsString();
            String description = json.has("description") ? json.get("description").getAsString() : "";
            double startingPrice = json.get("startingPrice").getAsDouble();
            double increment = json.has("increment") ? json.get("increment").getAsDouble() : 100000.0;
            int durationMinutes = json.has("durationMinutes") ? json.get("durationMinutes").getAsInt() : 30;

            Map<String, Object> extraParams = new HashMap<>();
            if (json.has("extraParams") && !json.get("extraParams").isJsonNull()) {
                JsonObject extra = json.getAsJsonObject("extraParams");
                for (String key : extra.keySet()) {
                    JsonElement elem = extra.get(key);
                    if (elem.isJsonPrimitive()) {
                        JsonPrimitive prim = elem.getAsJsonPrimitive();
                        if (prim.isNumber()) {
                            extraParams.put(key, prim.getAsNumber());
                        } else if (prim.isBoolean()) {
                            extraParams.put(key, prim.getAsBoolean());
                        } else {
                            extraParams.put(key, prim.getAsString());
                        }
                    }
                }
            }

            Auction auction = auctionService.createAuction(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    category,
                    itemName,
                    description,
                    startingPrice,
                    increment,
                    durationMinutes,
                    extraParams
            );

            sendResponse(ResponseMessage.success("Đăng sản phẩm và tạo phiên đấu giá thành công!", JsonUtils.toJson(auction)));
        } catch (Exception e) {
            log.error("Lỗi tạo phiên đấu giá: {}", e.getMessage(), e);
            sendResponse(ResponseMessage.error("Lỗi tạo phiên: " + e.getMessage()));
        }
    }

    private void handlePlaceBid(RequestMessage request) {
        if (currentUser == null) {
            sendResponse(ResponseMessage.error("Bạn phải đăng nhập trước khi tham gia đấu giá!"));
            return;
        }

        try {
            JsonObject json = JsonUtils.getGson().fromJson(request.getPayloadJson(), JsonObject.class);
            Long auctionId = json.get("auctionId").getAsLong();
            double bidAmount = json.get("bidAmount").getAsDouble();

            BidTransaction tx = auctionService.placeBid(auctionId, currentUser.getId(), currentUser.getUsername(), bidAmount);
            sendResponse(ResponseMessage.success("Đặt giá thành công!", JsonUtils.toJson(tx)));
        } catch (Exception e) {
            sendResponse(ResponseMessage.error(e.getMessage()));
        }
    }

    private void handleSetAutoBid(RequestMessage request) {
        if (currentUser == null) {
            sendResponse(ResponseMessage.error("Bạn phải đăng nhập trước khi bật Auto-Bid!"));
            return;
        }
        try {
            JsonObject json = JsonUtils.getGson().fromJson(request.getPayloadJson(), JsonObject.class);
            Long auctionId = json.get("auctionId").getAsLong();
            double maxBid = json.get("maxBid").getAsDouble();
            double increment = json.has("increment") ? json.get("increment").getAsDouble() : 100000.0;

            auctionService.registerAutoBid(auctionId, currentUser.getId(), currentUser.getUsername(), maxBid, increment);
            sendResponse(ResponseMessage.success(String.format("Đã kích hoạt Auto-Bid thành công! (Giá trần: %,.0f VND)", maxBid), null));
        } catch (Exception e) {
            sendResponse(ResponseMessage.error("Lỗi cài đặt Auto-Bid: " + e.getMessage()));
        }
    }

    private void handleCancelAutoBid(RequestMessage request) {
        if (currentUser == null) {
            sendResponse(ResponseMessage.error("Bạn chưa đăng nhập!"));
            return;
        }
        try {
            Long auctionId = Long.parseLong(request.getPayloadJson());
            auctionService.cancelAutoBid(auctionId, currentUser.getId());
            sendResponse(ResponseMessage.success("Đã hủy chế độ Auto-Bid thành công!", null));
        } catch (Exception e) {
            sendResponse(ResponseMessage.error("Lỗi hủy Auto-Bid: " + e.getMessage()));
        }
    }

    private void handleUpdateAuction(RequestMessage request) {
        if (currentUser == null) {
            sendResponse(ResponseMessage.error("Bạn chưa đăng nhập!"));
            return;
        }
        try {
            JsonObject json = JsonUtils.getGson().fromJson(request.getPayloadJson(), JsonObject.class);
            Long auctionId = json.get("auctionId").getAsLong();
            String itemName = json.get("itemName").getAsString();
            String description = json.has("description") ? json.get("description").getAsString() : "";
            double startingPrice = json.get("startingPrice").getAsDouble();
            double increment = json.has("increment") ? json.get("increment").getAsDouble() : 100000.0;
            int durationMinutes = json.has("durationMinutes") ? json.get("durationMinutes").getAsInt() : 30;

            boolean ok = auctionService.updateAuction(auctionId, currentUser.getId(), itemName, description,
                    startingPrice, increment, durationMinutes);
            if (ok) {
                sendResponse(ResponseMessage.success("Cập nhật thông tin sản phẩm và phiên đấu giá thành công!", null));
            } else {
                sendResponse(ResponseMessage.error("Không thể cập nhật phiên #" + auctionId));
            }
        } catch (Exception e) {
            sendResponse(ResponseMessage.error("Lỗi cập nhật: " + e.getMessage()));
        }
    }

    private void handleDeleteSellerAuction(RequestMessage request) {
        if (currentUser == null) {
            sendResponse(ResponseMessage.error("Bạn chưa đăng nhập!"));
            return;
        }
        try {
            Long auctionId = Long.parseLong(request.getPayloadJson());
            boolean ok = auctionService.deleteSellerAuction(auctionId, currentUser.getId());
            if (ok) {
                sendResponse(ResponseMessage.success("Đã xóa sản phẩm/phiên đấu giá thành công!", null));
            } else {
                sendResponse(ResponseMessage.error("Không thể xóa phiên #" + auctionId));
            }
        } catch (Exception e) {
            sendResponse(ResponseMessage.error("Lỗi xóa sản phẩm: " + e.getMessage()));
        }
    }

    private void handlePayAuction(RequestMessage request) {
        if (currentUser == null) {
            sendResponse(ResponseMessage.error("Bạn chưa đăng nhập!"));
            return;
        }
        try {
            Long auctionId = Long.parseLong(request.getPayloadJson());
            boolean ok = auctionService.payAuction(auctionId, currentUser.getId());
            if (ok) {
                sendResponse(ResponseMessage.success("Xác nhận thanh toán thành công (Trạng thái PAID)!", null));
            } else {
                sendResponse(ResponseMessage.error("Không thể hoàn tất thanh toán phiên #" + auctionId));
            }
        } catch (Exception e) {
            sendResponse(ResponseMessage.error("Lỗi thanh toán: " + e.getMessage()));
        }
    }

    private void handleSubscribe(RequestMessage request) {
        try {
            Long auctionId = Long.parseLong(request.getPayloadJson());
            AuctionManager.getInstance().getNotifier().subscribe(auctionId, this);
            sendResponse(ResponseMessage.success("Subscribed", null));
        } catch (Exception e) {
            sendResponse(ResponseMessage.error(e.getMessage()));
        }
    }

    private void handleUnsubscribe(RequestMessage request) {
        try {
            Long auctionId = Long.parseLong(request.getPayloadJson());
            AuctionManager.getInstance().getNotifier().unsubscribe(auctionId, this);
            sendResponse(ResponseMessage.success("Unsubscribed", null));
        } catch (Exception e) {
            sendResponse(ResponseMessage.error(e.getMessage()));
        }
    }

    private void handleGetAllUsers() {
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            sendResponse(ResponseMessage.error("Quyền truy cập bị từ chối! Chỉ Admin mới được thực hiện thao tác này."));
            return;
        }
        List<User> users = userService.getAllUsers();
        sendResponse(ResponseMessage.success("OK", JsonUtils.toJson(users)));
    }

    private void handleDeleteUser(RequestMessage request) {
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            sendResponse(ResponseMessage.error("Quyền truy cập bị từ chối! Chỉ Admin mới được thực hiện thao tác này."));
            return;
        }
        try {
            Long userId = Long.parseLong(request.getPayloadJson());
            if (currentUser.getId().equals(userId)) {
                sendResponse(ResponseMessage.error("Không thể tự xóa tài khoản Quản trị viên đang đăng nhập!"));
                return;
            }
            boolean deleted = userService.deleteUser(userId);
            if (deleted) {
                sendResponse(ResponseMessage.success("Đã xóa tài khoản người dùng thành công!", null));
            } else {
                sendResponse(ResponseMessage.error("Không tìm thấy người dùng có ID: " + userId));
            }
        } catch (Exception e) {
            sendResponse(ResponseMessage.error("Lỗi xóa người dùng: " + e.getMessage()));
        }
    }

    private void handleCancelAuction(RequestMessage request) {
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            sendResponse(ResponseMessage.error("Quyền truy cập bị từ chối! Chỉ Admin mới được thực hiện thao tác này."));
            return;
        }
        try {
            Long auctionId = Long.parseLong(request.getPayloadJson());
            boolean canceled = auctionService.cancelAuction(auctionId);
            if (canceled) {
                sendResponse(ResponseMessage.success("Đã hủy bỏ phiên đấu giá #" + auctionId + " thành công!", null));
            } else {
                sendResponse(ResponseMessage.error("Không tìm thấy phiên đấu giá #" + auctionId));
            }
        } catch (Exception e) {
            sendResponse(ResponseMessage.error("Lỗi hủy phiên: " + e.getMessage()));
        }
    }

    private void handleDeleteAuction(RequestMessage request) {
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            sendResponse(ResponseMessage.error("Quyền truy cập bị từ chối! Chỉ Admin mới được thực hiện thao tác này."));
            return;
        }
        try {
            Long auctionId = Long.parseLong(request.getPayloadJson());
            boolean deleted = auctionService.deleteAuction(auctionId);
            if (deleted) {
                sendResponse(ResponseMessage.success("Đã xóa vĩnh viễn phiên đấu giá #" + auctionId + "!", null));
            } else {
                sendResponse(ResponseMessage.error("Không tìm thấy phiên đấu giá #" + auctionId));
            }
        } catch (Exception e) {
            sendResponse(ResponseMessage.error("Lỗi xóa phiên: " + e.getMessage()));
        }
    }

    public synchronized void sendResponse(ResponseMessage response) {
        if (writer != null) {
            writer.println(JsonUtils.toJson(response));
        }
    }

    @Override
    public synchronized void onAuctionEvent(BroadcastEvent event) {
        // Observer Pattern: Push thông báo event realtime thẳng về Socket cho Client
        if (writer != null) {
            writer.println(JsonUtils.toJson(event));
        }
    }

    private void cleanup() {
        isRunning = false;
        AuctionManager.getInstance().getNotifier().removeObserver(this);
        sessionManager.unregister(this);
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        log.info("Client ngắt kết nối: {}", socket.getRemoteSocketAddress());
    }
}
