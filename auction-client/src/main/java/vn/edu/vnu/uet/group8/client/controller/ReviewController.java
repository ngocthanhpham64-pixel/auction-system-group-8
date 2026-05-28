package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import vn.edu.vnu.uet.group8.client.service.RatingService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

public class ReviewController {

    @FXML private ComboBox<String> cbScore;
    @FXML private TextArea txtComment;
    @FXML private Label lblSellerName;
    @FXML private Label lblSellerAvatarInitials;
    @FXML private Label lblStars;
    @FXML private Button btnSubmit;

    private int selectedScore = 0;
    private int sellerId;
    private String sellerUsername;

    @FXML
    public void initialize() {
        // Initialize ComboBox listener
        cbScore.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedScore = Integer.parseInt(newVal.split(" - ")[0]); // Parse score from "X - Description"
                updateStars(selectedScore);
            } else {
                selectedScore = 0;
                updateStars(0);
            }
        });
        updateStars(0); // Initial state
        
        // Fallback to ClientModel if accessed without setSellerInfo
        vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
        if (item != null && this.sellerId == 0) {
            setSellerInfo(item.getSellerId(), item.getSellerUsername(), item.getSellerUsername());
        }
    }

    /**
     * Phương thức này được gọi từ bên ngoài (ví dụ: từ NotificationController)
     * để truyền thông tin người bán vào form đánh giá.
     */
    public void setSellerInfo(int sellerId, String sellerName, String sellerUsername) {
        this.sellerId = sellerId;
        this.sellerUsername = sellerUsername;
        lblSellerName.setText(sellerName);
        if (sellerName != null && !sellerName.isEmpty()) {
            lblSellerAvatarInitials.setText(sellerName.substring(0, 1).toUpperCase());
        } else {
            lblSellerAvatarInitials.setText("?");
        }
    }

    private void updateStars(int score) {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= score) {
                stars.append("⭐"); // Filled star
            } else {
                stars.append("☆"); // Empty star
            }
        }
        lblStars.setText(stars.toString());
    }

    @FXML
    public void onSubmit() {
        if (selectedScore == 0) {
            AlertUtil.showError("Vui lòng chọn mức độ hài lòng!");
            return;
        }
        if (this.sellerId == 0) {
            AlertUtil.showError("Không thể xác định người bán để đánh giá.");
            return;
        }
        String comment = txtComment.getText().trim();
        String raterUsername = SessionManager.getUsername(); // Get current user's username
        
        vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
        int currentItemId = item != null ? item.getItemId() : 0;

        btnSubmit.setDisable(true);
        btnSubmit.setText("ĐANG GỬI...");

        RatingService.rateSeller(currentItemId, this.sellerId, raterUsername, selectedScore, comment, success -> {
            Platform.runLater(() -> {
                btnSubmit.setDisable(false);
                btnSubmit.setText("GỬI ĐÁNH GIÁ");
                if (success) {
                    AlertUtil.showInfo("Cảm ơn bạn đã đánh giá!");
                    if (btnSubmit.getScene() != null && btnSubmit.getScene().getWindow() != null) {
                        btnSubmit.getScene().getWindow().hide();
                    }
                } else {
                    AlertUtil.showError("Đánh giá thất bại. Vui lòng thử lại.");
                }
            });
        }, err -> Platform.runLater(() -> {
            btnSubmit.setDisable(false);
            btnSubmit.setText("GỬI ĐÁNH GIÁ");
            AlertUtil.showError("Lỗi: " + err);
        }));
    }
}