package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class LoginController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    // ===== MOCK ACCOUNTS (DEV ONLY - XOA TRUOC KHI NOP BAI) =====
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
        setupEnterKeySubmit();
    }

    private void setupEnterKeySubmit() {
        if (pfPassword != null) {
            pfPassword.setOnAction(e -> onLogin());
        }
        if (tfEmail != null) {
            tfEmail.setOnAction(e -> {
                if (pfPassword != null) pfPassword.requestFocus();
            });
        }
    }

    @FXML
    private void onLogin() {
        String email = tfEmail != null ? tfEmail.getText().trim() : "";
        String password = pfPassword != null ? pfPassword.getText() : "";

        if (email.isEmpty()) {
            showError("Vui long nhap email");
            if (tfEmail != null) tfEmail.requestFocus();
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Email khong hop le");
            if (tfEmail != null) tfEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Vui long nhap mat khau");
            if (pfPassword != null) pfPassword.requestFocus();
            return;
        }

        hideError();

        // ===== MOCK LOGIN (DEV ONLY) =====
        if (isMockAccount(email, password)) {
            LOGGER.info(() -> "Mock login: " + email);
            // Giả lập session để phân quyền
            mockSessionFor(email);
            navigateAfterLogin();
            return;
        }

        // ===== REAL LOGIN =====
        setLoadingState(true);
        AuthService.login(email, password,
                () -> {
                    LOGGER.info(() -> "Login thanh cong: " + email);
                    setLoadingState(false);
                    navigateAfterLogin();
                },
                errorMsg -> {
                    LOGGER.warning("Login that bai: " + errorMsg);
                    setLoadingState(false);
                    showError(errorMsg != null ? errorMsg : "Dang nhap that bai");
                }
        );
    }

    /**
     * Điều hướng sau khi đăng nhập thành công dựa vào quyền admin.
     */
    private void navigateAfterLogin() {
        if (SessionManager.isAdmin()) {
            SceneManager.switchTo("AdminLayout.fxml");
        } else {
            SceneManager.switchTo(SceneManager.VIEW_MAIN);
        }
    }

    /**
     * Giả lập session cho mock account để kiểm tra phân quyền.
     * TODO: Xoá khi backend thật đã hoạt động.
     */
    private void mockSessionFor(String email) {
        String token = "mock-token-" + System.currentTimeMillis();
        int userId = email.equals(MOCK_ADMIN_EMAIL) ? 1 : 2;
        String username = email.split("@")[0];
        String fullName = email.equals(MOCK_ADMIN_EMAIL) ? "Admin Mock" : "User Mock";
        String role = email.equals(MOCK_ADMIN_EMAIL) ? "ADMIN" : "MEMBER";
        SessionManager.setSession(token, userId, username, fullName, role);
    }

    private boolean isMockAccount(String email, String password) {
        if (MOCK_ADMIN_EMAIL.equals(email) && MOCK_ADMIN_PASS.equals(password)) return true;
        if (MOCK_USER_EMAIL.equals(email) && MOCK_USER_PASS.equals(password)) return true;
        return false;
    }

    private void setLoadingState(boolean loading) {
        if (btnSubmit == null) return;
        btnSubmit.setDisable(loading);
        btnSubmit.setText(loading ? "Dang dang nhap..." : "Dang nhap");
        if (tfEmail != null) tfEmail.setDisable(loading);
        if (pfPassword != null) pfPassword.setDisable(loading);
    }

    @FXML
    private void onTabLogin() { /* da o LoginView */ }

    @FXML
    private void onTabRegister() {
        SceneManager.switchTo("RegisterVIew.fxml");
    }

    @FXML
    private void onForgotPassword() {
        LOGGER.info("Click quen mat khau");
        showError("Tinh nang dang phat trien");
    }

    private void showError(String message) {
        if (lblError == null) return;
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        if (lblError == null) return;
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}