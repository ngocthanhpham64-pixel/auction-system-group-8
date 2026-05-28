package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

public class ForgetPasswordController {

    @FXML private VBox vboxEmailStep;
    @FXML private VBox vboxResetStep;
    @FXML private TextField tfEmail;
    @FXML private TextField tfOtp;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirmPassword;
    @FXML private Button btnSendOtp;
    @FXML private Button btnResetPassword;

    @FXML
    public void onSendOtp() {
        String email = tfEmail.getText().trim();
        if (email.isEmpty()) {
            AlertUtil.showError("Vui lòng nhập địa chỉ email!");
            return;
        }

        btnSendOtp.setDisable(true);
        btnSendOtp.setText("Đang gửi...");

        try {
            AuthService.requestOtp(email, otp -> Platform.runLater(() -> {
                try {
                    AlertUtil.showInfo("Mã OTP đã được gửi! (Demo: OTP là " + otp + ")");
                    vboxEmailStep.setVisible(false);
                    vboxEmailStep.setManaged(false);
                    vboxResetStep.setVisible(true);
                    vboxResetStep.setManaged(true);
                    tfOtp.setText(otp);
                } finally {
                    // Đảm bảo nút được khôi phục nếu muốn quay lại
                    btnSendOtp.setDisable(false);
                    btnSendOtp.setText("GỬI MÃ XÁC THỰC");
                }
            }), err -> Platform.runLater(() -> {
                AlertUtil.showError("Lỗi: " + err);
                btnSendOtp.setDisable(false);
                btnSendOtp.setText("GỬI MÃ XÁC THỰC");
            }));
        } catch (Exception e) {
            AlertUtil.showError("Lỗi hệ thống: " + e.getMessage());
            btnSendOtp.setDisable(false);
            btnSendOtp.setText("GỬI MÃ XÁC THỰC");
        }
    }

    @FXML
    public void onResetPassword() {
        String email = tfEmail.getText().trim();
        String otp = tfOtp.getText().trim();
        String newPass = pfNewPassword.getText();
        String confirmPass = pfConfirmPassword.getText();

        if (otp.isEmpty() || newPass.isEmpty()) {
            AlertUtil.showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            AlertUtil.showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        btnResetPassword.setDisable(true);
        
        AuthService.resetPassword(email, otp, newPass, () -> Platform.runLater(() -> {
            AlertUtil.showInfo("Đặt lại mật khẩu thành công! Hãy đăng nhập lại.");
            SceneManager.switchTo(SceneManager.VIEW_LOGIN);
        }), err -> Platform.runLater(() -> {
            AlertUtil.showError("Lỗi: " + err);
            btnResetPassword.setDisable(false);
        }));
    }

    @FXML
    public void onBackToLogin() {
        SceneManager.switchTo(SceneManager.VIEW_LOGIN);
    }
}