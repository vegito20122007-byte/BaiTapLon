package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.common.enums.CommandType;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller cho form Người bán (Seller) đăng sản phẩm và tạo phiên đấu giá mới.
 * Áp dụng Factory Method tương ứng với các danh mục (Electronics / Art / Vehicle).
 */
public class CreateAuctionController {

    @FXML
    private ComboBox<String> cbCategory;

    @FXML
    private TextField txtItemName;

    @FXML
    private TextArea txtDescription;

    @FXML
    private TextField txtStartingPrice;

    @FXML
    private TextField txtIncrement;

    @FXML
    private TextField txtDuration;

    @FXML
    private Label lblField1;

    @FXML
    private TextField txtField1;

    @FXML
    private Label lblField2;

    @FXML
    private TextField txtField2;

    private Runnable onCreatedCallback;

    @FXML
    public void initialize() {
        cbCategory.getItems().addAll(
                "Điện tử (ELECTRONICS)",
                "Tác phẩm nghệ thuật (ART)",
                "Phương tiện (VEHICLE)"
        );
        cbCategory.getSelectionModel().select(0);
        updateExtraFields("ELECTRONICS");

        cbCategory.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int idx = newVal.intValue();
            if (idx == 0) {
                updateExtraFields("ELECTRONICS");
            } else if (idx == 1) {
                updateExtraFields("ART");
            } else {
                updateExtraFields("VEHICLE");
            }
        });
    }

    private void updateExtraFields(String category) {
        if ("ELECTRONICS".equalsIgnoreCase(category)) {
            lblField1.setText("Thương hiệu:");
            txtField1.setPromptText("Ví dụ: Sony, Apple, Samsung");
            txtField1.setText("Sony");
            lblField2.setText("Bảo hành (tháng):");
            txtField2.setText("12");
        } else if ("ART".equalsIgnoreCase(category)) {
            lblField1.setText("Tên nghệ sĩ/họa sĩ:");
            txtField1.setPromptText("Ví dụ: Bùi Xuân Phái, Tô Ngọc Vân");
            txtField1.setText("Tô Ngọc Vân");
            lblField2.setText("Năm sáng tác:");
            txtField2.setText("2010");
        } else {
            lblField1.setText("Hãng sản xuất:");
            txtField1.setPromptText("Ví dụ: Toyota, Honda, VinFast");
            txtField1.setText("Toyota");
            lblField2.setText("Số km đã đi (odo):");
            txtField2.setText("15000");
        }
    }

    public void setOnCreatedCallback(Runnable callback) {
        this.onCreatedCallback = callback;
    }

    @FXML
    public void handleSubmit(ActionEvent event) {
        String itemName = txtItemName.getText().trim();
        if (itemName.isEmpty()) {
            showAlert("Vui lòng nhập tên sản phẩm!");
            return;
        }

        double startingPrice;
        double increment;
        int duration;

        try {
            startingPrice = Double.parseDouble(txtStartingPrice.getText().trim());
            if (startingPrice <= 0) {
                showAlert("Giá khởi điểm phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Giá khởi điểm không hợp lệ!");
            return;
        }

        try {
            increment = Double.parseDouble(txtIncrement.getText().trim());
            if (increment <= 0) {
                showAlert("Bước giá phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Bước giá không hợp lệ!");
            return;
        }

        try {
            duration = Integer.parseInt(txtDuration.getText().trim());
            if (duration <= 0) {
                showAlert("Thời lượng phiên phải lớn hơn 0 phút!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Thời lượng phiên không hợp lệ!");
            return;
        }

        int catIdx = cbCategory.getSelectionModel().getSelectedIndex();
        String category = catIdx == 0 ? "ELECTRONICS" : (catIdx == 1 ? "ART" : "VEHICLE");

        JsonObject extra = new JsonObject();
        try {
            if ("ELECTRONICS".equals(category)) {
                extra.addProperty("brand", txtField1.getText().trim().isEmpty() ? "Unknown" : txtField1.getText().trim());
                extra.addProperty("warranty", Integer.parseInt(txtField2.getText().trim()));
            } else if ("ART".equals(category)) {
                extra.addProperty("artist", txtField1.getText().trim().isEmpty() ? "Anonymous" : txtField1.getText().trim());
                extra.addProperty("year", Integer.parseInt(txtField2.getText().trim()));
            } else {
                extra.addProperty("manufacturer", txtField1.getText().trim().isEmpty() ? "Unknown" : txtField1.getText().trim());
                extra.addProperty("odometer", Integer.parseInt(txtField2.getText().trim()));
            }
        } catch (NumberFormatException e) {
            showAlert("Thông số chuyên biệt không hợp lệ (số bảo hành / năm / km phải là số nguyên)!");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("category", category);
        payload.addProperty("itemName", itemName);
        payload.addProperty("description", txtDescription.getText().trim());
        payload.addProperty("startingPrice", startingPrice);
        payload.addProperty("increment", increment);
        payload.addProperty("durationMinutes", duration);
        payload.add("extraParams", extra);

        new Thread(() -> {
            try {
                RequestMessage req = new RequestMessage(CommandType.CREATE_AUCTION, payload.toString());
                ResponseMessage res = SocketClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                        showAlert(res.getMessage());
                        if (onCreatedCallback != null) {
                            onCreatedCallback.run();
                        }
                        closeWindow();
                    } else {
                        showAlert("Thất bại: " + res.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Lỗi gửi dữ liệu: " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtItemName.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
