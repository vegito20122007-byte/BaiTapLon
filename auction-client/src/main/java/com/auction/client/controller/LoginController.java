package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.ClientSession;
import com.auction.common.enums.CommandType;
import com.auction.common.model.User;
import com.auction.common.protocol.RequestMessage;
import com.auction.common.protocol.ResponseMessage;
import com.auction.common.util.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller cho màn hình Đăng nhập (MVC Pattern).
 */
public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblStatus;

    @FXML
    public void initialize() {
        // Tự động kết nối Socket khi mở app
        new Thread(() -> {
            try {
                SocketClient.getInstance().connect("localhost", 8888);
                Platform.runLater(() -> lblStatus.setText("Đã kết nối Server"));
            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("Chưa kết nối Server (hãy bật Server trước)"));
            }
        }).start();
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        try {
            if (!SocketClient.getInstance().isConnected()) {
                try {
                    SocketClient.getInstance().connect("localhost", 8888);
                    lblStatus.setText("Đã kết nối Server");
                } catch (Exception connEx) {
                    lblStatus.setText("Chưa kết nối Server (hãy bật Server trước)");
                    showAlert("Không thể kết nối đến máy chủ!\nVui lòng khởi động Server trước (chạy file run-server.bat hoặc ServerApp.main trong IDE).");
                    return;
                }
            }

            JsonObject req = new JsonObject();
            req.addProperty("username", username);
            req.addProperty("password", password);

            RequestMessage msg = new RequestMessage(CommandType.LOGIN, req.toString());
            ResponseMessage res = SocketClient.getInstance().sendRequest(msg);

            if (res.getStatus() == ResponseMessage.Status.SUCCESS) {
                JsonObject data = JsonParser.parseString(res.getDataJson()).getAsJsonObject();
                String token = data.get("token").getAsString();
                User user = JsonUtils.fromJson(data.get("user").toString(), User.class);

                ClientSession.getInstance().setToken(token);
                ClientSession.getInstance().setCurrentUser(user);

                // Chuyển sang màn hình Dashboard
                navigateToDashboard();
            } else {
                showAlert(res.getMessage());
            }
        } catch (Exception e) {
            showAlert("Lỗi đăng nhập: " + e.getMessage());
        }
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setTitle("Hệ thống đấu giá - Dashboard");
            stage.setScene(new Scene(root, 950, 650));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Không thể tải màn hình Dashboard: " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setTitle("Hệ thống Đấu giá trực tuyến - Đăng ký tài khoản");
            stage.setScene(new Scene(root, 460, 640));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Không thể mở màn hình đăng ký: " + e.getMessage());
        }
    }

    public void setPrefilledUsername(String username) {
        if (txtUsername != null && username != null) {
            txtUsername.setText(username);
            txtPassword.clear();
            txtPassword.requestFocus();
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
