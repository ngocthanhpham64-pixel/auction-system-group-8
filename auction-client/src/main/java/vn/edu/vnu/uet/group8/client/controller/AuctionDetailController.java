package vn.edu.vnu.uet.group8.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.service.BidService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.common.entity.Item;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;

/**
 * AuctionDetailController — wire BidService thật + tính năng #4 Kiểm định.
 *
 * Tích hợp:
 *   - placeBid() gọi BidService.placeBid() qua JSON protocol
 *   - displayCertInfo() hiển thị badge "✓ Đã kiểm định" theo item.isVerified()
 *
 * Note: setItem() được gọi từ ProductCardController khi user click card.
 */
public class AuctionDetailController implements Initializable {

    // ===== FXML — Sản phẩm =====
    @FXML private ImageView imgMain;
    @FXML private Label lblCategory;
    @FXML private Label lblProductName;
    @FXML private Label lblBidStats;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStartPrice;
    @FXML private Label lblMinBid;
    @FXML private Label lblStep;
    @FXML private Label lblDescription;
    @FXML private Label lblCondition;
    @FXML private Label lblBrand;
    @FXML private Label lblModel;
    @FXML private Label lblYear;
    @FXML private Label lblMaterial;
    @FXML private Label lblOrigin;

    // ===== FXML — Tính năng #4 Kiểm định =====
    @FXML private Label lblCertBadge;
    @FXML private Label lblCertStatus;
    @FXML private Label lblCertBody;
    @FXML private VBox  paneCertDetail;

    // ===== FXML — Countdown =====
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;

    // ===== FXML — Bid =====
    @FXML private TextField tfBidAmount;
    @FXML private TextField tfMaxPrice;
    @FXML private Button btnBidNow;
    @FXML private VBox bidHistory;

    // ===== FXML — Tabs =====
    @FXML private VBox paneDesc;
    @FXML private VBox paneSpec;
    @FXML private VBox paneOrigin;
    @FXML private VBox paneAuto;
    @FXML private Button btnTabDesc;
    @FXML private Button btnTabSpec;
    @FXML private Button btnTabOrigin;
    @FXML private Button btnAutoToggle;

    // ===== STATE =====
    private Item currentItem;
    private BigDecimal currentPrice = BigDecimal.ZERO;
    private BigDecimal bidStep = new BigDecimal("10000000");
    private Timeline countdown;
    private int remainSeconds = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showTab("desc");
        currentItem = ClientModel.getInstance().auctionItemsProperty().get()
                .stream().findFirst().orElse(null);

