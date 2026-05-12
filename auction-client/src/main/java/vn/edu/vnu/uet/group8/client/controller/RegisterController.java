package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import vn.edu.vnu.uet.group8.client.service.RegisterService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * RegisterController — xử lý form đăng ký tài khoản mới.
 */
public class RegisterController implements Initializable {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0\\d{9}$");

    @FXML private TextField tfUsername;
    @FXML private TextField tfFullName;
    @FXML private TextField tfEmail;
    @FXML private TextField tfPhone;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmPassword;
    @FXML private CheckBox cbTerms;
    @FXML private Hyperlink linkTerms;
    @FXML private Label lblError;
    @FXML private Button btnSubmit;
    @FXML private Button btnTabLogin;
    @FXML private Button btnTabRegister;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideError();
    }

    @FXML
    private void onRegister() {
        String username = tfUsername.getText().trim();
        String fullName = tfFullName.getText().trim();
        String email = tfEmail.getText().trim();
        String phone = tfPhone.getText().trim();
        String password = pfPassword.getText();
        String confirmPassword = pfConfirmPassword.getText();

        // Validate client-side
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty()
                || phone.isEmpty() || password.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin");
            return;
        }

        if (username.length() < 3) {
            showError("Tên đăng nhập phải có ít nhất 3 ký tự");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Email không hợp lệ");
            return;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            showError("Số điện thoại phải có 10 số, bắt đầu bằng 0");
            return;
        }

        if (password.length() < 8) {
            showError("Mật khẩu phải có ít nhất 8 ký tự");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp");
            return;
        }

        if (!cbTerms.isSelected()) {
            showError("Bạn cần đồng ý với điều khoản sử dụng");
            return;
        }

        // Gọi RegisterService
        hideError();
        btnSubmit.setDisable(true);
        btnSubmit.setText("Đang đăng ký...");

        RegisterService.register(username, email, password, fullName, phone,
                // onSuccess — Runnable, không nhận tham số
                () -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Đăng ký");
                    AlertUtil.showInfo("Đăng ký thành công!\nVui lòng đăng nhập để tiếp tục.");
                    SceneManager.switchTo(SceneManager.VIEW_LOGIN);
                },
                // onFailure
                errorMsg -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Đăng ký");
                    showError(errorMsg);
                }
        );
    }

    @FXML
    private void onTabLogin() {
        SceneManager.switchTo(SceneManager.VIEW_LOGIN);
    }

    @FXML
    private void onTabRegister() {
        // Đã ở RegisterView, không làm gì
    }

    @FXML
    private void onShowTerms() {
        AlertUtil.showInfo(
                "ĐIỀU KHOẢN SỬ DỤNG AUCTIVA\n\n"
                        + "1. Người dùng cam kết cung cấp thông tin chính xác\n"
                        + "2. Mọi giao dịch đấu giá đều có hiệu lực pháp lý\n"
                        + "3. Cấm giả mạo, lừa đảo, gian lận đấu giá\n"
                        + "4. Tuân thủ luật pháp Việt Nam\n"
                        + "5. Auctiva có quyền khóa tài khoản vi phạm"
        );
    }

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