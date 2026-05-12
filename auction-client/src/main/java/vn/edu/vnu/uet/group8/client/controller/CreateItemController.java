package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * CreateItemController — form đăng sản phẩm mới (hoặc sửa).
 *
 * Validate:
 *   - Tên, danh mục, mô tả, giá khởi điểm, bước nhảy: bắt buộc
 *   - Giá khởi điểm > 0
 *   - Bước nhảy > 0
 *   - Duration: 1-168 giờ (1h - 1 tuần)
 *   - Nếu hasCert: certBody + certId bắt buộc
 *
 * Submit → SellerService.createItem() → navigate về SellerDashboard.
 */
public class CreateItemController implements Initializable {

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
        paneCertFields.setVisible(false);
        paneCertFields.setManaged(false);
    }

    private void initComboBoxes() {
        cbCategory.getItems().addAll(
                "Đồng hồ cao cấp", "Điện tử", "Trang sức", "Nghệ thuật",
                "Xe cổ", "Sách quý", "Đồ cổ", "Thời trang", "Khác"
        );

        cbCondition.getItems().addAll(
                "Mới 100%", "Như mới (99%)", "Tốt (90%)", "Khá (70%)", "Cũ (50%)"
        );
        cbCondition.getSelectionModel().selectFirst();
    }

    @FXML
    private void onToggleCert() {
        boolean show = cbHasCert.isSelected();
        paneCertFields.setVisible(show);
        paneCertFields.setManaged(show);
    }

    // ===== SUBMIT =====

    @FXML
    private void onSubmit() {
        String name = tfName.getText().trim();
        String category = cbCategory.getValue();
        String condition = cbCondition.getValue();
        String description = taDescription.getText().trim();
        String startPriceStr = tfStartPrice.getText().replaceAll("[^\\d]", "");
        String bidStepStr = tfBidStep.getText().replaceAll("[^\\d]", "");
        String durationStr = tfDurationHours.getText().trim();
        boolean hasCert = cbHasCert.isSelected();

        // Validate
        if (name.isEmpty() || category == null || description.isEmpty()
                || startPriceStr.isEmpty() || bidStepStr.isEmpty() || durationStr.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin bắt buộc");
            return;
        }

        BigDecimal startPrice;
        BigDecimal bidStep;
        int duration;

        try {
            startPrice = new BigDecimal(startPriceStr);
            bidStep = new BigDecimal(bidStepStr);
            duration = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            showError("Giá khởi điểm, bước nhảy, thời gian phải là số");
            return;
        }

        if (startPrice.compareTo(BigDecimal.ZERO) <= 0) {
            showError("Giá khởi điểm phải > 0");
            return;
        }

        if (bidStep.compareTo(BigDecimal.ZERO) <= 0) {
            showError("Bước nhảy giá phải > 0");
            return;
        }

        if (duration < 1 || duration > 168) {
            showError("Thời gian đấu giá phải từ 1 đến 168 giờ");
            return;
        }

        String certBody = null;
        String certId = null;
        if (hasCert) {
            certBody = tfCertBody.getText().trim();
            certId = tfCertId.getText().trim();
            if (certBody.isEmpty() || certId.isEmpty()) {
                showError("Vui lòng điền đầy đủ thông tin kiểm định");
                return;
            }
        }

        // Build specs map
        Map<String, String> specs = new HashMap<>();
        if (!tfBrand.getText().isBlank())    specs.put("brand", tfBrand.getText().trim());
        if (!tfModel.getText().isBlank())    specs.put("model", tfModel.getText().trim());
        if (!tfYear.getText().isBlank())     specs.put("year", tfYear.getText().trim());
        if (!tfMaterial.getText().isBlank()) specs.put("material", tfMaterial.getText().trim());
        if (!tfOrigin.getText().isBlank())   specs.put("origin", tfOrigin.getText().trim());

        // Build request
        SellerService.CreateItemRequest request = new SellerService.CreateItemRequest(
                name, category, condition, description,
                startPrice, bidStep, duration,
                specs, hasCert, certBody, certId
        );

        // Submit
        hideError();
        btnSubmit.setDisable(true);
        btnSubmit.setText("Đang đăng...");

        SellerService.createItem(request,
                // onSuccess
                item -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Đăng bán ngay");
                    AlertUtil.showInfo("Đăng sản phẩm thành công!");
                    SceneManager.switchTo("SellerDashboardView.fxml");
                },
                // onFailure
                errorMsg -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Đăng bán ngay");
                    showError(errorMsg);
                }
        );
    }

    @FXML
    private void onSaveDraft() {
        // TODO: BE cần endpoint lưu draft (status = DRAFT, không lên sàn)
        AlertUtil.showInfo("Tính năng đang phát triển");
    }

    @FXML
    private void onBack() {
        SceneManager.switchTo("SellerDashboardView.fxml");
    }

    // ===== HELPERS =====

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}