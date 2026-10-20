package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.CommandType;
import com.auction.common.enums.UserRole;
import com.auction.common.model.Admin;
import com.auction.common.model.Auction;
import com.auction.common.model.Bidder;
import com.auction.common.model.Seller;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * Controller cho Trung tâm Quản trị hệ thống (Admin Management Panel).
 * Quản lý người dùng, kiểm duyệt phiên đấu giá và thống kê toàn sàn.
 */
public class AdminManagementController {

    // Tab 1: Thống kê
    @FXML
    private Label lblTotalUsers;

    @FXML
    private Label lblUserBreakdown;

    @FXML
    private Label lblRunningAuctions;

    @FXML
    private Label lblAuctionBreakdown;

    @FXML
    private Label lblTotalVolume;

    // Tab 2: Quản lý người dùng
    @FXML
    private TableView<User> tableUsers;

    @FXML
    private TableColumn<User, Long> colUserId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colFullName;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colRole;

    @FXML
    private TableColumn<User, String> colUserDetail;

    // Tab 3: Quản lý phiên đấu giá
    @FXML
    private TableView<Auction> tableAuctions;

    @FXML
    private TableColumn<Auction, Long> colAucId;

    @FXML
    private TableColumn<Auction, String> colAucItem;

    @FXML
    private TableColumn<Auction, String> colAucSeller;

    @FXML
    private TableColumn<Auction, Double> colAucPrice;

    @FXML
    private TableColumn<Auction, String> colAucBidder;

    @FXML
    private TableColumn<Auction, String> colAucStatus;

    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctionList = FXCollections.observableArrayList();
    private Runnable onDataChangedCallback;

    @FXML
    public void initialize() {
        setupUserTable();
        setupAuctionTable();
        loadAllData();
    }

    public void setOnDataChangedCallback(Runnable callback) {
        this.onDataChangedCallback = callback;
    }

    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getRole() != null ? cell.getValue().getRole().name() : ""));

        // Thể hiện tính Đa hình (Polymorphism) của các lớp con User
        colUserDetail.setCellValueFactory(cell -> {
            User u = cell.getValue();
            if (u instanceof Bidder) {
                return new SimpleStringProperty(String.format("Số dư: %,.0f VND", ((Bidder) u).getBalance()));
            } else if (u instanceof Seller) {
                return new SimpleStringProperty(String.format("Đánh giá: %.1f ⭐", ((Seller) u).getRating()));
            } else if (u instanceof Admin) {
                return new SimpleStringProperty(String.format("Bộ phận: %s", ((Admin) u).getDepartment()));
            }
            return new SimpleStringProperty(u.getSummary());
        });

        tableUsers.setItems(userList);
    }

    private void setupAuctionTable() {
        colAucId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAucItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAucSeller.setCellValueFactory(new PropertyValueFactory<>("sellerUsername"));
        colAucPrice.setCellValueFactory(new PropertyValueFactory<>("currentHighestPrice"));
        colAucBidder.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getHighestBidderUsername() != null ? cell.getValue().getHighestBidderUsername() : "(Chưa có)"));
        colAucStatus.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStatus() != null ? cell.getValue().getStatus().name() : ""));

        tableAuctions.setItems(auctionList);
    }

    private void loadAllData() {
        loadUsers();
        loadAuctions();
    }

    public void loadUsers() {
        new Thread(() -> {
            try {
                RequestMessage req = new RequestMessage(CommandType.GET_ALL_USERS, null);
                ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> list = JsonUtils.getGson().fromJson(res.getDataJson(), listType);

                    Platform.runLater(() -> {
                        userList.clear();
                        if (list != null) {
                            userList.addAll(list);
                        }
                        updateKPIs();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi nạp người dùng: " + e.getMessage()));
            }
        }).start();
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
                        updateKPIs();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi nạp danh sách phiên: " + e.getMessage()));
            }
        }).start();
    }

    private void updateKPIs() {
        // Cập nhật người dùng
        long bidders = userList.stream().filter(u -> u.getRole() == UserRole.BIDDER).count();
        long sellers = userList.stream().filter(u -> u.getRole() == UserRole.SELLER).count();
        long admins = userList.stream().filter(u -> u.getRole() == UserRole.ADMIN).count();
        lblTotalUsers.setText(String.valueOf(userList.size()));
        lblUserBreakdown.setText(String.format("Bidder: %d | Seller: %d | Admin: %d", bidders, sellers, admins));

        // Cập nhật phiên đấu giá
        long running = auctionList.stream().filter(a -> a.getStatus() == AuctionStatus.RUNNING).count();
        long finished = auctionList.stream().filter(a -> a.getStatus() == AuctionStatus.FINISHED).count();
        long canceled = auctionList.stream().filter(a -> a.getStatus() == AuctionStatus.CANCELED).count();
        lblRunningAuctions.setText(String.valueOf(running));
        lblAuctionBreakdown.setText(String.format("Chạy: %d | Kết thúc: %d | Đã hủy: %d", running, finished, canceled));

        // Tổng giá trị phiên
        double total = auctionList.stream().mapToDouble(Auction::getCurrentHighestPrice).sum();
        lblTotalVolume.setText(String.format("%,.0f VND", total));
    }

    @FXML
    public void handleRefreshUsers(ActionEvent event) {
        loadUsers();
    }

    @FXML
    public void handleRefreshAuctions(ActionEvent event) {
        loadAuctions();
    }

    @FXML
    public void handleDeleteUser(ActionEvent event) {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn người dùng cần xóa!");
            return;
        }

        if (confirm("Xác nhận xóa tài khoản", "Bạn có chắc chắn muốn xóa tài khoản '" + selected.getUsername() + "'?")) {
            new Thread(() -> {
                try {
                    RequestMessage req = new RequestMessage(CommandType.DELETE_USER, String.valueOf(selected.getId()));
                    ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        showAlert(res.getMessage());
                        if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                            loadUsers();
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi xóa người dùng: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handleCancelAuction(ActionEvent event) {
        Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn phiên đấu giá cần hủy!");
            return;
        }

        if (selected.getStatus() == AuctionStatus.CANCELED || selected.getStatus() == AuctionStatus.FINISHED) {
            showAlert("Phiên đấu giá này đã kết thúc hoặc đã bị hủy trước đó!");
            return;
        }

        if (confirm("Xác nhận hủy phiên", "Bạn có chắc chắn muốn hủy phiên đấu giá #" + selected.getId() + " (" + selected.getItemName() + ")?")) {
            new Thread(() -> {
                try {
                    RequestMessage req = new RequestMessage(CommandType.CANCEL_AUCTION, String.valueOf(selected.getId()));
                    ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        showAlert(res.getMessage());
                        if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                            loadAuctions();
                            if (onDataChangedCallback != null) {
                                onDataChangedCallback.run();
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi hủy phiên: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handleDeleteAuction(ActionEvent event) {
        Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn phiên đấu giá cần xóa vĩnh viễn!");
            return;
        }

        if (confirm("Xác nhận xóa vĩnh viễn", "Hành động này sẽ xóa hoàn toàn phiên #" + selected.getId() + " khỏi cơ sở dữ liệu. Tiếp tục?")) {
            new Thread(() -> {
                try {
                    RequestMessage req = new RequestMessage(CommandType.DELETE_AUCTION, String.valueOf(selected.getId()));
                    ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        showAlert(res.getMessage());
                        if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                            loadAuctions();
                            if (onDataChangedCallback != null) {
                                onDataChangedCallback.run();
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi xóa phiên: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) lblTotalUsers.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
