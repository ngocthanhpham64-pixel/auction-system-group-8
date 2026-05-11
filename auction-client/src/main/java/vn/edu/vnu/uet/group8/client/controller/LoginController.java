package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblError;
    @FXML private Button btnTabLogin;
    @FXML private Button btnTabRegister;
    @FXML private Button btnSubmit;

    // ===== STATE =====
    /** true = đang ở tab Đăng nhập, false = Đăng ký */
    private boolean isLoginMode = true;

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideError();
    }

    // ===== TAB SWITCHING =====

    @FXML
    private void onTabLogin() {
        isLoginMode = true;
        // Tab active: nền trắng, nổi; inactive: trong suốt, chữ mờ
        btnTabLogin.setStyle("-fx-background-color: white; -fx-text-fill: #1a1a1a; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-cursor: hand;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");
        btnTabRegister.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSubmit.setText("Đăng nhập");
        hideError();
    }

    @FXML
    private void onTabRegister() {
        isLoginMode = false;
        btnTabRegister.setStyle("-fx-background-color: white; -fx-text-fill: #1a1a1a; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-cursor: hand;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");
        btnTabLogin.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSubmit.setText("Đăng ký");
        hideError();
    }

    // ===== LOGIN / REGISTER ACTION =====

    @FXML
    private void onLogin() {
        if (isLoginMode) {
            handleLogin();
        } else {
            handleRegister();
        }
    }

    private void handleLogin() {
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText();

        // Validate rỗng
        if (email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập email và mật khẩu");
            return;
        }

        // Validate format email đơn giản
        if (!email.contains("@")) {
            showError("Email không hợp lệ");
            return;
        }

        // TODO: thay bằng gọi server thật
        if (email.equals("admin@auctiva.com") && password.equals("123456")) {
            hideError();
            navigateToMain();
        } else {
            showError("Email hoặc mật khẩu không đúng");
            // Rung effect: đổi màu border input
            tfEmail.setStyle(tfEmail.getStyle() + "; -fx-border-color: #E03030;");
            pfPassword.setStyle(pfPassword.getStyle() + "; -fx-border-color: #E03030;");
        }
    }

    private void handleRegister() {
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if (!email.contains("@")) {
            showError("Email không hợp lệ");
            return;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        // TODO: gọi server đăng ký
        hideError();
        navigateToMain();
    }

    @FXML
    private void onForgotPassword() {
        // TODO: mở dialog quên mật khẩu
        showError("Vui lòng liên hệ admin@auctiva.com để đặt lại mật khẩu");
    }

    // ===== NAVIGATION =====

    private void navigateToMain() {
        try {
            URL resource = getClass().getResource("/fxml/MainLayout.fxml");
            if (resource == null) return;
            Parent mainLayout = FXMLLoader.load(resource);
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(mainLayout));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===== UI HELPERS =====

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        // Reset border màu lỗi
        tfEmail.setStyle("");
        pfPassword.setStyle("");
    }
}
