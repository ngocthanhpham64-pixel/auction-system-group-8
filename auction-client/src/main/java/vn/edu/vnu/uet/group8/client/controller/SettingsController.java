package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    // ===== FXML BINDINGS — Thông tin cá nhân =====
    @FXML private Label lblFullName;
    @FXML private Label lblBirthday;
    @FXML private Label lblAddress;

    // Bảo mật
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;
    @FXML private Label lbl2FA;

    // Thanh toán
    @FXML private Label lblPaymentMethod;
    @FXML private Label lblDepositLimit;

    // Thông báo
    @FXML private CheckBox cbPushNoti;
    @FXML private CheckBox cbEmailOutbid;
    @FXML private CheckBox cbEmailEnding;
    @FXML private CheckBox cbEmailPromo;

    // Giao diện
    @FXML private ComboBox<String> cbLanguage;
    @FXML private CheckBox cbDarkMode;

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbLanguage.getItems().addAll("Tiếng Việt", "English");
        cbLanguage.getSelectionModel().selectFirst();
        // TODO: load settings từ server / local preferences
    }

    // ===== THÔNG TIN CÁ NHÂN =====

    @FXML private void onEditFullName() { openEditDialog("Họ và tên", lblFullName); }
    @FXML private void onEditBirthday() { openEditDialog("Ngày sinh", lblBirthday); }
    @FXML private void onEditAddress()  { openEditDialog("Địa chỉ",   lblAddress);  }

    // ===== BẢO MẬT =====

    @FXML private void onChangeEmail()    { openEditDialog("Email mới",    lblEmail); }
    @FXML private void onChangePhone()    { openEditDialog("Số điện thoại", lblPhone); }
    @FXML private void onChangePassword() {
        // TODO: mở dialog đổi mật khẩu (yêu cầu mật khẩu cũ)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đổi mật khẩu");
        alert.setHeaderText(null);
        alert.setContentText("Tính năng đổi mật khẩu sẽ sớm ra mắt.");
        alert.showAndWait();
    }
    @FXML private void onManage2FA() {
        System.out.println("[SettingsController] Quản lý xác thực 2 lớp");
    }

    // ===== THANH TOÁN =====

    @FXML private void onManagePayment()   { System.out.println("[Settings] Quản lý phương thức thanh toán"); }
    @FXML private void onEditDepositLimit() { System.out.println("[Settings] Chỉnh hạn mức đặt cọc"); }

    // ===== THÔNG BÁO (Toggle) =====

    @FXML private void onTogglePush()         { saveSetting("push_noti",    cbPushNoti.isSelected()); }
    @FXML private void onToggleEmailOutbid()  { saveSetting("email_outbid", cbEmailOutbid.isSelected()); }
    @FXML private void onToggleEmailEnding()  { saveSetting("email_ending", cbEmailEnding.isSelected()); }
    @FXML private void onToggleEmailPromo()   { saveSetting("email_promo",  cbEmailPromo.isSelected()); }

    // ===== GIAO DIỆN =====

    @FXML
    private void onChangeLanguage() {
        String lang = cbLanguage.getValue();
        System.out.println("[Settings] Đổi ngôn ngữ: " + lang);
        // TODO: áp dụng locale
    }

    @FXML
    private void onToggleDarkMode() {
        boolean dark = cbDarkMode.isSelected();
        System.out.println("[Settings] Dark mode: " + dark);
        // TODO: đổi stylesheet toàn app
    }

    // ===== VÙNG NGUY HIỂM =====

    @FXML
    private void onDeleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa tài khoản");
        confirm.setHeaderText("Bạn có chắc chắn muốn xóa tài khoản?");
        confirm.setContentText("Hành động này không thể hoàn tác. Tất cả dữ liệu sẽ bị xóa vĩnh viễn.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: gọi server xóa tài khoản (soft-delete: setDeleted(true))
            System.out.println("[Settings] Xóa tài khoản");
        }
    }

    // ===== HELPERS =====

    /**
     * Mở dialog chỉnh sửa đơn giản (TextField) và cập nhật Label.
     */
    private void openEditDialog(String fieldName, Label target) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(target.getText());
        dialog.setTitle("Chỉnh sửa");
        dialog.setHeaderText(null);
        dialog.setContentText(fieldName + ":");
        // Style dialog button OK
        dialog.showAndWait().ifPresent(value -> {
            if (!value.isBlank()) {
                target.setText(value);
                // TODO: gửi lên server cập nhật
                System.out.println("[Settings] Cập nhật " + fieldName + " = " + value);
            }
        });
    }

    private void saveSetting(String key, boolean value) {
        // TODO: lưu setting xuống server / preferences
        System.out.println("[Settings] " + key + " = " + value);
    }
}
