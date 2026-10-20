package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.ClientSession;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.CommandType;
import com.auction.common.enums.UserRole;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.auction.common.util.JsonUtils;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * Controller cho Dashboard danh sách phiên đấu giá (MVC Pattern).
 * Điều phối các chức năng chính: Xem danh sách, Vào phòng đấu giá trực tiếp,
 * Đăng sản phẩm mới, Chi tiết sản phẩm, Quản lý Seller, Quản trị Admin, Thanh toán PAID.
 */
public class MainDashboardController {

    @FXML
    private Label lblWelcome;

    @FXML
    private Button btnSellerPanel;

    @FXML
    private Button btnCreateAuction;

    @FXML
    private Button btnAdminPanel;

    @FXML
    private Button btnCancelAuction;

    @FXML
    private Button btnPayAuction;

    @FXML
    private TableView<Auction> tableAuctions;

    @FXML
    private TableColumn<Auction, Long> colId;

    @FXML
    private TableColumn<Auction, String> colItemName;

    @FXML
    private TableColumn<Auction, Double> colCurrentPrice;

    @FXML
    private TableColumn<Auction, String> colHighestBidder;

    @FXML
    private TableColumn<Auction, String> colStatus;

    private final ObservableList<Auction> auctionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User user = ClientSession.getInstance().getCurrentUser();
        if (user != null) {
            lblWelcome.setText("Xin chào: " + user.getFullName() + " (" + user.getRole() + ")");
            boolean isSellerOrAdmin = user.getRole() == UserRole.SELLER || user.getRole() == UserRole.ADMIN;
            btnCreateAuction.setVisible(isSellerOrAdmin);
            btnCreateAuction.setManaged(isSellerOrAdmin);
            btnSellerPanel.setVisible(isSellerOrAdmin);
            btnSellerPanel.setManaged(isSellerOrAdmin);

            boolean isAdmin = user.getRole() == UserRole.ADMIN;
            btnAdminPanel.setVisible(isAdmin);
            btnAdminPanel.setManaged(isAdmin);
            btnCancelAuction.setVisible(isAdmin);
            btnCancelAuction.setManaged(isAdmin);
        } else {
            btnCreateAuction.setVisible(false);
            btnCreateAuction.setManaged(false);
            btnSellerPanel.setVisible(false);
            btnSellerPanel.setManaged(false);
            btnAdminPanel.setVisible(false);
            btnAdminPanel.setManaged(false);
            btnCancelAuction.setVisible(false);
            btnCancelAuction.setManaged(false);
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentHighestPrice"));
        colHighestBidder.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getHighestBidderUsername() != null ? cell.getValue().getHighestBidderUsername() : "Chưa có"));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        tableAuctions.setItems(auctionList);
        loadAuctions();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadAuctions();
    }

    public void loadAuctions() {
        new Thread(() -> {
            try {
                RequestMessage req = new RequestMessage(CommandType.GET_AUCTIONS, null);
                ResponseMessage res = SocketClient.getInstance().sendRequest(req);
                if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                    Type listType = new TypeToken<List<Auction>>() {}.getType();
                    List<Auction> list = JsonUtils.getGson().fromJson(res.getDataJson(), listType);

                    Platform.runLater(() -> {
                        auctionList.clear();
                        if (list != null) {
                            auctionList.addAll(list);
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi tải danh sách: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleJoinRoom(ActionEvent event) {
        Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn một phiên đấu giá trong bảng!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-room.fxml"));
            Parent root = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.initAuctionData(selected.getId());

            Stage stage = new Stage();
            stage.setTitle("Phòng đấu giá trực tiếp - " + selected.getItemName());
            stage.setScene(new Scene(root, 850, 620));
            stage.setOnCloseRequest(e -> controller.cleanup());
            stage.show();
        } catch (IOException e) {
            showAlert("Không thể mở phòng đấu giá: " + e.getMessage());
        }
    }

    @FXML
    public void handleViewProductDetails(ActionEvent event) {
        Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn một sản phẩm trong danh sách để xem chi tiết!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product-detail.fxml"));
            Parent root = loader.load();

            ProductDetailDialogController controller = loader.getController();
            controller.initData(selected.getId());

            Stage stage = new Stage();
            stage.setTitle("Chi tiết sản phẩm - " + selected.getItemName());
            stage.setScene(new Scene(root, 540, 650));
            stage.initOwner(lblWelcome.getScene().getWindow());
            stage.show();
        } catch (IOException e) {
            showAlert("Không thể mở màn hình chi tiết: " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenCreateAuction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-auction.fxml"));
            Parent root = loader.load();

            CreateAuctionController controller = loader.getController();
            controller.setOnCreatedCallback(this::loadAuctions);

            Stage stage = new Stage();
            stage.setTitle("Đăng bán sản phẩm đấu giá mới");
            stage.setScene(new Scene(root, 560, 660));
            stage.initOwner(btnCreateAuction.getScene().getWindow());
            stage.show();
        } catch (IOException e) {
            showAlert("Không thể mở giao diện đăng sản phẩm: " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenSellerPanel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/seller-management.fxml"));
            Parent root = loader.load();

            SellerManagementController controller = loader.getController();
            controller.setOnDataChangedCallback(this::loadAuctions);

            Stage stage = new Stage();
            stage.setTitle("Quản lý sản phẩm & phiên đấu giá (Seller)");
            stage.setScene(new Scene(root, 950, 600));
            stage.initOwner(lblWelcome.getScene().getWindow());
            stage.show();
        } catch (IOException e) {
            showAlert("Không thể mở bảng quản lý Seller: " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenAdminPanel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin-management.fxml"));
            Parent root = loader.load();

            AdminManagementController controller = loader.getController();
            controller.setOnDataChangedCallback(this::loadAuctions);

            Stage stage = new Stage();
            stage.setTitle("Trung tâm Quản trị hệ thống (Admin)");
            stage.setScene(new Scene(root, 950, 650));
            stage.initOwner(lblWelcome.getScene().getWindow());
            stage.show();
        } catch (IOException e) {
            showAlert("Không thể mở bảng quản trị Admin: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelAuction(ActionEvent event) {
        Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn một phiên đấu giá cần hủy!");
            return;
        }

        if (selected.getStatus() == AuctionStatus.CANCELED || selected.getStatus() == AuctionStatus.FINISHED) {
            showAlert("Phiên đấu giá này đã kết thúc hoặc đã bị hủy trước đó!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận hủy phiên (Admin)");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn hủy phiên đấu giá #" + selected.getId() + " (" + selected.getItemName() + ")?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    RequestMessage req = new RequestMessage(CommandType.CANCEL_AUCTION, String.valueOf(selected.getId()));
                    ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        showAlert(res.getMessage());
                        if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                            loadAuctions();
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi hủy phiên: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handlePayAuction(ActionEvent event) {
        Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn phiên đấu giá cần thanh toán!");
            return;
        }

        if (selected.getStatus() != AuctionStatus.FINISHED) {
            showAlert("Chỉ có thể thanh toán khi phiên đã kết thúc (FINISHED) và có người thắng cuộc!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thanh toán (PAID)");
        confirm.setHeaderText(null);
        confirm.setContentText(String.format("Xác nhận thanh toán số tiền %,.0f VND cho sản phẩm '%s'?\nTrạng thái phiên sẽ chuyển sang PAID.",
                selected.getCurrentHighestPrice(), selected.getItemName()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    RequestMessage req = new RequestMessage(CommandType.PAY_AUCTION, String.valueOf(selected.getId()));
                    ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        showAlert(res.getMessage());
                        if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                            loadAuctions();
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi thanh toán: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        new Thread(() -> {
            try {
                RequestMessage req = new RequestMessage(CommandType.LOGOUT, null);
                SocketClient.getInstance().sendRequest(req);
            } catch (Exception ignored) {
            }
        }).start();

        ClientSession.getInstance().clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Hệ thống Đấu giá trực tuyến - Đăng nhập");
            stage.setScene(new Scene(root, 420, 520));
            stage.show();

            Stage currentStage = (Stage) lblWelcome.getScene().getWindow();
            if (currentStage != null) {
                currentStage.close();
            }
        } catch (IOException e) {
            showAlert("Không thể mở màn hình đăng nhập: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