        // Có id thì load chi tiết
        if (currentItem != null) {
            loadItemDetail(currentItem.getId());
        }
    }

    /**
     * Setter cho phép Controller cha truyền item ID vào (alternative).
     */
    public void setItemId(int itemId) {
        loadItemDetail(itemId);
    }

    // ===== LOAD DATA TỪ SERVER =====

    private void loadItemDetail(int itemId) {
        AuctionService.loadDetail(itemId, item -> {
            if (item == null) {
                AlertUtil.showError("Không tải được chi tiết sản phẩm");
                return;
            }
            this.currentItem = item;
            this.currentPrice = item.getCurrentPrice() != null
                    ? item.getCurrentPrice() : BigDecimal.ZERO;
            displayItemInfo();
            displayCertInfo();
            updatePriceDisplay();
            startCountdownFromEndTime(item.getEndTime());
        });
    }

    private void displayItemInfo() {
        if (currentItem == null) return;
        lblProductName.setText(currentItem.getName());
        if (currentItem.getCategory() != null) lblCategory.setText(currentItem.getCategory());
        if (currentItem.getDescription() != null) lblDescription.setText(currentItem.getDescription());
    }

    // ===== TÍNH NĂNG #4: KIỂM ĐỊNH =====

    private void displayCertInfo() {
        if (currentItem == null) return;

        boolean verified = currentItem.isVerified();

        // Badge nhỏ cạnh tên SP
        if (lblCertBadge != null) {
            lblCertBadge.setVisible(verified);
            lblCertBadge.setManaged(verified);
            if (verified) {
                lblCertBadge.setText("✓ Đã kiểm định");
                if (!lblCertBadge.getStyleClass().contains("badge-certified")) {
                    lblCertBadge.getStyleClass().add("badge-certified");
                }
            }
        }

        // Status text trong tab Nguồn gốc
        if (lblCertStatus != null) {
            lblCertStatus.setText(verified ? "✓ Đã kiểm định" : "Chưa kiểm định");
        }

        // Pane chi tiết — chỉ hiện khi verified
        if (paneCertDetail != null) {
            paneCertDetail.setVisible(verified);
            paneCertDetail.setManaged(verified);
        }

        if (verified && lblCertBody != null && currentItem.getCertBody() != null) {
            lblCertBody.setText(currentItem.getCertBody());
        }
    }

    // ===== TABS =====

    @FXML private void onTabDesc()   { showTab("desc"); }
    @FXML private void onTabSpec()   { showTab("spec"); }
    @FXML private void onTabOrigin() { showTab("origin"); }

    private void showTab(String tab) {
        paneDesc.setVisible(false);   paneDesc.setManaged(false);
        paneSpec.setVisible(false);   paneSpec.setManaged(false);
        paneOrigin.setVisible(false); paneOrigin.setManaged(false);

        String inactive = "-fx-background-color: #F5F5F5; -fx-text-fill: #666; -fx-padding: 10; -fx-cursor: hand;";
        String active = "-fx-background-color: #F97316; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;";

        btnTabDesc.setStyle(inactive);
        btnTabSpec.setStyle(inactive);
        btnTabOrigin.setStyle(inactive);

        switch (tab) {
            case "desc"   -> { paneDesc.setVisible(true);   paneDesc.setManaged(true);   btnTabDesc.setStyle(active); }
            case "spec"   -> { paneSpec.setVisible(true);   paneSpec.setManaged(true);   btnTabSpec.setStyle(active); }
            case "origin" -> { paneOrigin.setVisible(true); paneOrigin.setManaged(true); btnTabOrigin.setStyle(active); }
        }
    }

    // ===== ĐẶT GIÁ THẬT QUA BidService =====

    @FXML
    private void onBidNow() {
        if (currentItem == null) {
            AlertUtil.showError("Chưa có sản phẩm để đặt giá");
            return;
        }

        String input = tfBidAmount.getText().replaceAll("[^\\d]", "");
        if (input.isBlank()) {
            showQuickBid(currentPrice.add(bidStep));
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(input);
            BigDecimal minBid = currentPrice.add(bidStep);
            if (amount.compareTo(minBid) < 0) {
                AlertUtil.showWarning("Giá đặt phải >= " + String.format("%,.0f đ", minBid));
                return;
            }
            placeBid(amount);
        } catch (NumberFormatException ignored) {
            AlertUtil.showWarning("Số tiền không hợp lệ");
        }
    }

    @FXML private void onQuickBid1() { showQuickBid(currentPrice.add(new BigDecimal("10000000"))); }
    @FXML private void onQuickBid2() { showQuickBid(currentPrice.add(new BigDecimal("20000000"))); }
    @FXML private void onQuickBid3() { showQuickBid(currentPrice.add(new BigDecimal("50000000"))); }

    private void showQuickBid(BigDecimal amount) {
        tfBidAmount.setText(String.format("%,.0f đ", amount));
    }

    /**
     * Gọi BidService.placeBid() thật qua JSON protocol.
     * Callback trả về BidResponse — kiểm tra success rồi update UI.
     */
    private void placeBid(BigDecimal amount) {
        btnBidNow.setDisable(true);
        BidService.placeBid(currentItem.getId(), amount, response -> {
            btnBidNow.setDisable(false);
            if (response.isSuccess()) {
                currentPrice = amount;
                updatePriceDisplay();
                tfBidAmount.clear();
                AlertUtil.showInfo("Đặt giá thành công!");
            } else {
                AlertUtil.showError(response.getMessage() != null
                        ? response.getMessage()
                        : "Đặt giá thất bại");
            }
        });
    }

    // ===== AUTO-BID =====

    @FXML
    private void onToggleAuto() {
        boolean show = !paneAuto.isVisible();
        paneAuto.setVisible(show);
        paneAuto.setManaged(show);
        btnAutoToggle.setText(show ? "Đặt giá tự động  ▲" : "Đặt giá tự động  ▼");
    }

    @FXML
    private void onActivateAuto() {
        if (currentItem == null) return;
        String input = tfMaxPrice.getText().replaceAll("[^\\d]", "");
        if (input.isBlank()) return;
        try {
            BigDecimal max = new BigDecimal(input);
            BidService.setAutoBid(currentItem.getId(), max, success -> {
                if (success) {
                    AlertUtil.showInfo("Đã kích hoạt đặt giá tự động");
                } else {
                    AlertUtil.showError("Kích hoạt auto-bid thất bại");
                }
            });
        } catch (NumberFormatException ignored) {
            AlertUtil.showWarning("Số tiền không hợp lệ");
        }
    }

    // ===== YÊU THÍCH / CHIA SẺ =====

    @FXML private void onFavorite() {
        // TODO: gọi FavoriteService.add() — wire ở Batch 2
    }

    @FXML private void onShare() { /* TODO */ }
    @FXML private void onBack() { /* TODO: navigate về Explore */ }

    @FXML private void onThumb1() { /* TODO */ }
    @FXML private void onThumb2() { /* TODO */ }
    @FXML private void onThumb3() { /* TODO */ }

    // ===== COUNTDOWN =====

    private void startCountdownFromEndTime(Instant endTime) {
        if (endTime == null) {
            remainSeconds = 0;
        } else {
            long seconds = endTime.getEpochSecond() - Instant.now().getEpochSecond();
            remainSeconds = seconds > 0 ? (int) seconds : 0;
        }

        if (countdown != null) countdown.stop();
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (remainSeconds <= 0) {
                countdown.stop();
                lblHours.setText("00");
                lblMinutes.setText("00");
                lblSeconds.setText("00");
                return;
            }
            remainSeconds--;
            int h = remainSeconds / 3600;
            int m = (remainSeconds % 3600) / 60;
            int s = remainSeconds % 60;
            lblHours.setText(String.format("%02d", h));
            lblMinutes.setText(String.format("%02d", m));
            lblSeconds.setText(String.format("%02d", s));
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    // ===== HELPER =====

    private void updatePriceDisplay() {
        lblCurrentPrice.setText(String.format("%,.0f đ", currentPrice));
        lblMinBid.setText("Mức tăng tối thiểu: " + String.format("%,.0f đ", bidStep));
        lblStep.setText("Bước nhảy: " + String.format("%,.0f đ", bidStep));
        BigDecimal minNext = currentPrice.add(bidStep);
        tfBidAmount.setPromptText(String.format("%,.0f đ", minNext));
    }
}