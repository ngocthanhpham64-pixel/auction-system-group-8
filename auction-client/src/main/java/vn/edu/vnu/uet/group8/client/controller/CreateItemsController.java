package vn.edu.vnu.uet.group8.client.controller;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.ModalUtil;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.request.CreateItemRequest;

public class CreateItemsController {

    private static final Logger log = LoggerFactory.getLogger(CreateItemsController.class);

    // --- MỤC 1: HÌNH ẢNH SẢN PHẨM ---
    @FXML private VBox paneImageUpload;
    @FXML private VBox vboxUploadPlaceholder;
    @FXML private VBox vboxImagePreview;
    @FXML private ImageView imgProduct;
    @FXML private Button btnChooseImage;
    @FXML private javafx.scene.control.Label lblImageCount;
    
    private java.util.ArrayList<String> productBase64Images = new java.util.ArrayList<>();
    private java.util.ArrayList<Image> localImages = new java.util.ArrayList<>();
    private int currentImageIndex = 0;
    
    private boolean isChoosing = false;

    // --- MỤC 2: THÔNG TIN SẢN PHẨM ---
    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextArea txtDescription;
    @FXML private VBox vboxDynamicSpecs; // Vùng chứa các thông số kỹ thuật động nếu có

    // --- MỤC 3: THÔNG TIN ĐẤU GIÁ ---
    @FXML private TextField txtStartPrice;

    // Thời gian bắt đầu
    @FXML private DatePicker dpStartDate;
    @FXML private ComboBox<String> cbStartHour;
    @FXML private ComboBox<String> cbStartMinute;

    // Thời gian kết thúc
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<String> cbEndHour;
    @FXML private ComboBox<String> cbEndMinute;

    // Nút đăng bài
    @FXML private Button btnSubmit;
    @FXML private Button btnSaveDraft;
    @FXML private Button btnDeleteDraft;
    private Integer editingItemId = null;

    @FXML
    public void initialize() {
        // 1. Tự động nạp dữ liệu cho các ô chọn Giờ (00 - 23) và Phút (00 - 59)
        populateTimeComboBoxes();

        // 2. Thiết lập thời gian mặc định khi vừa mở trang cho người dùng đỡ phải chọn nhiều
        setDefaultDateTime();

        // 3. Nạp danh sách danh mục và tình trạng động từ Enum hệ thống
        cbCategory.getItems().clear();
        for (vn.edu.vnu.uet.group8.common.enums.ItemCategory cat : vn.edu.vnu.uet.group8.common.enums.ItemCategory.values()) {
            cbCategory.getItems().add(cat.getLabel());
        }

        cbCondition.getItems().clear();
        for (vn.edu.vnu.uet.group8.common.enums.ItemCondition cond : vn.edu.vnu.uet.group8.common.enums.ItemCondition.values()) {
            cbCondition.getItems().add(cond.getLabel());
        }

        // 4. Kiểm tra xem có đang sửa nháp không
        AuctionItemDTO draftItem = ClientModel.getInstance().getCurrentAuctionItem();
        boolean isDraftEdit = draftItem != null && ((draftItem.getStatus() != null && "DRAFT".equals(draftItem.getStatus().name())) 
                              || (draftItem.getCurrentPrice() == null && draftItem.getEndTime() == null));
        if (isDraftEdit) {
            editingItemId = draftItem.getItemId();
            txtTitle.setText(draftItem.getTitle());
            txtDescription.setText(draftItem.getDescription());
            if (draftItem.getCategory() != null) cbCategory.setValue(draftItem.getCategory().getLabel());
            if (draftItem.getCondition() != null) cbCondition.setValue(draftItem.getCondition().getLabel());
            
            // Hiện nút xóa nháp
            if (btnDeleteDraft != null) {
                btnDeleteDraft.setVisible(true);
                btnDeleteDraft.setManaged(true);
            }

            // Tải lại các ảnh cũ của bản nháp
            if (draftItem.getImageUrls() != null && !draftItem.getImageUrls().isEmpty()) {
                for (String url : draftItem.getImageUrls()) {
                    try {
                        Image img = new Image(url);
                        localImages.add(img);
                        productBase64Images.add(url);
                    } catch (Exception e) {
                        log.error("Lỗi tải ảnh nháp: {}", e.getMessage());
                    }
                }
                if (!productBase64Images.isEmpty()) {
                    currentImageIndex = 0;
                    updateImagePreview();
                }
            }

            // Xóa khỏi model để không ảnh hưởng lần sau
            ClientModel.getInstance().setCurrentAuctionItem(null);
        }
    }

