package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginController — Wire AuthService thật từ Backend.
 *
 * Logic:
 *   - Validate input phía client (rỗng, format)
 *   - Gọi AuthService.login() bất đồng bộ
 *   - Callback onSuccess → navigate MainLayout
 *   - Callback onFailure → hiện lỗi
 *
 * Note: Bỏ hoàn toàn mock account "admin@auctiva.com/123456".
 * Server phải đang chạy + đã đăng ký user thật.
 */
public class LoginController implements Initializable {

    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblError;
    @FXML private Button btnTabLogin;
    @FXML private Button btnTabRegister;
    @FXML private Button btnSubmit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideError();
    }

    @FXML
    private void onLogin() {
        String email = tfEmail.getText().trim();
        String password = pfPassword.getText();

        // Validate client-side trước (fail fast)
        if (email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        hideError();
        btnSubmit.setDisable(true);
        btnSubmit.setText("Đang đăng nhập...");

        // Gọi AuthService — callback đã chạy trên FX Thread
        AuthService.login(email, password,
                // onSuccess: Runnable
                () -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Đăng nhập");
                    SceneManager.switchTo(SceneManager.VIEW_MAIN);
                },
                // onFailure: Consumer<String>
                errorMsg -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Đăng nhập");
                    showError(errorMsg);
                }
        );
    }

    @FXML
    private void onTabLogin() {
        // Đã ở LoginView rồi, không làm gì
    }

    @FXML
    private void onTabRegister() {
        // Chuyển sang RegisterView (sẽ làm trong Batch 3)
        SceneManager.switchTo("RegisterView.fxml");
    }

    @FXML
    private void onForgotPassword() {
        // TODO: chuyển sang ForgotPasswordView (chưa có)
        showError("Tính năng đang phát triển");
    }

    // ===== HELPERS =====

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}