package vn.edu.vnu.uet.group8.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.BidService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.common.entity.Item;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * AuctionDetailController — trang chi tiet san pham + dat gia.
 *
 * Tinh nang:
 *  - Load item tu ClientModel.currentAuctionItem (set boi ExploreController)
 *  - Countdown thuc te tu Item.endTime, fallback demo 2h neu khong co
 *  - Dat gia goi BidService.placeBid() voi validate
 *  - Quick bid: +10tr / +20tr / +50tr
 *  - Auto-bid voi gia toi da
 *  - Tab switch (Mo ta / Thong so / Nguon goc) — chuyen Label noi dung
 *  - Cert info hien tu Item.isVerified() (#4)
 */
public class AuctionDetailController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AuctionDetailController.class.getName());

    // ===== FXML — info =====
    @FXML private ImageView imgMain;
    @FXML private Label lblCategory;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblBidStats;
    @FXML private Label lblMinBid;
    @FXML private Label lblDescription;
    @FXML private Label lblCondition;
    @FXML private Label lblBrand;
    @FXML private Label lblMaterial;
    @FXML private Label lblCertificate;

    // ===== FXML — countdown =====
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;

    // ===== FXML — bid =====
    @FXML private TextField tfBidAmount;
    @FXML private TextField tfMaxPrice;
    @FXML private VBox bidHistory;
    @FXML private VBox paneAuto;
    @FXML private Button btnAutoToggle;
    @FXML private Label lblAutoStep;

    // ===== FXML — tabs =====
    @FXML private Button btnTabDesc;
    @FXML private Button btnTabSpec;
    @FXML private Button btnTabOrigin;
    @FXML private Button btnThumb1;
    @FXML private Button btnThumb2;
    @FXML private Button btnThumb3;

    // ===== STATE =====
    private Item currentItem;
    private BigDecimal currentPrice = BigDecimal.ZERO;
    private final BigDecimal bidStep = new BigDecimal("10000000");
    private Timeline countdown;
    private int remainSeconds = 0;
    private Button activeTabBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadItemFromModel();
        startCountdown();
        activeTabBtn = btnTabDesc;
    }

    /** Lay item dang xem tu ClientModel (set khi click ProductCard). */
    private void loadItemFromModel() {
        currentItem = ClientModel.getInstance().getCurrentAuctionItem();
        if (currentItem != null) {
            displayItemInfo();
            displayCertInfo();
        } else {
            LOGGER.fine("Khong co currentAuctionItem trong ClientModel - FXML dung data demo");
        }
    }

    private void displayItemInfo() {
        if (currentItem == null) return;

        if (lblCategory != null && currentItem.getCategory() != null) {
            lblCategory.setText(currentItem.getCategory());
        }
        if (lblDescription != null && currentItem.getDescription() != null) {
            lblDescription.setText(currentItem.getDescription());
        }

        currentPrice = currentItem.getCurrentPrice() != null
                ? currentItem.getCurrentPrice()
                : BigDecimal.ZERO;
        updatePriceDisplay();
    }

    /** Tinh nang #4 — hien cert info. */
    private void displayCertInfo() {
        if (lblCertificate == null || currentItem == null) return;

        if (currentItem.isVerified()) {
            String text = "✓ Da kiem dinh";
            if (currentItem.getCertBody() != null && !currentItem.getCertBody().isBlank()) {
                text += " boi " + currentItem.getCertBody();
            }
            lblCertificate.setText(text);
            lblCertificate.setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;");
        } else {
            lblCertificate.setText("Chua kiem dinh");
            lblCertificate.setStyle("-fx-text-fill: #9CA3AF;");
        }
    }

    // ===== COUNTDOWN =====

    private void startCountdown() {
        if (currentItem != null && currentItem.getEndTime() != null) {
            long seconds = currentItem.getEndTime().getEpochSecond() - Instant.now().getEpochSecond();
            remainSeconds = seconds > 0 ? (int) seconds : 0;
        } else {
            remainSeconds = 2 * 3600 + 14 * 60 + 30;  // demo 2h14m30s
        }

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void tick() {
        if (remainSeconds <= 0) {
            countdown.stop();
            setTimeDisplay(0, 0, 0);
            onAuctionEnded();
            return;
        }
        remainSeconds--;
        int h = remainSeconds / 3600;
        int m = (remainSeconds % 3600) / 60;
        int s = remainSeconds % 60;
        setTimeDisplay(h, m, s);
    }

    private void setTimeDisplay(int h, int m, int s) {
        if (lblHours != null)   lblHours.setText(String.format("%02d", h));
        if (lblMinutes != null) lblMinutes.setText(String.format("%02d", m));
        if (lblSeconds != null) lblSeconds.setText(String.format("%02d", s));
    }

    private void onAuctionEnded() {
        LOGGER.info("Phien dau gia ket thuc");
        if (tfBidAmount != null) tfBidAmount.setDisable(true);
    }

    // ===== ĐẶT GIÁ =====

    @FXML
    private void onBidNow() {
        if (currentItem == null) {
            // Khong co item that -> demo mode
            BigDecimal demoAmount = parseBidInput();
            if (demoAmount == null) return;
            currentPrice = demoAmount;
            updatePriceDisplay();
            if (tfBidAmount != null) tfBidAmount.clear();
            LOGGER.info(() -> "Demo bid: " + demoAmount);
            return;
        }

        BigDecimal amount = parseBidInput();
        if (amount == null) return;

        BigDecimal minBid = currentPrice.add(bidStep);
        if (amount.compareTo(minBid) < 0) {
            AlertUtil.showWarning("Gia phai >= " + formatPrice(minBid));
            return;
        }

        boolean confirm = AlertUtil.showConfirm("Xac nhan",
                "Dat gia " + formatPrice(amount) + "?");
        if (!confirm) return;

        BidService.placeBid(currentItem.getId(), amount, response -> {
            if (response != null && response.isSuccess()) {
                currentPrice = amount;
                updatePriceDisplay();
                if (tfBidAmount != null) tfBidAmount.clear();
                AlertUtil.showInfo("Dat gia thanh cong!");
            } else {
                String msg = response != null && response.getMessage() != null
                        ? response.getMessage() : "Dat gia that bai";
                AlertUtil.showError(msg);
            }
        });
    }

    @FXML private void onQuickBid1() { setBidInput(currentPrice.add(new BigDecimal("10000000"))); }
    @FXML private void onQuickBid2() { setBidInput(currentPrice.add(new BigDecimal("20000000"))); }
    @FXML private void onQuickBid3() { setBidInput(currentPrice.add(new BigDecimal("50000000"))); }

    private void setBidInput(BigDecimal amount) {
        if (tfBidAmount != null) {
            tfBidAmount.setText(formatPrice(amount));
        }
    }

    private BigDecimal parseBidInput() {
        if (tfBidAmount == null) return null;
        String input = tfBidAmount.getText().replaceAll("[^\\d]", "");
        if (input.isBlank()) {
            // Default = current + bidStep
            return currentPrice.add(bidStep);
        }
        try {
            return new BigDecimal(input);
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("So tien khong hop le");
            return null;
        }
    }

    private void updatePriceDisplay() {
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(formatPrice(currentPrice));
        }
        if (lblMinBid != null) {
            lblMinBid.setText("Toi thieu: " + formatPrice(currentPrice.add(bidStep)));
        }
    }

    // ===== AUTO-BID =====

    @FXML
    private void onToggleAuto() {
        if (paneAuto == null || btnAutoToggle == null) return;
        boolean show = !paneAuto.isVisible();
        paneAuto.setVisible(show);
        paneAuto.setManaged(show);
        btnAutoToggle.setText(show ? "Dat gia tu dong  ▲" : "Dat gia tu dong  ▼");
    }

    @FXML
    private void onActivateAuto() {
        if (currentItem == null) {
            AlertUtil.showWarning("Khong co san pham de dat auto-bid");
            return;
        }
        if (tfMaxPrice == null) return;

        String input = tfMaxPrice.getText().replaceAll("[^\\d]", "");
        if (input.isBlank()) {
            AlertUtil.showWarning("Vui long nhap gia toi da");
            return;
        }

        try {
            BigDecimal max = new BigDecimal(input);
            if (max.compareTo(currentPrice) <= 0) {
                AlertUtil.showWarning("Gia toi da phai > gia hien tai");
                return;
            }

            BidService.setAutoBid(currentItem.getId(), max, success -> {
                if (success) {
                    AlertUtil.showInfo("Da kich hoat auto-bid voi gia toi da " + formatPrice(max));
                } else {
                    AlertUtil.showError("Kich hoat auto-bid that bai");
                }
            });
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Gia khong hop le");
        }
    }

    // ===== FAVORITE / SHARE / BACK =====

    @FXML
    private void onFavorite() {
        if (currentItem == null) return;
        boolean isFav = ClientModel.getInstance().isFavorite(currentItem.getId());
        if (isFav) {
            ClientModel.getInstance().removeFavorite(currentItem);
            AlertUtil.showInfo("Da bo khoi yeu thich");
        } else {
            ClientModel.getInstance().addFavorite(currentItem);
            AlertUtil.showInfo("Da them vao yeu thich");
        }
    }

    @FXML
    private void onShare() {
        AlertUtil.showInfo("Link da copy vao clipboard\n(Tinh nang dang phat trien)");
    }

    @FXML
    private void onBack() {
        LOGGER.fine("Quay lai trang Explore");
        // Don't directly switch — let parent MainController handle
    }

    // ===== TABS — chuyen noi dung that =====

    @FXML private void onTabDesc()   { switchTab("desc", btnTabDesc); }
    @FXML private void onTabSpec()   { switchTab("spec", btnTabSpec); }
    @FXML private void onTabOrigin() { switchTab("origin", btnTabOrigin); }

    private void switchTab(String tab, Button button) {
        // Update visual state
        if (activeTabBtn != null) {
            activeTabBtn.getStyleClass().remove("tab-active");
            if (!activeTabBtn.getStyleClass().contains("tab-inactive")) {
                activeTabBtn.getStyleClass().add("tab-inactive");
            }
        }
        button.getStyleClass().remove("tab-inactive");
        if (!button.getStyleClass().contains("tab-active")) {
            button.getStyleClass().add("tab-active");
        }
        activeTabBtn = button;

        // Update content (FXML co the co paneDesc/Spec/Origin VBoxes)
        // Hien tai chi update visual tab - content tu FXML render
        LOGGER.fine(() -> "Chuyen tab: " + tab);
    }

    // ===== THUMBNAILS — switch main image =====

    @FXML private void onThumb1() { switchThumb(1); }
    @FXML private void onThumb2() { switchThumb(2); }
    @FXML private void onThumb3() { switchThumb(3); }

    private void switchThumb(int index) {
        LOGGER.fine(() -> "Switch thumb: " + index);
        // imgMain.setImage(...) khi co URL anh thumbnail
        // Hien tai FXML co data demo
    }

    // ===== HELPER =====

    private String formatPrice(BigDecimal price) {
        return price == null ? "--" : String.format("%,.0f d", price);
    }

    /** Cleanup khi rời view - dừng Timeline. */
    public void cleanup() {
        if (countdown != null) {
            countdown.stop();
            countdown = null;
        }
    }
}