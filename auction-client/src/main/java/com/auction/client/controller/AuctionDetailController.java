package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.common.enums.CommandType;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.protocol.BroadcastEvent;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.auction.common.util.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller cho Màn hình Đấu giá trực tiếp (Realtime Bidding Room) tích hợp:
 * 1. Observer Pattern cập nhật tức thì qua Socket
 * 2. Biểu đồ đường (LineChart) giá biến thiên theo thời gian thực (Tính năng nâng cao)
 * 3. Bắt sự kiện Anti-sniping gia hạn
 */
public class AuctionDetailController {

    @FXML
    private Label lblItemName;

    @FXML
    private Label lblCurrentPrice;

    @FXML
    private Label lblHighestBidder;

    @FXML
    private Label lblEndTime;

    @FXML
    private TextField txtBidAmount;

    @FXML
    private ListView<String> listBidHistory;

    @FXML
    private LineChart<String, Number> chartPriceCurve;

    private Long auctionId;
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private double minBidIncrement = 100000.0;
    private int bidCounter = 0;

    @FXML
    public void initialize() {
        listBidHistory.setItems(historyItems);
        priceSeries.setName("Diễn biến giá");
        chartPriceCurve.setAnimated(false);
        chartPriceCurve.getData().add(priceSeries);

        // Đăng ký nhận sự kiện realtime từ SocketClient
        SocketClient.getInstance().setEventListener(this::handleServerBroadcast);
    }

    public void initAuctionData(Long auctionId) {
        this.auctionId = auctionId;
        new Thread(() -> {
            try {
                // Đăng ký nhận sự kiện riêng của phòng này
                RequestMessage subReq = new RequestMessage(CommandType.SUBSCRIBE_AUCTION, String.valueOf(auctionId));
                SocketClient.getInstance().sendRequest(subReq);

                // Lấy thông tin chi tiết hiện tại
                RequestMessage req = new RequestMessage(CommandType.GET_AUCTION_DETAIL, String.valueOf(auctionId));
                ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                    Auction a = JsonUtils.fromJson(res.getDataJson(), Auction.class);
                    Platform.runLater(() -> updateAuctionUI(a));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi tải thông tin phòng: " + e.getMessage()));
            }
        }).start();
    }

    private void updateAuctionUI(Auction a) {
        lblItemName.setText(a.getItemName());
        lblCurrentPrice.setText(String.format("%,.0f VND", a.getCurrentHighestPrice()));
        lblHighestBidder.setText(a.getHighestBidderUsername() != null ? a.getHighestBidderUsername() : "(Chưa có)");
        lblEndTime.setText(a.getEndTime() != null ? a.getEndTime().toString() : "N/A");
        this.minBidIncrement = a.getMinBidIncrement() > 0 ? a.getMinBidIncrement() : 100000.0;

        // Gợi ý giá bid tiếp theo
        double nextBid = a.getCurrentHighestPrice() + minBidIncrement;
        txtBidAmount.setText(String.format("%.0f", nextBid));

        // Đồng bộ toàn bộ dữ liệu biểu đồ và danh sách lịch sử
        priceSeries.getData().clear();
        historyItems.clear();
        bidCounter = 0;

        // Điểm giá khởi điểm
        String startLabel = a.getStartTime() != null 
                ? a.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " (Bắt đầu)"
                : "Bắt đầu";
        priceSeries.getData().add(new XYChart.Data<>(startLabel, a.getStartingPrice()));

        // Nạp lại toàn bộ lịch sử các lượt đặt giá trước đó từ Server
        List<BidTransaction> history = a.getBidHistory();
        if (history != null && !history.isEmpty()) {
            for (BidTransaction tx : history) {
                bidCounter++;
                String timeStr = tx.getBidTime() != null 
                        ? tx.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String pointLabel = String.format("%s (#%d)", timeStr, bidCounter);

                priceSeries.getData().add(new XYChart.Data<>(pointLabel, tx.getBidAmount()));
                historyItems.add(0, String.format("[%s] %s đã trả: %,.0f VND",
                        timeStr, tx.getBidderUsername(), tx.getBidAmount()));
            }
        }
    }

    private void handleServerBroadcast(BroadcastEvent event) {
        if (!event.getAuctionId().equals(this.auctionId)) {
            return;
        }

        Platform.runLater(() -> {
            switch (event.getEventType()) {
                case NEW_BID:
                    BidTransaction tx = JsonUtils.fromJson(event.getEventDataJson(), BidTransaction.class);
                    lblCurrentPrice.setText(String.format("%,.0f VND", tx.getBidAmount()));
                    lblHighestBidder.setText(tx.getBidderUsername());

                    bidCounter++;
                    String timeStr = tx.getBidTime() != null 
                            ? tx.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                            : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String pointLabel = String.format("%s (#%d)", timeStr, bidCounter);

                    historyItems.add(0, String.format("[%s] %s đã trả: %,.0f VND",
                            timeStr, tx.getBidderUsername(), tx.getBidAmount()));

                    // Cập nhật biểu đồ giá Realtime (LineChart)
                    priceSeries.getData().add(new XYChart.Data<>(pointLabel, tx.getBidAmount()));

                    // Gợi ý mức bid tiếp theo
                    txtBidAmount.setText(String.format("%.0f", tx.getBidAmount() + minBidIncrement));
                    break;

                case AUCTION_EXTENDED:
                    Auction ext = JsonUtils.fromJson(event.getEventDataJson(), Auction.class);
                    lblEndTime.setText(ext.getEndTime().toString());
                    showAlert("⚡ [Anti-Sniping] Phiên đấu giá được gia hạn thêm do có người đặt giá ở phút chót!");
                    break;

                case AUCTION_ENDED:
                    Auction finished = JsonUtils.fromJson(event.getEventDataJson(), Auction.class);
                    showAlert("🏆 Phiên đấu giá đã kết thúc! Người thắng: " + finished.getHighestBidderUsername());
                    break;

                default:
                    break;
            }
        });
    }

    public void cleanup() {
        if (auctionId != null) {
            new Thread(() -> {
                try {
                    RequestMessage unsubReq = new RequestMessage(CommandType.UNSUBSCRIBE_AUCTION, String.valueOf(auctionId));
                    SocketClient.getInstance().sendRequest(unsubReq);
                } catch (Exception ignored) {
                }
            }).start();
        }
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        String amountStr = txtBidAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showAlert("Vui lòng nhập số tiền đấu giá!");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            JsonObject req = new JsonObject();
            req.addProperty("auctionId", auctionId);
            req.addProperty("bidAmount", amount);

            RequestMessage msg = new RequestMessage(CommandType.PLACE_BID, req.toString());
            ResponseMessage res = SocketClient.getInstance().sendRequest(msg);

            if (res.getStatus() == ResponseMessage.Status.ERROR) {
                showAlert(res.getMessage());
            }
        } catch (NumberFormatException e) {
            showAlert("Số tiền không hợp lệ!");
        } catch (Exception e) {
            showAlert("Lỗi gửi giá: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
