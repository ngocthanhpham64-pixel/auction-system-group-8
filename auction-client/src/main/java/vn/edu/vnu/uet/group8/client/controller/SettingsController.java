package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

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

    @FXML private Label lblFullName;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;
    @FXML private Label lblAddress;
    @FXML private Label lblBirthday;
    @FXML private Label lblDepositLimit;
    @FXML private Label lblPaymentMethod;
    @FXML private Label lbl2FA;

    @FXML private CheckBox cbEmailEnding;
    @FXML private CheckBox cbEmailOutbid;
    @FXML private CheckBox cbEmailPromo;
    @FXML private CheckBox cbPushNoti;
    @FXML private CheckBox cbDarkMode;
    @FXML private ComboBox<String> cbLanguage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserInfo();
        loadPreferences();
        setupLanguageOptions();
    }

    /** Load user info tu SessionManager. */
    private void loadUserInfo() {
        if (lblFullName != null && SessionManager.getFullName() != null) {
            lblFullName.setText(SessionManager.getFullName());
        }
        // lblEmail, lblPhone... lay tu LoginResponse / UserService.loadProfile khi can
    }

    /** Load preferences da save -> set checkbox + combobox state. */
    private void loadPreferences() {
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

    private void setupLanguageOptions() {
        if (cbLanguage == null) return;
        cbLanguage.getItems().setAll("Tieng Viet", "English");
        String saved = PREFS.get(PREF_LANGUAGE, "Tieng Viet");
        cbLanguage.setValue(saved);
    }

    // ===== EDIT THONG TIN CA NHAN =====

    @FXML
    private void onEditFullName() {
        editField("Ho ten", lblFullName, value -> value.length() >= 2,
                "Ho ten phai co it nhat 2 ky tu");
    }

    @FXML
    private void onChangeEmail() {
        editField("Email", lblEmail,
                value -> EMAIL_PATTERN.matcher(value).matches(),
                "Email khong hop le");
    }

    @FXML
    private void onChangePhone() {
        editField("So dien thoai", lblPhone,
                value -> PHONE_PATTERN.matcher(value).matches(),
                "SDT phai co 10 so, bat dau bang 0");
    }

    @FXML
    private void onEditAddress() {
        editField("Dia chi", lblAddress,
                value -> value.length() >= 5,
                "Dia chi qua ngan");
    }

    @FXML
    private void onEditBirthday() {
        editField("Ngay sinh", lblBirthday,
                value -> value.matches("\\d{2}/\\d{2}/\\d{4}"),
                "Dinh dang: dd/MM/yyyy");
    }

    @FXML
    private void onEditDepositLimit() {
        editField("Han muc nap (VND)", lblDepositLimit,
                value -> value.replaceAll("[^\\d]", "").matches("\\d+"),
                "Phai la so");
    }

    /**
     * Generic edit field: dialog + validate + update label.
     * @param fieldLabel ten field hien thi cho user
     * @param targetLabel label de update
     * @param validator function check value hop le
     * @param errorMsg loi neu validate fail
     */
    private void editField(String fieldLabel, Label targetLabel,
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
        AlertUtil.showInfo("Cap nhat thanh cong (cho BE sync server)");
    }

    // ===== BAO MAT =====

    @FXML
    private void onChangePassword() {
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

        LOGGER.info("Yeu cau doi mat khau (cho BE bo sung API)");
        AlertUtil.showInfo("Yeu cau da gui. Cho BE bo sung API.");
    }

    @FXML
    private void onManage2FA() {
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
    private void onManagePayment() {
        AlertUtil.showInfo("Quan ly phuong thuc thanh toan dang phat trien");
    }

    // ===== TOGGLE NOTIFICATION =====

    @FXML
    private void onToggleEmailEnding() {
        savePreference(PREF_EMAIL_ENDING, cbEmailEnding.isSelected(), "Email ket thuc dau gia");
    }

    @FXML
    private void onToggleEmailOutbid() {
        savePreference(PREF_EMAIL_OUTBID, cbEmailOutbid.isSelected(), "Email bi outbid");
    }

    @FXML
    private void onToggleEmailPromo() {
        savePreference(PREF_EMAIL_PROMO, cbEmailPromo.isSelected(), "Email khuyen mai");
    }

    @FXML
    private void onTogglePush() {
        savePreference(PREF_PUSH_NOTI, cbPushNoti.isSelected(), "Push notification");
    }

    private void savePreference(String key, boolean value, String label) {
        PREFS.putBoolean(key, value);
        LOGGER.info(() -> label + " = " + value);
    }

    // ===== GIAO DIEN =====

    /**
     * Dark mode THUC SU: thay doi stylesheet cua Scene + persist.
     */
    @FXML
    private void onToggleDarkMode() {
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
    private void onChangeLanguage() {
        if (cbLanguage == null) return;
        String selected = cbLanguage.getValue();
        if (selected == null) return;

        PREFS.put(PREF_LANGUAGE, selected);
        LOGGER.info(() -> "Doi ngon ngu: " + selected);
        AlertUtil.showInfo("Khoi dong lai app de ap dung ngon ngu moi.");
    }

    // ===== VUNG NGUY HIEM =====

    @FXML
    private void onDeleteAccount() {
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