package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label; 
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.service.RegisterService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

public class LoginController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final String MOCK_ADMIN_EMAIL = "admin@auctiva.com";
    private static final String MOCK_ADMIN_PASS  = "123456";
    private static final String MOCK_USER_EMAIL  = "user@auctiva.com";
    private static final String MOCK_USER_PASS   = "123456";

    @FXML Label lblTitle, lblSubtitle;
    @FXML Button btnTabLogin, btnTabRegister;
    @FXML VBox formLogin, formRegister;

    @FXML TextField tfEmail;
    @FXML PasswordField pfPassword;
    @FXML Button btnLoginSubmit;
    @FXML Label lblLoginError;

    @FXML TextField tfRegUsername, tfRegFullName, tfRegEmail, tfRegPhone;
    @FXML PasswordField pfRegPassword, pfRegConfirm;
    @FXML CheckBox cbRegTerms;
    @FXML Button btnRegSubmit;
    @FXML Label lblRegError;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoginForm();
        setupEnterKey();
    }

    void setupEnterKey() {
        if (pfPassword != null) pfPassword.setOnAction(e -> onLogin());
        if (pfRegConfirm != null) pfRegConfirm.setOnAction(e -> onRegister());
    }

    void switchFormWithAnimation(VBox showForm, VBox hideForm, Runnable onFinished) {
        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(hideForm.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(150), new KeyValue(hideForm.opacityProperty(), 0.0))
        );
        fadeOut.setOnFinished(e -> {
            hideForm.setVisible(false);
            hideForm.setManaged(false);
            showForm.setOpacity(0.0);
            showForm.setVisible(true);
            showForm.setManaged(true);
            Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(showForm.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(200), new KeyValue(showForm.opacityProperty(), 1.0))
            );
            fadeIn.setOnFinished(ev -> { if (onFinished != null) onFinished.run(); });
            fadeIn.play();
        });
        fadeOut.play();
    }

    @FXML
    void onTabLogin() {
        if (formLogin.isVisible()) return;
        switchFormWithAnimation(formLogin, formRegister, () -> {
            btnTabLogin.setStyle("-fx-background-color: white; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");
            btnTabRegister.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-background-radius: 10; -fx-cursor: hand;");
            lblTitle.setText("Chào mừng trở lại!");
            lblSubtitle.setText("Đăng nhập để tiếp tục đấu giá");
        });
    }

    @FXML
    void onTabRegister() {
        if (formRegister.isVisible()) return;
        switchFormWithAnimation(formRegister, formLogin, () -> {
            btnTabRegister.setStyle("-fx-background-color: white; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");
            btnTabLogin.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-background-radius: 10; -fx-cursor: hand;");
            lblTitle.setText("Tạo tài khoản mới");
            lblSubtitle.setText("Đăng ký để bắt đầu đấu giá");
        });
    }

    @FXML
    void onLogin() {
        String email = safeText(tfEmail);
        String password = pfPassword != null ? pfPassword.getText() : "";

        if (email.isEmpty()) { showLoginError("Vui lòng nhập email"); return; }
        if (!EMAIL_PATTERN.matcher(email).matches()) { showLoginError("Email không hợp lệ"); return; }
        if (password.isEmpty()) { showLoginError("Vui lòng nhập mật khẩu"); return; }

        hideLoginError();

        if (isMockAccount(email, password)) {
            mockSessionFor(email);
            navigateAfterLogin();
            return;
        }

        setLoginLoading(true);
        AuthService.login(email, password,
                () -> {
                    setLoginLoading(false);
                    navigateAfterLogin();
                },
                errorMsg -> {
                    setLoginLoading(false);
                    showLoginError(errorMsg != null ? errorMsg : "Đăng nhập thất bại");
                }
        );
    }

    @FXML
    void onRegister() {
        String username = safeText(tfRegUsername);
        String fullName = safeText(tfRegFullName);
        String email    = safeText(tfRegEmail);
        String phone    = safeText(tfRegPhone);
        String password = pfRegPassword != null ? pfRegPassword.getText() : "";
        String confirm  = pfRegConfirm != null ? pfRegConfirm.getText() : "";

        String error = validateRegister(username, fullName, email, phone, password, confirm);
        if (error != null) { showRegError(error); return; }

        hideRegError();
        setRegLoading(true);
        RegisterService.register(username, email, password, fullName, phone,
                () -> {
                    setRegLoading(false);
                    AlertUtil.showInfo("Đăng ký thành công! Vui lòng đăng nhập.");
                    onTabLogin();
                },
                errorMsg -> {
                    setRegLoading(false);
                    showRegError(errorMsg != null ? errorMsg : "Đăng ký thất bại");
                }
        );
    }

    private String validateRegister(String username, String fullName, String email,
                                    String phone, String password, String confirm) {
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty()
                || phone.isEmpty() || password.isEmpty()) {
            return "Vui lòng điền đầy đủ thông tin";
        }
        if (username.length() < 3 || username.length() > 20) return "Tên đăng nhập 3-20 ký tự";
        if (!fullName.matches("^[\\p{L} ]{2,50}$")) return "Họ tên 2-50 ký tự, chỉ chữ và khoảng trắng";
        if (!EMAIL_PATTERN.matcher(email).matches()) return "Email không hợp lệ";
        if (!phone.matches("^0\\d{9}$")) return "SĐT phải 10 số, bắt đầu 0";
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"))
            return "Mật khẩu >=8 ký tự, có ít nhất 1 chữ và 1 số";
        if (!password.equals(confirm)) return "Mật khẩu xác nhận không khớp";
        if (cbRegTerms != null && !cbRegTerms.isSelected()) return "Vui lòng đồng ý điều khoản";
        return null;
    }

    private boolean isMockAccount(String email, String password) {
        return (MOCK_ADMIN_EMAIL.equals(email) && MOCK_ADMIN_PASS.equals(password))
                || (MOCK_USER_EMAIL.equals(email) && MOCK_USER_PASS.equals(password));
    }

    void mockSessionFor(String email) {
        String token = "mock-token-" + System.currentTimeMillis();
        int userId = email.equals(MOCK_ADMIN_EMAIL) ? 1 : 2;
        String username = email.split("@")[0];
        String fullName = email.equals(MOCK_ADMIN_EMAIL) ? "Admin Mock" : "User Mock";
        String role = email.equals(MOCK_ADMIN_EMAIL) ? "ADMIN" : "MEMBER";
        SessionManager.setSession(token, userId, username, fullName, role);
    }

    void navigateAfterLogin() {
        if (SessionManager.isAdmin()) {
            SceneManager.switchTo("AdminLayout.fxml");
        } else {
            SceneManager.switchTo(SceneManager.VIEW_MAIN);
        }
    }

    @FXML
    void onForgotPassword() {
        showLoginError("Liên hệ admin@auctiva.com để đặt lại mật khẩu");
    }

    @FXML
    void onShowTerms() {
        AlertUtil.showInfo("ĐIỀU KHOẢN SỬ DỤNG AUCTIVA\n\n1. Cung cấp thông tin chính xác.\n2. Giao dịch có hiệu lực pháp lý.\n3. Nghiêm cấm gian lận.\n4. Tuân thủ pháp luật Việt Nam.\n5. Auctiva có quyền khóa tài khoản vi phạm.");
    }

    void setLoginLoading(boolean loading) {
        if (btnLoginSubmit != null) {
            btnLoginSubmit.setDisable(loading);
            btnLoginSubmit.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
        }
    }

    void setRegLoading(boolean loading) {
        if (btnRegSubmit != null) {
            btnRegSubmit.setDisable(loading);
            btnRegSubmit.setText(loading ? "Đang đăng ký..." : "Đăng ký");
        }
    }

    void showLoginError(String msg) {
        if (lblLoginError == null) return;
        lblLoginError.setText(msg);
        lblLoginError.setVisible(true);
        lblLoginError.setManaged(true);
    }

    void hideLoginError() {
        if (lblLoginError == null) return;
        lblLoginError.setVisible(false);
        lblLoginError.setManaged(false);
    }

    void showRegError(String msg) {
        if (lblRegError == null) return;
        lblRegError.setText(msg);
        lblRegError.setVisible(true);
        lblRegError.setManaged(true);
    }

    void hideRegError() {
        if (lblRegError == null) return;
        lblRegError.setVisible(false);
        lblRegError.setManaged(false);
    }

    private String safeText(TextField field) {
        return field != null ? field.getText().trim() : "";
    }

    void showLoginForm() {
        formLogin.setVisible(true);    formLogin.setManaged(true);
        formRegister.setVisible(false); formRegister.setManaged(false);
        // Set tab style trực tiếp
        btnTabLogin.setStyle("-fx-background-color: white; -fx-text-fill: #1a1a1a; "
                + "-fx-font-weight: bold; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");
        btnTabRegister.setStyle("-fx-background-color: transparent; "
                + "-fx-text-fill: #888; -fx-background-radius: 10;");
    }

}