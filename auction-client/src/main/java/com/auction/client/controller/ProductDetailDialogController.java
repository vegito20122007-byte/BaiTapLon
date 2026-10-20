package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.common.enums.CommandType;
import com.auction.common.model.*;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.auction.common.util.JsonUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Controller cho Màn hình Chi tiết sản phẩm (Product Details View).
 * Đáp ứng yêu cầu mục 3.1.6: Hiển thị đầy đủ thông tin sản phẩm và áp dụng Đa hình (Polymorphism) OOP.
 */
public class ProductDetailDialogController {

    @FXML
    private Label lblTitle;

    @FXML
    private Label lblCategory;

    @FXML
    private Label lblStartingPrice;

    @FXML
    private Label lblCurrentPrice;

    @FXML
    private Label lblMinIncrement;

    @FXML
    private Label lblSeller;

    @FXML
    private Label lblHighestBidder;

    @FXML
    private Label lblStartTime;

    @FXML
    private Label lblEndTime;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblDescription;

    @FXML
    private VBox boxExtraAttributes;

    @FXML
    private Label lblExtra1;

    @FXML
    private Label lblExtra2;

    @FXML
    private Label lblPolymorphicSummary;

    private Long auctionId;
    private Auction currentAuction;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public void initData(Long auctionId) {
        this.auctionId = auctionId;
        new Thread(() -> {
            try {
                RequestMessage req = new RequestMessage(CommandType.GET_AUCTION_DETAIL, String.valueOf(auctionId));
                ResponseMessage res = SocketClient.getInstance().sendRequest(req);
                if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                    Auction auction = JsonUtils.fromJson(res.getDataJson(), Auction.class);
                    Platform.runLater(() -> setAuction(auction));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi tải chi tiết sản phẩm: " + e.getMessage()));
            }
        }).start();
    }

    public void setAuction(Auction auction) {
        this.currentAuction = auction;
        this.auctionId = auction.getId();

        lblTitle.setText(auction.getItemName());
        lblStartingPrice.setText(String.format("%,.0f VND", auction.getStartingPrice()));
        lblCurrentPrice.setText(String.format("%,.0f VND", auction.getCurrentHighestPrice()));
        lblMinIncrement.setText(String.format("%,.0f VND", auction.getMinBidIncrement()));
        lblSeller.setText(auction.getSellerUsername() != null ? auction.getSellerUsername() : "N/A");
        lblHighestBidder.setText(auction.getHighestBidderUsername() != null ? auction.getHighestBidderUsername() : "(Chưa có)");

        lblStartTime.setText(auction.getStartTime() != null ? auction.getStartTime().format(TIME_FMT) : "N/A");
        lblEndTime.setText(auction.getEndTime() != null ? auction.getEndTime().format(TIME_FMT) : "N/A");
        lblStatus.setText(auction.getStatus() != null ? auction.getStatus().name() : "N/A");
        lblDescription.setText(auction.getDescription() != null && !auction.getDescription().isEmpty()
                ? auction.getDescription() : "(Không có mô tả chi tiết)");

        Item item = auction.getItem();
        if (item != null) {
            lblCategory.setText(item.getCategory().toUpperCase());
            lblPolymorphicSummary.setText(item.getSummary());

            if (item instanceof ElectronicsItem) {
                ElectronicsItem elec = (ElectronicsItem) item;
                lblExtra1.setText("Thương hiệu: " + elec.getBrand());
                lblExtra2.setText(String.format("Thời hạn bảo hành: %d tháng", elec.getWarrantyMonths()));
            } else if (item instanceof ArtItem) {
                ArtItem art = (ArtItem) item;
                lblExtra1.setText("Nghệ sĩ / Họa sĩ: " + art.getArtist());
                lblExtra2.setText("Năm sáng tác: " + art.getYearCreated());
            } else if (item instanceof VehicleItem) {
                VehicleItem veh = (VehicleItem) item;
                lblExtra1.setText("Hãng sản xuất: " + veh.getManufacturer());
                lblExtra2.setText(String.format("Số km đã đi (odometer): %,d km", veh.getOdometerKm()));
            } else {
                lblExtra1.setText("Phân loại: " + item.getCategory());
                lblExtra2.setText("");
            }
        } else {
            lblCategory.setText("CHUNG");
            lblExtra1.setText("Thông tin phân loại: Mặc định");
            lblExtra2.setText("");
            lblPolymorphicSummary.setText(auction.getSummary());
        }
    }

    @FXML
    public void handleEnterRoom(ActionEvent event) {
        if (currentAuction == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-room.fxml"));
            Parent root = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.initAuctionData(currentAuction.getId());

            Stage stage = new Stage();
            stage.setTitle("Phòng đấu giá trực tiếp - " + currentAuction.getItemName());
            stage.setScene(new Scene(root, 850, 620));
            stage.setOnCloseRequest(e -> controller.cleanup());
            stage.show();

            handleClose(event);
        } catch (IOException e) {
            showAlert("Không thể mở phòng đấu giá: " + e.getMessage());
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) lblTitle.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
