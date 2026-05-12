package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import vn.edu.vnu.uet.group8.client.service.AuthService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * SettingsController — quản lý cài đặt tài khoản.
 *
 * Note: BE hiện chưa có API update profile từng field
 * (chỉ có loadProfile + deposit). Một số setting tạm để TODO
 * chờ BE bổ sung endpoint UPDATE_PROFILE.
 */
public class SettingsController implements Initializable {

    // Cá nhân
    @FXML private Label lblFullName;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;

    // Notification
    @FXML private CheckBox cbNotifBid;
    @FXML private CheckBox cbNotifWin;
    @FXML private CheckBox cbNotifNewItem;

    // Giao diện
    @FXML private CheckBox cbDarkMode;
    @FXML private Label lblLanguage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCurrentSettings();
    }

    private void loadCurrentSettings() {
        // Hiển thị info từ SessionManager
        if (lblFullName != null) lblFullName.setText(SessionManager.getFullName());
        if (lblEmail != null) lblEmail.setText("(load từ profile)");
        if (lblPhone != null) lblPhone.setText("(load từ profile)");
    }

    // ===== EDIT CÁ NHÂN =====

    @FXML private void onEditFullName() { openEditDialog("Họ tên", "fullName"); }
    @FXML private void onEditEmail()    { openEditDialog("Email", "email"); }
    @FXML private void onEditPhone()    { openEditDialog("Số điện thoại", "phone"); }
    @FXML private void onEditAddress()  { openEditDialog("Địa chỉ", "address"); }
    @FXML private void onEditBirthday() { openEditDialog("Ngày sinh", "birthday"); }

    private void openEditDialog(String fieldLabel, String fieldKey) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cập nhật " + fieldLabel);
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập " + fieldLabel + " mới:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(value -> saveSetting(fieldKey, value));
    }

    private void saveSetting(String key, String value) {
        // TODO: Khi BE có API USER_UPDATE → gọi UserService.updateProfile(key, value)
        // Hiện tại chỉ hiện thông báo
        AlertUtil.showInfo("Đã ghi nhận: " + key + " = " + value
                + "\n(Chờ BE bổ sung API)");
    }

    // ===== BẢO MẬT =====

    @FXML
    private void onChangePassword() {
        // TODO: BE chưa có API CHANGE_PASSWORD
        // Cần: oldPassword + newPassword + confirmNewPassword
        AlertUtil.showInfo("Đổi mật khẩu — chờ BE bổ sung API");
    }

    @FXML
    private void onEnable2FA() {
        AlertUtil.showInfo("2FA — chờ BE bổ sung API");
    }

    // ===== NOTIFICATION TOGGLES =====

    @FXML
    private void onToggleNotifBid() {
        saveSetting("notifBid", String.valueOf(cbNotifBid.isSelected()));
    }

    @FXML
    private void onToggleNotifWin() {
        saveSetting("notifWin", String.valueOf(cbNotifWin.isSelected()));
    }

    @FXML
    private void onToggleNotifNewItem() {
        saveSetting("notifNewItem", String.valueOf(cbNotifNewItem.isSelected()));
    }

    // ===== GIAO DIỆN =====

    @FXML
    private void onToggleDarkMode() {
        boolean dark = cbDarkMode.isSelected();
        // TODO: apply theme dark/light
        saveSetting("darkMode", String.valueOf(dark));
    }

    @FXML
    private void onChangeLanguage() {
        AlertUtil.showInfo("Đổi ngôn ngữ — chờ i18n");
    }

    // ===== VÙNG NGUY HIỂM =====

    @FXML
    private void onDeleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xóa tài khoản");
        alert.setHeaderText("Hành động này KHÔNG THỂ HOÀN TÁC");
        alert.setContentText("Bạn có chắc muốn xóa vĩnh viễn tài khoản?");

        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            // TODO: BE chưa có API DELETE_ACCOUNT
            AlertUtil.showInfo("Xóa tài khoản — chờ BE bổ sung API");
        }
    }

    @FXML
    private void onLogout() {
        boolean ok = AlertUtil.showConfirm("Đăng xuất", "Bạn có chắc muốn đăng xuất?");
        if (ok) AuthService.logout();
    }
}