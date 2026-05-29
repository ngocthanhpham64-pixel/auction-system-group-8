package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;

/**
 * Controller quản lý hiển thị và tương tác cho từng thẻ sản phẩm đơn lẻ.
 */
public class ProductCardController {
    @FXML private VBox root;
    @FXML private ImageView productImage;
    @FXML private Label productName;
    @FXML private Label currentPrice;
    @FXML private Label lblCategory;
    @FXML private Label lblCondition;
    @FXML private Label lblBidCount;
    @FXML private Label lblTimer;
    @FXML private Label lblCertBadge;
    @FXML private javafx.scene.control.Button btnAction;

    private AuctionItemDTO item;
    private javafx.animation.AnimationTimer countdownTimer;
    private java.util.function.Consumer<vn.edu.vnu.uet.group8.common.dto.response.ServerResponse> priceUpdateSub;
    private boolean navigating = false;

    /**
     * Đổ dữ liệu từ DTO vào UI của thẻ.
     */
    public void setData(AuctionItemDTO item) {
        this.item = item;
        productName.setText(item.getTitle());
        currentPrice.setText(UIFormatter.formatPrice(item.getCurrentPrice()));
        lblCategory.setText(item.getCategory() != null ? item.getCategory().name() : "Other");
        lblCondition.setText(item.getCondition() != null ? item.getCondition().name() : "USED");
        lblBidCount.setText(item.getBidCount() + " bids");
        
        // Hiển thị ảnh bìa sản phẩm
        if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            try {
                productImage.setImage(new Image(item.getImageUrls().get(0), true));
            } catch (Exception e) {
                // Image fallback được xử lý tự động bởi label placeholder trong FXML
            }
        }
        
        // Hiển thị trạng thái theo đúng Enum
        if (item.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.UPCOMING) {
            if (countdownTimer != null) countdownTimer.stop();
            lblTimer.setText("Sắp diễn ra");
            if (btnAction != null) btnAction.setText("Xem chi tiết →");
            if (lblCertBadge != null) {
                lblCertBadge.setText("SẮP DIỄN RA");
                lblCertBadge.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 3 9;");
                lblCertBadge.setVisible(true);
                lblCertBadge.setManaged(true);
            }
        } else if (item.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.SOLD || item.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.ENDED_NO_BID || item.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.CANCELLED) {
            if (countdownTimer != null) countdownTimer.stop();
            lblTimer.setText("Đã kết thúc");
            if (btnAction != null) btnAction.setText("Xem kết quả →");
            if (lblCertBadge != null) {
                lblCertBadge.setText("ĐÃ KẾT THÚC");
                lblCertBadge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 3 9;");
                lblCertBadge.setVisible(true);
                lblCertBadge.setManaged(true);
            }
        } else if (item.getEndTime() != null) {
            if (btnAction != null) btnAction.setText("Tham gia đấu giá →");
            if (lblCertBadge != null) {
                lblCertBadge.setText("🔥 LIVE");
                lblCertBadge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 3 9;");
                lblCertBadge.setVisible(true);
                lblCertBadge.setManaged(true);
            }
            if (countdownTimer != null) countdownTimer.stop();
            countdownTimer = new javafx.animation.AnimationTimer() {
                @Override
                public void handle(long now) {
                    long remainingMillis = item.getEndTime().toEpochMilli() - System.currentTimeMillis();
                    if (remainingMillis <= 0) {
                        lblTimer.setText("00:00:00");
                        this.stop();
                    } else {
                        long totalSeconds = remainingMillis / 1000;
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        long seconds = totalSeconds % 60;
                        lblTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    }
                }
            };
            countdownTimer.start();
        } else {
            lblTimer.setText("--:--:--");
        }
        
        setupRealtimePrice();
    }

    private void setupRealtimePrice() {
        if (priceUpdateSub != null) {
            AuctionService.unsubscribeAuctionStatus(priceUpdateSub);
        }
        priceUpdateSub = AuctionService.subscribeAuctionStatus(status -> {
            javafx.application.Platform.runLater(() -> {
                if (this.item != null && this.item.getItemId() == status.getItemId()) {
                    currentPrice.setText(UIFormatter.formatPrice(status.getCurrentPrice()));
                }
            });
        });
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                if (priceUpdateSub != null) {
                    AuctionService.unsubscribeAuctionStatus(priceUpdateSub);
                }
                if (countdownTimer != null) {
                    countdownTimer.stop();
                }
            }
        });
    }

    /**
     * Khi click vào thẻ -> Load chi tiết và chuyển sang AuctionDetailView.
     */
    @FXML
    private void onCardClick() {
        if (navigating) return;
        if (item != null && (item.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.ENDED_NO_BID || 
            item.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.SOLD ||
            item.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.CANCELLED)) {
            vn.edu.vnu.uet.group8.client.util.AlertUtil.showError("Phiên đấu giá đã kết thúc. Bạn không thể xem chi tiết.");
            return;
        }
        
        if (item != null && item.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.UPCOMING) {
            vn.edu.vnu.uet.group8.client.util.AlertUtil.showInfo("Sản phẩm đang chuẩn bị đấu giá.");
            return;
        }
        
        navigating = true;
        AuctionService.loadDetail(item.getItemId(), detail -> {
            javafx.application.Platform.runLater(() -> {
                if (MainController.getInstance() != null) {
                    MainController.getInstance().loadContentView("AuctionDetailView.fxml");
                } else {
                    vn.edu.vnu.uet.group8.client.util.SceneManager.switchTo("AuctionDetailView.fxml");
                }
                navigating = false;
            });
        });
    }
}