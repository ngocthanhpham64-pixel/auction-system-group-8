package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import vn.edu.vnu.uet.group8.client.service.RegisterService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * RegisterController — man hinh dang ky tai khoan.
 *
 * Validate phia client (fail fast):
 *  - All fields required
 *  - Username: 3-20 ky tu, chi alphanumeric + underscore
 *  - Email: format chuan
 *  - Phone: 10 so, bat dau 0
 *  - Password: >= 8 ky tu, co chua chu va so
 *  - Confirm password phai khop
 *  - Phai tick dieu khoan
 *
 * Sau khi dang ky thanh cong -> chuyen ve LoginView.
 */
public class RegisterController implements Initializable {

    protected static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());

    // Validators
    protected static final Pattern EMAIL_PATTERN    = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    protected static final Pattern PHONE_PATTERN    = Pattern.compile("^0\\d{9}$");
    protected static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    protected static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    @FXML TextField tfUsername;
    @FXML TextField tfFullName;
    @FXML TextField tfEmail;
    @FXML TextField tfPhone;
    @FXML PasswordField pfPassword;
    @FXML PasswordField pfConfirmPassword;
    @FXML CheckBox cbTerms;
    @FXML Hyperlink linkTerms;
    @FXML Label lblError;
    @FXML Button btnSubmit;
    @FXML Button btnTabLogin;
    @FXML Button btnTabRegister;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideError();
        setupEnterKeySubmit();
    }

    void setupEnterKeySubmit() {
        if (pfConfirmPassword != null) {
            pfConfirmPassword.setOnAction(e -> onRegister());
        }
    }

    @FXML
    void onRegister() {
        // Lay tat ca input
        String username = safeText(tfUsername);
        String fullName = safeText(tfFullName);
        String email    = safeText(tfEmail);
        String phone    = safeText(tfPhone);
        String password = pfPassword != null ? pfPassword.getText() : "";
        String confirm  = pfConfirmPassword != null ? pfConfirmPassword.getText() : "";

        // ===== VALIDATE =====
        String error = validate(username, fullName, email, phone, password, confirm);
        if (error != null) {
            showError(error);
            return;
        }

        hideError();
        setLoadingState(true);

        RegisterService.register(username, email, password, fullName, phone,
                () -> {
                    LOGGER.info(() -> "Dang ky thanh cong: " + username);
                    setLoadingState(false);
                    AlertUtil.showInfo("Dang ky thanh cong!\n"
                            + "Vui long dang nhap voi tai khoan " + username);
                    SceneManager.switchTo(SceneManager.VIEW_LOGIN);
                },
                errorMsg -> {
                    LOGGER.warning("Dang ky that bai: " + errorMsg);
                    setLoadingState(false);
                    showError(errorMsg != null ? errorMsg : "Dang ky that bai");
                }
        );
    }

    /**
     * Validate tat ca field. Return error message hoac null neu OK.
     */
    protected String validate(String username, String fullName, String email,
                            String phone, String password, String confirm) {

        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty()
                || phone.isEmpty() || password.isEmpty()) {
            return "Vui long dien day du thong tin";
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Ten dang nhap 3-20 ky tu, chi chu/so/gach duoi";
        }

        if (fullName.length() < 2 || fullName.length() > 50) {
            return "Ho ten phai 2-50 ky tu";
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Email khong hop le";
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return "SDT phai 10 so, bat dau bang 0";
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return "Mat khau >= 8 ky tu, co chua chu va so";
        }

        if (!password.equals(confirm)) {
            return "Mat khau xac nhan khong khop";
        }

        if (cbTerms != null && !cbTerms.isSelected()) {
            return "Vui long dong y voi dieu khoan su dung";
        }

        return null;
    }

    @FXML
    void onTabLogin() {
        SceneManager.switchTo(SceneManager.VIEW_LOGIN);
    }

    @FXML
    void onTabRegister() {
        // Da o RegisterView
    }

    @FXML
    void onShowTerms() {
        AlertUtil.showInfo(
                "DIEU KHOAN SU DUNG AUCTIVA\n\n"
                        + "1. Nguoi dung cam ket cung cap thong tin chinh xac.\n\n"
                        + "2. Moi giao dich dau gia co hieu luc phap ly.\n\n"
                        + "3. Cam gia mao, lua dao, gian lan.\n\n"
                        + "4. Tuan thu phap luat Viet Nam.\n\n"
                        + "5. Auctiva co quyen khoa tai khoan vi pham."
        );
    }

    void setLoadingState(boolean loading) {
        if (btnSubmit == null) return;
        btnSubmit.setDisable(loading);
        btnSubmit.setText(loading ? "Dang dang ky..." : "Dang ky");
    }

    // ===== HELPERS =====

    protected String safeText(TextField field) {
        if (field == null) return "";
        String text = field.getText();
        return text != null ? text.trim() : "";
    }

    void showError(String message) {
        if (lblError == null) return;
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    void hideError() {
        if (lblError == null) return;
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}