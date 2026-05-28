package vn.edu.vnu.uet.group8.client.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;

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

    protected static final Logger LOGGER = Logger.getLogger(CreateItemController.class.getName());

    protected static final BigDecimal MIN_START_PRICE = new BigDecimal("100000");
    protected static final BigDecimal MAX_START_PRICE = new BigDecimal("100000000000");  // 100 ty
    protected static final int MIN_DURATION = 1;
    protected static final int MAX_DURATION = 168;

    @FXML TextField tfName;
    @FXML ComboBox<String> cbCategory;
    @FXML ComboBox<String> cbCondition;
    @FXML TextArea taDescription;

    @FXML TextField tfBrand;
    @FXML TextField tfModel;
    @FXML TextField tfYear;
    @FXML TextField tfMaterial;
    @FXML TextField tfOrigin;

    @FXML TextField tfStartPrice;
    @FXML TextField tfBidStep;
    @FXML TextField tfDurationHours;

    @FXML CheckBox cbHasCert;
    @FXML VBox paneCertFields;
    @FXML TextField tfCertBody;
    @FXML TextField tfCertId;

    @FXML Label lblError;
    @FXML Button btnSubmit;

    // Lưu trữ các chuỗi Base64 của ảnh được tải lên
    protected final List<String> base64Images = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComboBoxes();
        hideError();
        hideCertPane();
        setupPriceFormatting();
    }

    void initComboBoxes() {
        if (cbCategory != null) {
            cbCategory.getItems().setAll(
                    ItemCategory.WATCHES.getLabel(), ItemCategory.ELECTRONICS.getLabel(), ItemCategory.JEWELRY.getLabel(), ItemCategory.ART.getLabel(),
                    ItemCategory.VEHICLES.getLabel(), ItemCategory.BOOKS.getLabel(), ItemCategory.ANTIQUES.getLabel(), ItemCategory.FASHION.getLabel(), ItemCategory.OTHER.getLabel()
            );
        }
        if (cbCondition != null) {
            cbCondition.getItems().setAll(
                    ItemCondition.NEW.getLabel(), ItemCondition.LIKE_NEW.getLabel(), ItemCondition.USED.getLabel()
            );
            cbCondition.getSelectionModel().selectFirst();
        }
    }

    /** Format gia VND khi user nhap (1000000 -> 1,000,000). */
    void setupPriceFormatting() {
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

    void formatPriceField(TextField field) {
        String input = field.getText().replaceAll("[^\\d]", "");
        if (input.isEmpty()) return;
        try {
            BigDecimal amount = new BigDecimal(input);
            field.setText(String.format("%,.0f", amount));
        } catch (NumberFormatException ignored) {}
    }

    @FXML
    void onToggleCert() {
        if (cbHasCert != null && paneCertFields != null) {
            boolean show = cbHasCert.isSelected();
            paneCertFields.setVisible(show);
            paneCertFields.setManaged(show);
        }
    }

    void hideCertPane() {
        if (paneCertFields != null) {
            paneCertFields.setVisible(false);
            paneCertFields.setManaged(false);
        }
    }

    // Gắn hàm này vào một nút "Thêm ảnh" (vd: btnAddImage) trên giao diện FXML
    @FXML
    void onChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                String encodedString = Base64.getEncoder().encodeToString(fileContent);
                base64Images.add(encodedString);
                AlertUtil.showInfo("Đã thêm 1 ảnh (" + selectedFile.getName() + ")");
            } catch (Exception e) {
                showError("Lỗi đọc file ảnh");
            }
        }
    }

    // ===== SUBMIT =====

    @FXML
    void onSubmit() {
        // Lay input
        String name = safeText(tfName);
        String categoryLabel = cbCategory != null ? cbCategory.getValue() : null;
        String conditionLabel = cbCondition != null ? cbCondition.getValue() : null;
        String description = taDescription != null ? taDescription.getText().trim() : "";

        // Validate
        String error = validateRequired(name, categoryLabel, conditionLabel, description);
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

        // Chuyển đổi label tiếng Việt sang tên Enum tiếng Anh
        String categoryEnumName = getCategoryNameFromLabel(categoryLabel);
        String conditionEnumName = getConditionNameFromLabel(conditionLabel);

        // Build specs
        Map<String, String> specs = collectSpecs();
        
        if (hasCert) {
            specs.put("isVerified", "true");
            specs.put("certBody", certBody);
            specs.put("certId", certId);
        }

        // Build request
        SellerService.CreateItemRequest request = new SellerService.CreateItemRequest(
                name, categoryEnumName, conditionEnumName, description,
                startPrice, bidStep, duration,
                specs, base64Images, hasCert, certBody, certId
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

    protected String validateRequired(String name, String category, String condition, String description) {
        if (name.isEmpty()) return "Vui long nhap ten san pham"; // 'category' and 'condition' are now labels
        if (name.length() < 5 || name.length() > 100) return "Ten 5-100 ky tu";
        if (category == null) return "Vui long chon danh muc";
        if (condition == null) return "Vui long chon tinh trang";
        if (description.isEmpty()) return "Vui long nhap mo ta";
        if (description.length() < 20) return "Mo ta toi thieu 20 ky tu";
        if (description.length() > 2000) return "Mo ta toi da 2000 ky tu";
        return null;
    }

    protected String validateNumbers(BigDecimal startPrice, BigDecimal bidStep, int duration) {
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

    protected Map<String, String> collectSpecs() {
        Map<String, String> specs = new HashMap<>();
        addSpec(specs, "brand", tfBrand);
        addSpec(specs, "model", tfModel);
        addSpec(specs, "year", tfYear);
        addSpec(specs, "material", tfMaterial);
        addSpec(specs, "origin", tfOrigin);
        return specs;
    }

    void addSpec(Map<String, String> specs, String key, TextField field) {
        if (field == null) return;
        String value = field.getText();
        if (value != null && !value.isBlank()) {
            specs.put(key, value.trim());
        }
    }

    @FXML
    void onSaveDraft() {
        // TODO: BE bo sung endpoint luu draft (status=DRAFT)
        AlertUtil.showInfo("Tinh nang luu nhap dang phat trien");
    }

    @FXML
    void onBack() {
        navigateToDashboard();
    }

    void navigateToDashboard() {
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadView("SellerDashboardView.fxml");
        } else {
            SceneManager.switchTo("SellerDashboardView.fxml");
        }
    }

    void setLoadingState(boolean loading) {
        if (btnSubmit != null) {
            btnSubmit.setDisable(loading);
            btnSubmit.setText(loading ? "Dang gui..." : "Dang ban ngay");
        }
    }

    // ===== HELPERS =====

    protected String safeText(TextField field) {
        if (field == null) return "";
        String text = field.getText();
        return text != null ? text.trim() : "";
    }

    protected BigDecimal parseAmount(TextField field) {
        if (field == null) return null;
        String input = field.getText().replaceAll("[^\\d]", "");
        if (input.isEmpty()) return null;
        try {
            return new BigDecimal(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Integer parseInt(TextField field) {
        if (field == null) return null;
        String input = field.getText().trim();
        if (input.isEmpty()) return null;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected String formatVnd(BigDecimal amount) {
        return amount == null ? "0 d" : String.format("%,.0f d", amount);
    }

    void showError(String msg) {
        if (lblError == null) return;
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    void hideError() {
        if (lblError == null) return;
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    /**
     * Chuyển đổi nhãn danh mục tiếng Việt sang tên Enum tiếng Anh.
     */
    protected String getCategoryNameFromLabel(String label) {
        if (label == null) return ItemCategory.OTHER.name();
        for (ItemCategory category : ItemCategory.values()) {
            if (category.getLabel().equalsIgnoreCase(label)) {
                return category.name();
            }
        }
        // Fallback hoặc ném lỗi nếu không tìm thấy
        LOGGER.warning("Unknown category label: " + label);
        return ItemCategory.OTHER.name(); // Mặc định là OTHER
    }

    /**
     * Chuyển đổi nhãn tình trạng tiếng Việt sang tên Enum tiếng Anh.
     */
    protected String getConditionNameFromLabel(String label) {
        if (label == null) return ItemCondition.USED.name();
        for (ItemCondition condition : ItemCondition.values()) {
            if (condition.getLabel().equalsIgnoreCase(label)) {
                return condition.name();
            }
        }
        // Do các nhãn "Tốt (90%)", "Khá (70%)", "Cũ (50%)" đều map về USED
        if (label.equals("Tot (90%)") || label.equals("Kha (70%)") || label.equals("Cu (50%)")) {
            return ItemCondition.USED.name();
        }
        LOGGER.warning("Unknown condition label: " + label);
        return ItemCondition.USED.name(); // Mặc định là USED
    }
}