package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * CreateItemController — form dang san pham moi.
 *
 * Validate:
 *  - Name: 5-100 ky tu
 *  - Category, Condition: required (combobox)
 *  - Description: 20-2000 ky tu
 *  - StartPrice: > 0
 *  - BidStep: > 0, < startPrice (logical)
 *  - DurationHours: 1-168 (1h - 1 tuan)
 *  - Specs: optional - chi them neu khong rong
 *  - HasCert: neu tick -> certBody + certId required
 *
 * Format VND tren the fly khi user nhap so tien.
 */
public class CreateItemController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(CreateItemController.class.getName());

    private static final BigDecimal MIN_START_PRICE = new BigDecimal("100000");
    private static final BigDecimal MAX_START_PRICE = new BigDecimal("100000000000");  // 100 ty
    private static final int MIN_DURATION = 1;
    private static final int MAX_DURATION = 168;

    @FXML private TextField tfName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextArea taDescription;

    @FXML private TextField tfBrand;
    @FXML private TextField tfModel;
    @FXML private TextField tfYear;
    @FXML private TextField tfMaterial;
    @FXML private TextField tfOrigin;

    @FXML private TextField tfStartPrice;
    @FXML private TextField tfBidStep;
    @FXML private TextField tfDurationHours;

    @FXML private CheckBox cbHasCert;
    @FXML private VBox paneCertFields;
    @FXML private TextField tfCertBody;
    @FXML private TextField tfCertId;

    @FXML private Label lblError;
    @FXML private Button btnSubmit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComboBoxes();
        hideError();
        hideCertPane();
        setupPriceFormatting();
    }

    private void initComboBoxes() {
        if (cbCategory != null) {
            cbCategory.getItems().setAll(
                    "Dong ho cao cap", "Dien tu", "Trang suc", "Nghe thuat",
                    "Xe co", "Sach quy", "Do co", "Thoi trang", "Khac"
            );
        }
        if (cbCondition != null) {
            cbCondition.getItems().setAll(
                    "Moi 100%", "Nhu moi (99%)", "Tot (90%)", "Kha (70%)", "Cu (50%)"
            );
            cbCondition.getSelectionModel().selectFirst();
        }
    }

    /** Format gia VND khi user nhap (1000000 -> 1,000,000). */
    private void setupPriceFormatting() {
        if (tfStartPrice != null) {
            tfStartPrice.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) formatPriceField(tfStartPrice);
            });
        }
        if (tfBidStep != null) {
            tfBidStep.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) formatPriceField(tfBidStep);
            });
        }
    }

    private void formatPriceField(TextField field) {
        String input = field.getText().replaceAll("[^\\d]", "");
        if (input.isEmpty()) return;
        try {
            BigDecimal amount = new BigDecimal(input);
            field.setText(String.format("%,.0f", amount));
        } catch (NumberFormatException ignored) {}
    }

    @FXML
    private void onToggleCert() {
        if (cbHasCert != null && paneCertFields != null) {
            boolean show = cbHasCert.isSelected();
            paneCertFields.setVisible(show);
            paneCertFields.setManaged(show);
        }
    }

    private void hideCertPane() {
        if (paneCertFields != null) {
            paneCertFields.setVisible(false);
            paneCertFields.setManaged(false);
        }
    }

    // ===== SUBMIT =====

    @FXML
    private void onSubmit() {
        // Lay input
        String name = safeText(tfName);
        String category = cbCategory != null ? cbCategory.getValue() : null;
        String condition = cbCondition != null ? cbCondition.getValue() : null;
        String description = taDescription != null ? taDescription.getText().trim() : "";

        // Validate
        String error = validateRequired(name, category, condition, description);
        if (error != null) {
            showError(error);
            return;
        }

        BigDecimal startPrice = parseAmount(tfStartPrice);
        BigDecimal bidStep = parseAmount(tfBidStep);
        Integer duration = parseInt(tfDurationHours);

        if (startPrice == null || bidStep == null || duration == null) {
            showError("Gia khoi diem, buoc nhay, thoi gian phai la so");
            return;
        }

        String numError = validateNumbers(startPrice, bidStep, duration);
        if (numError != null) {
            showError(numError);
            return;
        }

        // Cert validate
        boolean hasCert = cbHasCert != null && cbHasCert.isSelected();
        String certBody = null, certId = null;
        if (hasCert) {
            certBody = safeText(tfCertBody);
            certId = safeText(tfCertId);
            if (certBody.isEmpty() || certId.isEmpty()) {
                showError("Vui long dien day du thong tin kiem dinh");
                return;
            }
        }

        // Build specs
        Map<String, String> specs = collectSpecs();

        // Build request
        SellerService.CreateItemRequest request = new SellerService.CreateItemRequest(
                name, category, condition, description,
                startPrice, bidStep, duration,
                specs, hasCert, certBody, certId
        );

        // Confirm
        boolean confirm = AlertUtil.showConfirm("Xac nhan dang ban",
                "San pham: " + name + "\n"
                        + "Gia khoi diem: " + formatVnd(startPrice) + "\n"
                        + "Thoi gian: " + duration + " gio\n\n"
                        + "Dang ban san pham nay?");
        if (!confirm) return;

        // Submit
        hideError();
        setLoadingState(true);

        SellerService.createItem(request,
                item -> {
                    LOGGER.info(() -> "Dang ban thanh cong: " + name);
                    setLoadingState(false);
                    AlertUtil.showInfo("Dang ban san pham thanh cong!");
                    navigateToDashboard();
                },
                errorMsg -> {
                    LOGGER.warning("Dang ban that bai: " + errorMsg);
                    setLoadingState(false);
                    showError(errorMsg);
                }
        );
    }

    private String validateRequired(String name, String category, String condition, String description) {
        if (name.isEmpty()) return "Vui long nhap ten san pham";
        if (name.length() < 5 || name.length() > 100) return "Ten 5-100 ky tu";
        if (category == null) return "Vui long chon danh muc";
        if (condition == null) return "Vui long chon tinh trang";
        if (description.isEmpty()) return "Vui long nhap mo ta";
        if (description.length() < 20) return "Mo ta toi thieu 20 ky tu";
        if (description.length() > 2000) return "Mo ta toi da 2000 ky tu";
        return null;
    }

    private String validateNumbers(BigDecimal startPrice, BigDecimal bidStep, int duration) {
        if (startPrice.compareTo(MIN_START_PRICE) < 0) {
            return "Gia khoi diem toi thieu " + formatVnd(MIN_START_PRICE);
        }
        if (startPrice.compareTo(MAX_START_PRICE) > 0) {
            return "Gia khoi diem toi da " + formatVnd(MAX_START_PRICE);
        }
        if (bidStep.compareTo(BigDecimal.ZERO) <= 0) {
            return "Buoc nhay phai > 0";
        }
        if (bidStep.compareTo(startPrice) >= 0) {
            return "Buoc nhay phai < gia khoi diem";
        }
        if (duration < MIN_DURATION || duration > MAX_DURATION) {
            return "Thoi gian " + MIN_DURATION + "-" + MAX_DURATION + " gio";
        }
        return null;
    }

    private Map<String, String> collectSpecs() {
        Map<String, String> specs = new HashMap<>();
        addSpec(specs, "brand", tfBrand);
        addSpec(specs, "model", tfModel);
        addSpec(specs, "year", tfYear);
        addSpec(specs, "material", tfMaterial);
        addSpec(specs, "origin", tfOrigin);
        return specs;
    }

    private void addSpec(Map<String, String> specs, String key, TextField field) {
        if (field == null) return;
        String value = field.getText();
        if (value != null && !value.isBlank()) {
            specs.put(key, value.trim());
        }
    }

    @FXML
    private void onSaveDraft() {
        // TODO: BE bo sung endpoint luu draft (status=DRAFT)
        AlertUtil.showInfo("Tinh nang luu nhap dang phat trien");
    }

    @FXML
    private void onBack() {
        navigateToDashboard();
    }

    private void navigateToDashboard() {
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadView("SellerDashboardView.fxml");
        } else {
            SceneManager.switchTo("SellerDashboardView.fxml");
        }
    }

    private void setLoadingState(boolean loading) {
        if (btnSubmit != null) {
            btnSubmit.setDisable(loading);
            btnSubmit.setText(loading ? "Dang gui..." : "Dang ban ngay");
        }
    }

    // ===== HELPERS =====

    private String safeText(TextField field) {
        if (field == null) return "";
        String text = field.getText();
        return text != null ? text.trim() : "";
    }

    private BigDecimal parseAmount(TextField field) {
        if (field == null) return null;
        String input = field.getText().replaceAll("[^\\d]", "");
        if (input.isEmpty()) return null;
        try {
            return new BigDecimal(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(TextField field) {
        if (field == null) return null;
        String input = field.getText().trim();
        if (input.isEmpty()) return null;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatVnd(BigDecimal amount) {
        return amount == null ? "0 d" : String.format("%,.0f d", amount);
    }

    private void showError(String msg) {
        if (lblError == null) return;
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        if (lblError == null) return;
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}