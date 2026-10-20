package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.common.enums.CommandType;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller cho màn hình Đăng ký tài khoản người dùng mới (MVC Pattern).
 */
public class RegisterController {

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtFullName;

    @FXML
    private TextField txtEmail;

    @FXML
    private ComboBox<String> cbRole;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private Label lblStatus;

    @FXML
    public void initialize() {
        cbRole.getItems().addAll(
                "Người tham gia đấu giá (BIDDER)",
                "Người bán hàng (SELLER)"
        );
        cbRole.getSelectionModel().select(0);
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();
        String confirmPassword = txtConfirmPassword.getText().trim();

        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Vui lòng điền đầy đủ tất cả các trường thông tin!");
            return;
        }

        if (username.contains(" ")) {
            showAlert("Tên đăng nhập không được chứa khoảng trắng!");
            return;
        }

        if (password.length() < 6) {
            showAlert("Mật khẩu phải có độ dài tối thiểu 6 ký tự!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Mật khẩu xác nhận không khớp!");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showAlert("Địa chỉ email không hợp lệ!");
            return;
        }

        String role = cbRole.getSelectionModel().getSelectedIndex() == 1 ? "SELLER" : "BIDDER";

        new Thread(() -> {
            try {
                if (!SocketClient.getInstance().isConnected()) {
                    try {
                        SocketClient.getInstance().connect("localhost", 8888);
                    } catch (Exception connEx) {
                        Platform.runLater(() -> {
                            lblStatus.setText("Chưa kết nối Server (hãy bật Server trước)");
                            showAlert("Không thể kết nối đến máy chủ! Vui lòng bật Server trước (run-server.bat).");
                        });
                        return;
                    }
                }

                JsonObject req = new JsonObject();
                req.addProperty("username", username);
                req.addProperty("password", password);
                req.addProperty("fullName", fullName);
                req.addProperty("email", email);
                req.addProperty("role", role);

                RequestMessage msg = new RequestMessage(CommandType.REGISTER, req.toString());
                ResponseMessage res = SocketClient.getInstance().sendRequest(msg);

                Platform.runLater(() -> {
                    if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                        showAlert("🎉 Đăng ký tài khoản thành công!\nHệ thống sẽ chuyển về màn hình đăng nhập.");
                        navigateToLogin(username);
                    } else {
                        showAlert("Đăng ký thất bại: " + res.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi giao tiếp mạng: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleBackToLogin(ActionEvent event) {
        navigateToLogin(null);
    }

    private void navigateToLogin(String prefilledUsername) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            if (prefilledUsername != null && !prefilledUsername.isEmpty()) {
                LoginController loginController = loader.getController();
                loginController.setPrefilledUsername(prefilledUsername);
            }

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setTitle("Hệ thống Đấu giá trực tuyến - Đăng nhập");
            stage.setScene(new Scene(root, 440, 520));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Không thể chuyển về màn hình đăng nhập: " + e.getMessage());
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
