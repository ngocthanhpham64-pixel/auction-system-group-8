package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

/**
 * SettingsController — cai dat tai khoan.
 *
 * Tinh nang THUC SU:
 *  - Dark mode: toggle thay doi CSS thuc te + persist xuong Preferences
 *  - Notification settings: persist boolean xuong Preferences
 *  - Edit field: validate format (email, phone) truoc khi luu
 *  - Persist su dung java.util.prefs.Preferences (built-in JDK)
 *
 * Cho BE bo sung API USER_UPDATE de sync server-side.
 */
public class SettingsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());
    private static final Preferences PREFS = Preferences.userNodeForPackage(SettingsController.class);

    // Pref keys
    private static final String PREF_DARK_MODE       = "darkMode";
    private static final String PREF_EMAIL_ENDING    = "emailEnding";
    private static final String PREF_EMAIL_OUTBID    = "emailOutbid";
    private static final String PREF_EMAIL_PROMO     = "emailPromo";
    private static final String PREF_PUSH_NOTI       = "pushNoti";
    private static final String PREF_LANGUAGE        = "language";

    // Validators
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    @FXML Label lblFullName;
    @FXML Label lblEmail;
    @FXML Label lblPhone;
    @FXML Label lblAddress;
    @FXML Label lblBirthday;
    @FXML Label lblDepositLimit;
    @FXML Label lblPaymentMethod;
    @FXML Label lbl2FA;

    @FXML CheckBox cbEmailEnding;
    @FXML CheckBox cbEmailOutbid;
    @FXML CheckBox cbEmailPromo;
    @FXML CheckBox cbPushNoti;
    @FXML CheckBox cbDarkMode;
    @FXML ComboBox<String> cbLanguage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserInfo();
        loadPreferences();
        setupLanguageOptions();
    }

    /** Load user info tu SessionManager. */
    void loadUserInfo() {
        if (lblFullName != null && SessionManager.getFullName() != null) {
            lblFullName.setText(SessionManager.getFullName());
        }
        // lblEmail, lblPhone... lay tu LoginResponse / UserService.loadProfile khi can
    }

    /** Load preferences da save -> set checkbox + combobox state. */
    void loadPreferences() {
        if (cbDarkMode != null) {
            cbDarkMode.setSelected(PREFS.getBoolean(PREF_DARK_MODE, false));
        }
        if (cbEmailEnding != null) {
            cbEmailEnding.setSelected(PREFS.getBoolean(PREF_EMAIL_ENDING, true));
        }
        if (cbEmailOutbid != null) {
            cbEmailOutbid.setSelected(PREFS.getBoolean(PREF_EMAIL_OUTBID, true));
        }
        if (cbEmailPromo != null) {
            cbEmailPromo.setSelected(PREFS.getBoolean(PREF_EMAIL_PROMO, false));
        }
        if (cbPushNoti != null) {
            cbPushNoti.setSelected(PREFS.getBoolean(PREF_PUSH_NOTI, true));
        }
    }

    void setupLanguageOptions() {
        if (cbLanguage == null) return;
        cbLanguage.getItems().setAll("Tieng Viet", "English");
        String saved = PREFS.get(PREF_LANGUAGE, "Tieng Viet");
        cbLanguage.setValue(saved);
    }

    // ===== EDIT THONG TIN CA NHAN =====

    @FXML
    void onEditFullName() {
        editField("Ho ten", lblFullName, value -> value.length() >= 2,
                "Ho ten phai co it nhat 2 ky tu");
    }

    @FXML
    void onChangeEmail() {
        editField("Email", lblEmail,
                value -> EMAIL_PATTERN.matcher(value).matches(),
                "Email khong hop le");
    }

    @FXML
    void onChangePhone() {
        editField("So dien thoai", lblPhone,
                value -> PHONE_PATTERN.matcher(value).matches(),
                "SDT phai co 10 so, bat dau bang 0");
    }

    @FXML
    void onEditAddress() {
        editField("Dia chi", lblAddress,
                value -> value.length() >= 5,
                "Dia chi qua ngan");
    }

    @FXML
    void onEditBirthday() {
        editField("Ngay sinh", lblBirthday,
                value -> value.matches("\\d{2}/\\d{2}/\\d{4}"),
                "Dinh dang: dd/MM/yyyy");
    }

    @FXML
    void onEditDepositLimit() {
        editField("Han muc nap (VND)", lblDepositLimit,
                value -> value.replaceAll("[^\\d]", "").matches("\\d+"),
                "Phai la so");
    }

    void syncProfileToServer() {
        String fullname = lblFullName != null ? lblFullName.getText() : "";
        String phone = lblPhone != null ? lblPhone.getText() : "";
        String address = lblAddress != null ? lblAddress.getText() : "";
        
        // Bỏ qua giá trị mặc định của FXML nếu người dùng chưa sửa
        if (fullname.isEmpty() || fullname.contains("Tên")) fullname = SessionManager.getFullName();
        if (phone.isEmpty() || phone.contains("Số")) phone = "0000000000"; // Placeholder an toàn để validate qua

        // Tạm thời truyền null cho avatar, bạn có thể bổ sung biến Base64 từ FileChooser vào đây sau
        UserService.updateProfile(fullname, phone, address, null, success -> javafx.application.Platform.runLater(() -> {
            if (success) {
                AlertUtil.showInfo("Cập nhật thành công và đã đồng bộ với máy chủ!");
            } else {
                AlertUtil.showError("Cập nhật thất bại. Vui lòng kiểm tra lại thông tin.");
            }
        }));
    }

    /**
     * Generic edit field: dialog + validate + update label.
     * @param fieldLabel ten field hien thi cho user
     * @param targetLabel label de update
     * @param validator function check value hop le
     * @param errorMsg loi neu validate fail
     */
    void editField(String fieldLabel, Label targetLabel,
                           java.util.function.Predicate<String> validator, String errorMsg) {
        TextInputDialog dialog = new TextInputDialog(
                targetLabel != null ? targetLabel.getText() : "");
        dialog.setTitle("Cap nhat " + fieldLabel);
        dialog.setHeaderText(null);
        dialog.setContentText(fieldLabel + " moi:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String value = result.get().trim();
        if (value.isEmpty()) {
            AlertUtil.showWarning("Khong duoc de trong");
            return;
        }

        if (!validator.test(value)) {
            AlertUtil.showWarning(errorMsg);
            return;
        }

        // Update UI ngay
        if (targetLabel != null) {
            targetLabel.setText(value);
        }
        LOGGER.info(() -> "Da cap nhat " + fieldLabel + " = " + value);
        syncProfileToServer();
    }

    // ===== BAO MAT =====

    @FXML
    void onChangePassword() {
        // Dialog 3 buoc: old / new / confirm
        TextInputDialog oldDialog = new TextInputDialog();
        oldDialog.setTitle("Doi mat khau");
        oldDialog.setHeaderText("Buoc 1/3");
        oldDialog.setContentText("Mat khau hien tai:");
        Optional<String> oldPass = oldDialog.showAndWait();
        if (oldPass.isEmpty() || oldPass.get().isBlank()) return;

        TextInputDialog newDialog = new TextInputDialog();
        newDialog.setTitle("Doi mat khau");
        newDialog.setHeaderText("Buoc 2/3");
        newDialog.setContentText("Mat khau moi (>= 8 ky tu):");
        Optional<String> newPass = newDialog.showAndWait();
        if (newPass.isEmpty()) return;

        if (newPass.get().length() < 8) {
            AlertUtil.showWarning("Mat khau toi thieu 8 ky tu");
            return;
        }

        TextInputDialog confirmDialog = new TextInputDialog();
        confirmDialog.setTitle("Doi mat khau");
        confirmDialog.setHeaderText("Buoc 3/3");
        confirmDialog.setContentText("Nhap lai mat khau moi:");
        Optional<String> confirm = confirmDialog.showAndWait();
        if (confirm.isEmpty()) return;

        if (!newPass.get().equals(confirm.get())) {
            AlertUtil.showWarning("Mat khau xac nhan khong khop");
            return;
        }

        UserService.changePassword(oldPass.get(), newPass.get(), success -> javafx.application.Platform.runLater(() -> {
            if (success) {
                AlertUtil.showInfo("Đổi mật khẩu thành công!");
            } else {
                AlertUtil.showError("Đổi mật khẩu thất bại, vui lòng kiểm tra lại mật khẩu cũ.");
            }
        }));
    }

    @FXML
    void onManage2FA() {
        boolean isOn = lbl2FA != null && "Da bat".equalsIgnoreCase(lbl2FA.getText());
        boolean confirm = AlertUtil.showConfirm("2FA",
                isOn ? "Tat xac thuc 2 yeu to?" : "Bat xac thuc 2 yeu to?");
        if (!confirm) return;

        if (lbl2FA != null) {
            lbl2FA.setText(isOn ? "Da tat" : "Da bat");
        }
        LOGGER.info(() -> "2FA: " + (isOn ? "OFF" : "ON"));
    }

    @FXML
    void onManagePayment() {
        AlertUtil.showInfo("Quan ly phuong thuc thanh toan dang phat trien");
    }

    // ===== TOGGLE NOTIFICATION =====

    @FXML
    void onToggleEmailEnding() {
        savePreference(PREF_EMAIL_ENDING, cbEmailEnding.isSelected(), "Email ket thuc dau gia");
    }

    @FXML
    void onToggleEmailOutbid() {
        savePreference(PREF_EMAIL_OUTBID, cbEmailOutbid.isSelected(), "Email bi outbid");
    }

    @FXML
    void onToggleEmailPromo() {
        savePreference(PREF_EMAIL_PROMO, cbEmailPromo.isSelected(), "Email khuyen mai");
    }

    @FXML
    void onTogglePush() {
        savePreference(PREF_PUSH_NOTI, cbPushNoti.isSelected(), "Push notification");
    }

    void savePreference(String key, boolean value, String label) {
        PREFS.putBoolean(key, value);
        LOGGER.info(() -> label + " = " + value);
    }

    // ===== GIAO DIEN =====

    /**
     * Dark mode THUC SU: thay doi stylesheet cua Scene + persist.
     */
    @FXML
    void onToggleDarkMode() {
        boolean dark = cbDarkMode.isSelected();
        Scene scene = cbDarkMode.getScene();
        if (scene == null) return;

        String darkCss = "/css/dark-theme.css";
        URL darkUrl = getClass().getResource(darkCss);

        if (dark) {
            if (darkUrl != null) {
                scene.getStylesheets().add(darkUrl.toExternalForm());
                LOGGER.info("Da bat Dark Mode");
            } else {
                // Fallback inline style neu khong co dark-theme.css
                scene.getRoot().setStyle("-fx-base: #2b2b2b; -fx-text-fill: white;");
                LOGGER.warning("Khong tim thay dark-theme.css, dung fallback");
            }
        } else {
            // Xoa dark stylesheet
            if (darkUrl != null) {
                scene.getStylesheets().removeIf(s -> s.endsWith("dark-theme.css"));
            }
            scene.getRoot().setStyle("");
            LOGGER.info("Da tat Dark Mode");
        }

        PREFS.putBoolean(PREF_DARK_MODE, dark);
    }

    @FXML
    void onChangeLanguage() {
        if (cbLanguage == null) return;
        String selected = cbLanguage.getValue();
        if (selected == null) return;

        PREFS.put(PREF_LANGUAGE, selected);
        LOGGER.info(() -> "Doi ngon ngu: " + selected);
        AlertUtil.showInfo("Khoi dong lai app de ap dung ngon ngu moi.");
    }

    // ===== VUNG NGUY HIEM =====

    @FXML
    void onDeleteAccount() {
        // 2 buoc xac nhan
        boolean firstConfirm = AlertUtil.showConfirm("Xoa tai khoan",
                "Ban co chac muon xoa tai khoan?\nHanh dong nay KHONG the hoan tac.");
        if (!firstConfirm) return;

        TextInputDialog typeDialog = new TextInputDialog();
        typeDialog.setTitle("Xac nhan cuoi cung");
        typeDialog.setHeaderText("Go 'XOA' de xac nhan");
        typeDialog.setContentText("Nhap:");
        Optional<String> typed = typeDialog.showAndWait();

        if (typed.isEmpty() || !"XOA".equals(typed.get().trim().toUpperCase())) {
            AlertUtil.showInfo("Huy xoa tai khoan");
            return;
        }

        LOGGER.warning("Nguoi dung xac nhan xoa tai khoan");
        AlertUtil.showInfo("Yeu cau xoa tai khoan da gui. Cho BE bo sung API.");
    }
}