package vn.edu.vnu.uet.group8.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class AuctionDetailController implements Initializable {

    // ===== FXML BINDINGS =====
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
    @FXML private Label lblCertificate;
    @FXML private Label lblSellerName;
    @FXML private Label lblSellerAvatar;
    @FXML private Label lblSellerStats;
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;

    @FXML private TextField tfBidAmount;
    @FXML private TextField tfMaxPrice;

    @FXML private VBox bidHistory;
    @FXML private VBox paneDesc;
    @FXML private VBox paneSpec;
    @FXML private VBox paneOrigin;
    @FXML private VBox paneAuto;

    @FXML private Button btnTabDesc;
    @FXML private Button btnTabSpec;
    @FXML private Button btnTabOrigin;
    @FXML private Button btnAutoToggle;

    // ===== STATE =====
    private BigDecimal currentPrice = new BigDecimal("850000000");
    private final BigDecimal bidStep  = new BigDecimal("10000000");
    private Timeline countdown;
    private int remainSeconds = 2 * 3600 + 14 * 60 + 30; // 2h14m30s

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updatePriceDisplay();
        startCountdown();
        showTab("desc");
    }

    // ===== QUAY LẠI =====

    @FXML
    private void onBack() {
        // TODO: yêu cầu MainController navigate về ExploreView
        System.out.println("[AuctionDetail] Quay lại danh sách");
    }

    // ===== TABS MÔ TẢ =====

    @FXML private void onTabDesc()   { showTab("desc"); }
    @FXML private void onTabSpec()   { showTab("spec"); }
    @FXML private void onTabOrigin() { showTab("origin"); }

    private void showTab(String tab) {
        // Ẩn tất cả
        paneDesc.setVisible(false);   paneDesc.setManaged(false);
        paneSpec.setVisible(false);   paneSpec.setManaged(false);
        paneOrigin.setVisible(false); paneOrigin.setManaged(false);

        // Style reset
        String inactive = "-fx-background-color: #F5F5F5; -fx-text-fill: #666; -fx-font-size: 12px; -fx-padding: 10; -fx-cursor: hand;";
        String activeDesc   = "-fx-background-color: #F97316; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12 0 0 0; -fx-padding: 10; -fx-cursor: hand;";
        String activeSpec   = "-fx-background-color: #F97316; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;";
        String activeOrigin = "-fx-background-color: #F97316; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 0 12 0 0; -fx-padding: 10; -fx-cursor: hand;";

        btnTabDesc.setStyle(inactive);
        btnTabSpec.setStyle(inactive);
        btnTabOrigin.setStyle(inactive);

        switch (tab) {
            case "desc" -> {
                paneDesc.setVisible(true); paneDesc.setManaged(true);
                btnTabDesc.setStyle(activeDesc);
            }
            case "spec" -> {
                paneSpec.setVisible(true); paneSpec.setManaged(true);
                btnTabSpec.setStyle(activeSpec);
            }
            case "origin" -> {
                paneOrigin.setVisible(true); paneOrigin.setManaged(true);
                btnTabOrigin.setStyle(activeOrigin);
            }
        }
    }

    // ===== ĐẤU GIÁ =====

    @FXML
    private void onBidNow() {
        String input = tfBidAmount.getText().replaceAll("[^\\d]", "");
        if (input.isBlank()) {
            showQuickBid(currentPrice.add(bidStep));
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(input);
            BigDecimal minBid = currentPrice.add(bidStep);
            if (amount.compareTo(minBid) < 0) {
                tfBidAmount.setStyle(tfBidAmount.getStyle() + "; -fx-border-color: #E03030;");
                return;
            }
            placeBid(amount);
        } catch (NumberFormatException ignored) {
            tfBidAmount.setStyle(tfBidAmount.getStyle() + "; -fx-border-color: #E03030;");
        }
    }

    @FXML private void onQuickBid1() { showQuickBid(currentPrice.add(new BigDecimal("10000000"))); }
    @FXML private void onQuickBid2() { showQuickBid(currentPrice.add(new BigDecimal("20000000"))); }
    @FXML private void onQuickBid3() { showQuickBid(currentPrice.add(new BigDecimal("50000000"))); }

    private void showQuickBid(BigDecimal amount) {
        tfBidAmount.setText(String.format("%,.0f đ", amount));
        tfBidAmount.setStyle("");
    }

    private void placeBid(BigDecimal amount) {
        currentPrice = amount;
        updatePriceDisplay();
        tfBidAmount.clear();
        // TODO: gửi lên server
        System.out.println("[AuctionDetail] Đặt giá: " + amount);
    }

    // ===== ĐẶT GIÁ TỰ ĐỘNG =====

    @FXML
    private void onToggleAuto() {
        boolean show = !paneAuto.isVisible();
        paneAuto.setVisible(show);
        paneAuto.setManaged(show);
        btnAutoToggle.setText(show ? "Đặt giá tự động  ▲" : "Đặt giá tự động  ▼");
    }

    @FXML
    private void onActivateAuto() {
        String input = tfMaxPrice.getText().replaceAll("[^\\d]", "");
        if (input.isBlank()) return;
        // TODO: gửi lên server kích hoạt auto-bid
        System.out.println("[AuctionDetail] Kích hoạt auto-bid tối đa: " + input);
    }

    // ===== YÊU THÍCH / CHIA SẺ =====

    @FXML private void onFavorite() { System.out.println("[AuctionDetail] Thêm yêu thích"); }
    @FXML private void onShare()    { System.out.println("[AuctionDetail] Chia sẻ"); }

    // ===== THUMBNAIL =====

    @FXML private void onThumb1() { setMainThumb(0); }
    @FXML private void onThumb2() { setMainThumb(1); }
    @FXML private void onThumb3() { setMainThumb(2); }

    private void setMainThumb(int idx) {
        // TODO: đổi imgMain theo ảnh thumbnail idx
        System.out.println("[AuctionDetail] Chọn thumbnail " + idx);
    }

    // ===== ĐẾM NGƯỢC =====

    private void startCountdown() {
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
        BigDecimal minNext = currentPrice.add(bidStep);
        lblMinBid.setText("Mức tăng tối thiểu: " + String.format("%,.0f đ", bidStep));
        lblStep.setText("Bước nhảy: " + String.format("%,.0f đ", bidStep));
        tfBidAmount.setPromptText(String.format("%,.0f đ", minNext));
    }
}
