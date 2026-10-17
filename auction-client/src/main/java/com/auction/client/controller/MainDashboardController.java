package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.ClientSession;
import com.auction.common.enums.CommandType;
import com.auction.common.enums.UserRole;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.auction.common.util.JsonUtils;
import com.google.gson.Gson;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * Controller cho Dashboard danh sách phiên đấu giá (MVC Pattern).
 */
public class MainDashboardController {

    @FXML
    private Label lblWelcome;

    @FXML
    private Button btnCreateAuction;

    @FXML
    private Button btnAdminPanel;

    @FXML
    private Button btnCancelAuction;

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

            boolean isAdmin = user.getRole() == UserRole.ADMIN;
            btnAdminPanel.setVisible(isAdmin);
            btnAdminPanel.setManaged(isAdmin);
            btnCancelAuction.setVisible(isAdmin);
            btnCancelAuction.setManaged(isAdmin);
        } else {
            btnCreateAuction.setVisible(false);
            btnCreateAuction.setManaged(false);
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

    private void loadAuctions() {
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
            stage.setScene(new Scene(root, 800, 600));
            stage.setOnCloseRequest(e -> controller.cleanup());
            stage.show();
        } catch (IOException e) {
            showAlert("Không thể mở phòng đấu giá: " + e.getMessage());
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

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
