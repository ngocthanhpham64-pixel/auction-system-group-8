package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.service.RegisterService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

public class LoginController {

    @FXML private VBox formLogin;
    @FXML private VBox formRegister;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblLoginError;
    @FXML private Button btnLoginSubmit;
    @FXML private Button btnTabLogin;
    @FXML private Button btnTabRegister;

    @FXML private TextField tfRegUsername;
    @FXML private TextField tfRegEmail;
    @FXML private PasswordField pfRegPassword;
    @FXML private PasswordField pfRegConfirm;
    @FXML private TextField tfRegFullName;
    @FXML private TextField tfRegPhone;
    @FXML private Label lblRegError;
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;

    @FXML
    public void onTabLogin() {
        formLogin.setVisible(true); formLogin.setManaged(true);
        formRegister.setVisible(false); formRegister.setManaged(false);
        lblTitle.setText("Chào mừng trở lại!");
        lblSubtitle.setText("Đăng nhập để tiếp tục đấu giá");

        btnTabLogin.setStyle("-fx-background-color: white; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");
        btnTabRegister.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-background-radius: 8; -fx-cursor: hand;");
        
        lblLoginError.setVisible(false); lblLoginError.setManaged(false);
    }

    @FXML
    public void onTabRegister() {
        formLogin.setVisible(false); formLogin.setManaged(false);
        formRegister.setVisible(true); formRegister.setManaged(true);
        lblTitle.setText("Tạo tài khoản mới");
        lblSubtitle.setText("Tham gia cộng đồng đấu giá Auctiva");

        btnTabRegister.setStyle("-fx-background-color: white; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");
        btnTabLogin.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-background-radius: 8; -fx-cursor: hand;");
        
        lblRegError.setVisible(false); lblRegError.setManaged(false);
    }

    @FXML
    public void onLogin() {
        String email = tfEmail.getText().trim();
        String pass = pfPassword.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            showLoginError("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        btnLoginSubmit.setDisable(true);
        AuthService.login(email, pass, () -> Platform.runLater(() -> {
            if (vn.edu.vnu.uet.group8.client.util.SessionManager.isAdmin()) {
                SceneManager.switchTo("AdminLayout.fxml", "Auctiva - Admin Dashboard");
            } else {
                SceneManager.switchTo(SceneManager.VIEW_MAIN, "Auctiva - Live Online Auction");
            }
        }), err -> Platform.runLater(() -> {
            showLoginError(err);
            btnLoginSubmit.setDisable(false);
        }));
    }

    @FXML
    public void onRegister() {
        String user = tfRegUsername.getText().trim();
        String email = tfRegEmail.getText().trim();
        String pass = pfRegPassword.getText();
        String confirm = pfRegConfirm.getText();
        String name = tfRegFullName.getText().trim();
        String phone = tfRegPhone.getText().trim();

        if (user.isEmpty() || email.isEmpty() || pass.isEmpty() || name.isEmpty() || phone.isEmpty()) {
            showRegError("Vui lòng điền đầy đủ thông tin");
            return;
        }

        if (!pass.equals(confirm)) {
            showRegError("Mật khẩu xác nhận không khớp");
            return;
        }

        RegisterService.register(user, email, pass, name, phone, () -> Platform.runLater(() -> {
            AlertUtil.showInfo("Đăng ký thành công! Hãy đăng nhập.");
            onTabLogin();
        }), err -> Platform.runLater(() -> {
            showRegError(err);
        }));
    }

    private void showLoginError(String msg) {
        lblLoginError.setText(msg);
        lblLoginError.setVisible(true);
        lblLoginError.setManaged(true);
    }

    private void showRegError(String msg) {
        lblRegError.setText(msg);
        lblRegError.setVisible(true);
        lblRegError.setManaged(true);
    }

    @FXML
    public void onForgotPassword() {
        SceneManager.switchTo("ForgetPasswordView.fxml", "Auctiva - Khôi phục mật khẩu");
    }

    @FXML public void onShowTerms() {
        AlertUtil.showInfo("Điều khoản dịch vụ: Vui lòng đấu giá văn minh.");
    }
}