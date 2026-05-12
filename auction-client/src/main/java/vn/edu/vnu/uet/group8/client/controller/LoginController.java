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
 * LoginController — wire AuthService thật + mock account để test khi BE chưa sẵn sàng.
 */
public class LoginController implements Initializable {

    // ===== MOCK ACCOUNTS (chỉ dùng để test khi BE chưa đồng bộ) =====
    private static final String MOCK_ADMIN_EMAIL = "admin@auctiva.com";
    private static final String MOCK_ADMIN_PASS  = "123456";
    private static final String MOCK_USER_EMAIL  = "user@auctiva.com";
    private static final String MOCK_USER_PASS   = "123456";

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

        // Validate
        if (email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        hideError();

        // ===== MOCK LOGIN — Bypass server cho test =====
        if (isMockAccount(email, password)) {
            System.out.println("[Login] Mock login: " + email);
            SceneManager.switchTo(SceneManager.VIEW_MAIN);
            return;
        }

        // ===== REAL LOGIN — Gọi server =====
        btnSubmit.setDisable(true);
        btnSubmit.setText("Đang đăng nhập...");

        AuthService.login(email, password,
                () -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Đăng nhập");
                    SceneManager.switchTo(SceneManager.VIEW_MAIN);
                },
                errorMsg -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Đăng nhập");
                    showError(errorMsg);
                }
        );
    }

    /**
     * Check mock account — chỉ trả về true nếu khớp 1 trong các tài khoản test.
     * TODO: Xóa method này khi BE đồng bộ xong + có user thật trong DB.
     */
    private boolean isMockAccount(String email, String password) {
        if (MOCK_ADMIN_EMAIL.equals(email) && MOCK_ADMIN_PASS.equals(password)) {
            return true;
        }
        if (MOCK_USER_EMAIL.equals(email) && MOCK_USER_PASS.equals(password)) {
            return true;
        }
        return false;
    }

    @FXML
    private void onTabLogin() {
        // Đã ở LoginView
    }

    @FXML
    private void onTabRegister() {
        SceneManager.switchTo("RegisterView.fxml");
    }

    @FXML
    private void onForgotPassword() {
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