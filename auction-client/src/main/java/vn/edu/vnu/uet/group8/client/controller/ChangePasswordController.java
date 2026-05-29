package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.ModalUtil;

public class ChangePasswordController {

    @FXML private PasswordField pfOldPassword;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirmPassword;
    @FXML private Button btnConfirmChangePassword;

    @FXML
    public void initialize() {
        btnConfirmChangePassword.setOnAction(this::onConfirmChangePasswordClick);
    }

    private void onConfirmChangePasswordClick(ActionEvent event) {
        String oldPass = pfOldPassword.getText();
        String newPass = pfNewPassword.getText();
        String confirmPass = pfConfirmPassword.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            AlertUtil.showError("Vui lòng điền đầy đủ thông tin mật khẩu.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            AlertUtil.showError("Mật khẩu mới và xác nhận mật khẩu không khớp!");
            return;
        }

        if (newPass.length() < 6) {
            AlertUtil.showError("Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }

        UserService.changePassword(oldPass, newPass, success -> Platform.runLater(() -> {
            closeModal();
            Platform.runLater(() -> {
                if (success) {
                    ModalUtil.showModal("ĐỔI MẬT KHẨU THÀNH CÔNG", "SuccessContent.fxml");
                } else {
                    ModalUtil.showModal("THAO TÁC THẤT BẠI", "FailureContent.fxml");
                }
            });
        }));
    }

    private void closeModal() {
        if (btnConfirmChangePassword.getScene() != null && btnConfirmChangePassword.getScene().getWindow() != null) {
            btnConfirmChangePassword.getScene().getWindow().hide();
        }
    }
}