    /**
     * Hàm nạp tự động dữ liệu 2 chữ số vào ComboBox Giờ và Phút
     */
    private void populateTimeComboBoxes() {
        // Nạp 24 giờ (từ 00 đến 23)
        for (int i = 0; i <= 23; i++) {
            String hour = String.format("%02d", i);
            cbStartHour.getItems().add(hour);
            cbEndHour.getItems().add(hour);
        }

        // Nạp 60 phút (từ 00 đến 59)
        for (int i = 0; i <= 59; i++) {
            String minute = String.format("%02d", i);
            cbStartMinute.getItems().add(minute);
            cbEndMinute.getItems().add(minute);
        }
    }

    /**
     * Hàm đặt ngày giờ mặc định: Bắt đầu từ hôm nay lúc 08:00, kết thúc sau 3 ngày lúc 20:00
     */
    private void setDefaultDateTime() {
        dpStartDate.setValue(LocalDate.now());
        cbStartHour.setValue("08");
        cbStartMinute.setValue("00");

        dpEndDate.setValue(LocalDate.now().plusDays(3)); // Mặc định phiên đấu giá kéo dài 3 ngày
        cbEndHour.setValue("20");
        cbEndMinute.setValue("00");
    }

    /**
     * Hàm mở FileChooser chọn ảnh và lưu Base64 cùng với preview cục bộ
     */
    @FXML
    public void chooseImage() {
        if (isChoosing) return;
        isChoosing = true;
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn ảnh sản phẩm");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Hình ảnh (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
            );
            
            // Cho phép chọn nhiều file nếu muốn, nhưng ở đây có thể chọn từng file một
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(btnSubmit.getScene().getWindow());
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                for (File file : selectedFiles) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    String base64 = Base64.getEncoder().encodeToString(fileContent);
                    productBase64Images.add(base64);
                    
