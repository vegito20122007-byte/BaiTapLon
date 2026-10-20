package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.ClientSession;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.CommandType;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.auction.common.util.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller cho Màn hình Quản lý sản phẩm (dành cho Seller).
 * Đáp ứng yêu cầu mục 3.1.2 & 3.1.6: Quản lý sản phẩm đấu giá (CRUD: Thêm / Sửa / Xóa)
 * và chuyển trạng thái FINISHED -> PAID.
 */
public class SellerManagementController {

    @FXML
    private Label lblSellerInfo;

    @FXML
    private TableView<Auction> tableMyAuctions;

    @FXML
    private TableColumn<Auction, Long> colId;

    @FXML
    private TableColumn<Auction, String> colItemName;

    @FXML
    private TableColumn<Auction, Double> colStartingPrice;

    @FXML
    private TableColumn<Auction, Double> colCurrentPrice;

    @FXML
    private TableColumn<Auction, String> colHighestBidder;

    @FXML
    private TableColumn<Auction, String> colStatus;

    @FXML
    private TableColumn<Auction, String> colEndTime;

    private final ObservableList<Auction> myAuctionsList = FXCollections.observableArrayList();
    private Runnable onDataChangedCallback;

    @FXML
    public void initialize() {
        User user = ClientSession.getInstance().getCurrentUser();
        if (user != null) {
            lblSellerInfo.setText(String.format("Người bán: %s (%s)", user.getFullName(), user.getUsername()));
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentHighestPrice"));
        colHighestBidder.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getHighestBidderUsername() != null ? cell.getValue().getHighestBidderUsername() : "(Chưa có)"));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStatus() != null ? cell.getValue().getStatus().name() : ""));
        colEndTime.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getEndTime() != null ? cell.getValue().getEndTime().toString() : "N/A"));

        tableMyAuctions.setItems(myAuctionsList);
        loadMyAuctions();
    }

    public void setOnDataChangedCallback(Runnable callback) {
        this.onDataChangedCallback = callback;
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadMyAuctions();
    }

    public void loadMyAuctions() {
        User user = ClientSession.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        new Thread(() -> {
            try {
                RequestMessage req = new RequestMessage(CommandType.GET_AUCTIONS, null);
                ResponseMessage res = SocketClient.getInstance().sendRequest(req);
                if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                    Type listType = new TypeToken<List<Auction>>() {}.getType();
                    List<Auction> all = JsonUtils.getGson().fromJson(res.getDataJson(), listType);

                    List<Auction> mine = all.stream()
                            .filter(a -> user.getId().equals(a.getSellerId()))
                            .collect(Collectors.toList());

                    Platform.runLater(() -> {
                        myAuctionsList.clear();
                        if (mine != null) {
                            myAuctionsList.addAll(mine);
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi tải danh sách sản phẩm: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleCreateNew(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-auction.fxml"));
            Parent root = loader.load();

            CreateAuctionController controller = loader.getController();
            controller.setOnCreatedCallback(() -> {
                loadMyAuctions();
                if (onDataChangedCallback != null) {
                    onDataChangedCallback.run();
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Đăng bán sản phẩm đấu giá mới");
            stage.setScene(new Scene(root, 560, 660));
            stage.show();
        } catch (IOException e) {
            showAlert("Không thể mở form đăng sản phẩm: " + e.getMessage());
        }
    }

    @FXML
    public void handleEditAuction(ActionEvent event) {
        Auction selected = tableMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn một sản phẩm trong danh sách để chỉnh sửa!");
            return;
        }

        if (selected.getHighestBidderId() != null) {
            showAlert("Sản phẩm này đã có người đặt giá, không thể chỉnh sửa thông tin!");
            return;
        }

        if (selected.getStatus() == AuctionStatus.FINISHED || selected.getStatus() == AuctionStatus.CANCELED
                || selected.getStatus() == AuctionStatus.PAID) {
            showAlert("Phiên đấu giá đã kết thúc hoặc bị hủy, không thể chỉnh sửa!");
            return;
        }

        // Tạo dialog sửa sản phẩm nhanh
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa thông tin sản phẩm #" + selected.getId());
        dialog.setHeaderText("Cập nhật thông tin cho sản phẩm: " + selected.getItemName());

        ButtonType saveBtnType = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtName = new TextField(selected.getItemName());
        TextArea txtDesc = new TextArea(selected.getDescription() != null ? selected.getDescription() : "");
        txtDesc.setPrefRowCount(3);
        TextField txtPrice = new TextField(String.format("%.0f", selected.getStartingPrice()));
        TextField txtInc = new TextField(String.format("%.0f", selected.getMinBidIncrement()));
        TextField txtDuration = new TextField("30");

        grid.add(new Label("Tên sản phẩm:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Mô tả:"), 0, 1);
        grid.add(txtDesc, 1, 1);
        grid.add(new Label("Giá khởi điểm:"), 0, 2);
        grid.add(txtPrice, 1, 2);
        grid.add(new Label("Bước giá:"), 0, 3);
        grid.add(txtInc, 1, 3);
        grid.add(new Label("Thời lượng mới (phút):"), 0, 4);
        grid.add(txtDuration, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtnType) {
            try {
                String newName = txtName.getText().trim();
                String newDesc = txtDesc.getText().trim();
                double newPrice = Double.parseDouble(txtPrice.getText().trim());
                double newInc = Double.parseDouble(txtInc.getText().trim());
                int newDur = Integer.parseInt(txtDuration.getText().trim());

                if (newName.isEmpty() || newPrice <= 0 || newInc <= 0 || newDur <= 0) {
                    showAlert("Dữ liệu nhập vào không hợp lệ!");
                    return;
                }

                JsonObject reqJson = new JsonObject();
                reqJson.addProperty("auctionId", selected.getId());
                reqJson.addProperty("itemName", newName);
                reqJson.addProperty("description", newDesc);
                reqJson.addProperty("startingPrice", newPrice);
                reqJson.addProperty("increment", newInc);
                reqJson.addProperty("durationMinutes", newDur);

                new Thread(() -> {
                    try {
                        RequestMessage req = new RequestMessage(CommandType.UPDATE_AUCTION, reqJson.toString());
                        ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                        Platform.runLater(() -> {
                            showAlert(res.getMessage());
                            if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                                loadMyAuctions();
                                if (onDataChangedCallback != null) {
                                    onDataChangedCallback.run();
                                }
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Lỗi cập nhật: " + e.getMessage()));
                    }
                }).start();

            } catch (Exception e) {
                showAlert("Vui lòng nhập đúng định dạng số!");
            }
        }
    }

    @FXML
    public void handleDeleteAuction(ActionEvent event) {
        Auction selected = tableMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn sản phẩm cần xóa!");
            return;
        }

        if (selected.getHighestBidderId() != null) {
            showAlert("Phiên đấu giá đã có người tham gia đặt giá, không thể xóa!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa sản phẩm");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn xóa sản phẩm/phiên đấu giá #" + selected.getId() + " (" + selected.getItemName() + ")?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    RequestMessage req = new RequestMessage(CommandType.DELETE_SELLER_AUCTION, String.valueOf(selected.getId()));
                    ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        showAlert(res.getMessage());
                        if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                            loadMyAuctions();
                            if (onDataChangedCallback != null) {
                                onDataChangedCallback.run();
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi xóa: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handleConfirmPayment(ActionEvent event) {
        Auction selected = tableMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn một phiên đấu giá trong bảng!");
            return;
        }

        if (selected.getStatus() != AuctionStatus.FINISHED) {
            showAlert("Chỉ có thể xác nhận thanh toán khi phiên đấu giá đã KẾT THÚC (FINISHED)!");
            return;
        }

        if (selected.getHighestBidderId() == null) {
            showAlert("Phiên đấu giá này kết thúc mà không có người trả giá, không thể thanh toán!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thanh toán (PAID)");
        confirm.setHeaderText("Xác nhận đã nhận tiền thanh toán từ người thắng cuộc?");
        confirm.setContentText(String.format("Người thắng: %s\nSố tiền: %,.0f VND\n\nChuyển trạng thái sang PAID?",
                selected.getHighestBidderUsername(), selected.getCurrentHighestPrice()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    RequestMessage req = new RequestMessage(CommandType.PAY_AUCTION, String.valueOf(selected.getId()));
                    ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        showAlert(res.getMessage());
                        if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                            loadMyAuctions();
                            if (onDataChangedCallback != null) {
                                onDataChangedCallback.run();
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi thanh toán: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handleEnterRoom(ActionEvent event) {
        Auction selected = tableMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn một phiên đấu giá!");
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
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) lblSellerInfo.getScene().getWindow();
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
