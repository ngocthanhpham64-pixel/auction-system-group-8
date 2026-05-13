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
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * LoginController — man hinh dang nhap.
 *
 * Tinh nang:
 *  - Validate email format + password not empty
 *  - Mock account fallback (DEV ONLY) khi BE chua co user
 *  - Loading state khi dang dang nhap
 *  - Enter key submit form
 *  - Error message ro rang
 *
 * Mock accounts cho test:
 *   admin@auctiva.com / 123456
 *   user@auctiva.com  / 123456
 */
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

    /** Cho phep nhan Enter trong password field de submit. */
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

        // Validate
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
            SceneManager.switchTo(SceneManager.VIEW_MAIN);
            return;
        }

        // ===== REAL LOGIN =====
        setLoadingState(true);
        AuthService.login(email, password,
                () -> {
                    LOGGER.info(() -> "Login thanh cong: " + email);
                    setLoadingState(false);
                    SceneManager.switchTo(SceneManager.VIEW_MAIN);
                },
                errorMsg -> {
                    LOGGER.warning("Login that bai: " + errorMsg);
                    setLoadingState(false);
                    showError(errorMsg != null ? errorMsg : "Dang nhap that bai");
                }
        );
    }

    /** TODO: Xoa method nay khi BE dong bo + DB co user that. */
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
    private void onTabLogin() {
        // Da o LoginView - khong lam gi
    }

    @FXML
    private void onTabRegister() {
        SceneManager.switchTo("RegisterVIew.fxml");  // FXML co typo "VIew"
    }

    @FXML
    private void onForgotPassword() {
        LOGGER.info("Click quen mat khau");
        showError("Tinh nang dang phat trien");
    }

    // ===== HELPERS =====

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