                    Image localImg = new Image(file.toURI().toString());
                    localImages.add(localImg);
                }
                
                currentImageIndex = productBase64Images.size() - 1;
                updateImagePreview();
            }
        } catch (Exception e) {
            AlertUtil.showError("Không thể đọc tệp ảnh: " + e.getMessage());
        } finally {
            isChoosing = false;
        }
    }

    private void updateImagePreview() {
        if (productBase64Images.isEmpty()) {
            vboxUploadPlaceholder.setVisible(true);
            vboxUploadPlaceholder.setManaged(true);
            vboxImagePreview.setVisible(false);
            vboxImagePreview.setManaged(false);
            imgProduct.setImage(null);
        } else {
            vboxUploadPlaceholder.setVisible(false);
            vboxUploadPlaceholder.setManaged(false);
            vboxImagePreview.setVisible(true);
            vboxImagePreview.setManaged(true);
            
            imgProduct.setImage(localImages.get(currentImageIndex));
            lblImageCount.setText((currentImageIndex + 1) + " / " + productBase64Images.size());
        }
    }

    @FXML
    public void nextImage() {
        if (!productBase64Images.isEmpty() && currentImageIndex < productBase64Images.size() - 1) {
            currentImageIndex++;
            updateImagePreview();
        }
    }

    @FXML
    public void prevImage() {
        if (!productBase64Images.isEmpty() && currentImageIndex > 0) {
            currentImageIndex--;
            updateImagePreview();
        }
    }

    @FXML
    public void removeImage() {
        if (!productBase64Images.isEmpty()) {
            productBase64Images.remove(currentImageIndex);
            localImages.remove(currentImageIndex);
            
            if (currentImageIndex >= productBase64Images.size() && currentImageIndex > 0) {
                currentImageIndex--;
            }
            updateImagePreview();
        }
    }

    @FXML
    public void onBackClick() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().switchView("ItemDashboard.fxml", "SELLER");
        }
    }

    /**
     * Hành động khi bấm nút Lưu Nháp
     */
    @FXML
    public void handleSaveDraft() {
        submitData(true);
    }

    @FXML
    public void handleCreateAuction() {
        submitData(false);
    }

    @FXML
    public void handleDeleteDraft() {
        if (editingItemId == null) return;

        boolean confirm = AlertUtil.showConfirm("Xác nhận xóa", "Bạn có chắc chắn muốn xóa bản nháp này không?");
        if (confirm) {
            btnSubmit.setDisable(true);
            if (btnSaveDraft != null) btnSaveDraft.setDisable(true);
            if (btnDeleteDraft != null) btnDeleteDraft.setDisable(true);

            SellerService.deleteItem(editingItemId, success -> javafx.application.Platform.runLater(() -> {
                btnSubmit.setDisable(false);
                if (btnSaveDraft != null) btnSaveDraft.setDisable(false);
                if (btnDeleteDraft != null) btnDeleteDraft.setDisable(false);

                if (success) {
                    AlertUtil.showInfo("Xóa bản nháp thành công!");
                    if (MainController.getInstance() != null) {
                        MainController.getInstance().switchView("ItemDashboard.fxml", "SELLER");
                    }
                } else {
                    AlertUtil.showError("Xóa bản nháp thất bại!");
                }
            }));
        }
    }

    private void submitData(boolean isDraft) {
        try {
            // Lấy các thông tin cơ bản
            String title = txtTitle.getText().trim();
            String categoryLabel = cbCategory.getValue();
            String conditionLabel = cbCondition.getValue();
            String description = txtDescription.getText().trim();
            String startPriceStr = txtStartPrice.getText().trim();

            // Kiểm tra validate cơ bản
            if (title.isEmpty() || categoryLabel == null || conditionLabel == null) {
                AlertUtil.showError("Vui lòng nhập tối thiểu Tên, Danh mục và Tình trạng!");
                return;
            }

            int durationMinutes = 0;
            BigDecimal startPrice = null;
            BigDecimal bidStep = null;
            Long startTimeMillis = null;

            if (!isDraft) {
                if (startPriceStr.isEmpty()) {
                    AlertUtil.showError("Vui lòng nhập Giá khởi điểm để đăng bài!");
                    return;
                }
                try {
                    startPrice = new BigDecimal(startPriceStr);
                    if (startPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        AlertUtil.showError("Giá khởi điểm phải lớn hơn 0!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Giá khởi điểm không hợp lệ!");
                    return;
                }

                // Tính toán thời gian đấu giá
                LocalDate startDate = dpStartDate.getValue();
                String startHour = cbStartHour.getValue();
                String startMinute = cbStartMinute.getValue();
                LocalDate endDate = dpEndDate.getValue();
                String endHour = cbEndHour.getValue();
                String endMinute = cbEndMinute.getValue();

                if (startDate == null || startHour == null || startMinute == null ||
                    endDate == null || endHour == null || endMinute == null) {
                    AlertUtil.showError("Vui lòng nhập đầy đủ thời gian bắt đầu và kết thúc!");
                    return;
                }

                try {
                    java.time.LocalDateTime startLDT = java.time.LocalDateTime.of(
                        startDate,
                        java.time.LocalTime.of(Integer.parseInt(startHour), Integer.parseInt(startMinute))
                    );
                    java.time.LocalDateTime endLDT = java.time.LocalDateTime.of(
                        endDate,
                        java.time.LocalTime.of(Integer.parseInt(endHour), Integer.parseInt(endMinute))
                    );
                    startTimeMillis = startLDT.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (startLDT.isBefore(now.minusMinutes(5))) {
                        AlertUtil.showError("Thời gian bắt đầu không được ở quá khứ!");
                        return;
                    }
                    if (endLDT.isBefore(now)) {
                        AlertUtil.showError("Thời gian kết thúc không được ở quá khứ!");
                        return;
                    }

                    if (!endLDT.isAfter(startLDT)) {
                        AlertUtil.showError("Thời gian kết thúc phải sau thời gian bắt đầu!");
                        return;
                    }

                    java.time.Duration duration = java.time.Duration.between(startLDT, endLDT);
                    durationMinutes = (int) duration.toMinutes();
                    if (durationMinutes <= 0) {
                        AlertUtil.showError("Thời gian kết thúc phải sau thời gian bắt đầu!");
                        return;
                    }
                } catch (Exception e) {
                    AlertUtil.showError("Thời gian bắt đầu hoặc kết thúc không hợp lệ!");
                    return;
                }
            }

            // Ánh xạ enums sang tên hằng để Server phân giải được
            vn.edu.vnu.uet.group8.common.enums.ItemCategory category = 
                vn.edu.vnu.uet.group8.common.enums.ItemCategory.fromLabel(categoryLabel);
            vn.edu.vnu.uet.group8.common.enums.ItemCondition condition = 
                vn.edu.vnu.uet.group8.common.enums.ItemCondition.fromLabel(conditionLabel);

            List<String> images = new java.util.ArrayList<>(productBase64Images);

            CreateItemRequest request = new CreateItemRequest(
                title, category.name(), condition.name(), description,
                startPrice, bidStep, durationMinutes, startTimeMillis,
                images
            );

            btnSubmit.setDisable(true);
            if (btnSaveDraft != null) btnSaveDraft.setDisable(true);

            if (editingItemId != null) {
                if (!isDraft) {
                    // Đăng bài từ bản nháp: Do Server không hỗ trợ publish từ update, ta xóa nháp cũ và tạo mới hoàn toàn
                    SellerService.deleteItem(editingItemId, successDel -> {
                        if (successDel) {
                            SellerService.createItem(request, 
                                createdItem -> javafx.application.Platform.runLater(() -> {
                                    btnSubmit.setDisable(false);
                                    if (btnSaveDraft != null) btnSaveDraft.setDisable(false);
                                    ModalUtil.showModal("ĐĂNG BÀI THÀNH CÔNG", "SuccessContent.fxml");
                                    if (MainController.getInstance() != null) MainController.getInstance().switchView("ItemDashboard.fxml", "SELLER");
                                }),
                                errorMessage -> javafx.application.Platform.runLater(() -> {
                                    btnSubmit.setDisable(false);
                                    if (btnSaveDraft != null) btnSaveDraft.setDisable(false);
                                    AlertUtil.showError("Đăng bài thất bại: " + errorMessage);
                                })
                            );
                        } else {
                            javafx.application.Platform.runLater(() -> {
                                btnSubmit.setDisable(false);
                                if (btnSaveDraft != null) btnSaveDraft.setDisable(false);
                                AlertUtil.showError("Không thể chuyển đổi bản nháp. Vui lòng thử lại!");
                            });
                        }
                    });
                } else {
                    // Nếu chỉ ấn lại nút "Lưu nháp" thì cập nhật bình thường
                    SellerService.updateItem(editingItemId, request, success -> javafx.application.Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        if (btnSaveDraft != null) btnSaveDraft.setDisable(false);
                        if (success) {
                            ModalUtil.showModal("CẬP NHẬT NHÁP THÀNH CÔNG", "SuccessContent.fxml");
                            if (MainController.getInstance() != null) MainController.getInstance().switchView("ItemDashboard.fxml", "SELLER");
                        } else {
                            AlertUtil.showError("Cập nhật thất bại!");
                        }
                    }));
                }
            } else {
                SellerService.createItem(request,
                    createdItem -> javafx.application.Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        if (btnSaveDraft != null) btnSaveDraft.setDisable(false);
                        ModalUtil.showModal("THÀNH CÔNG", "SuccessContent.fxml");
                        if (MainController.getInstance() != null) MainController.getInstance().switchView("ItemDashboard.fxml", "SELLER");
                    }),
                    errorMessage -> javafx.application.Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        if (btnSaveDraft != null) btnSaveDraft.setDisable(false);
                        AlertUtil.showError("Thất bại: " + errorMessage);
                    })
                );
            }

        } catch (Exception e) {
            log.error("Lỗi khi tạo bài đấu giá: {}", e.getMessage());
            AlertUtil.showError("Lỗi hệ thống: " + e.getMessage());
        }
    }
}