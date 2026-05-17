package vn.edu.vnu.uet.group8.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductCardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ProductCardController.class.getName());

    @FXML private VBox root;
    @FXML private StackPane imageContainer;
    @FXML private ImageView productImage;
    @FXML private Label productName;
    @FXML private Label currentPrice;
    @FXML private Label lblTimer;
    @FXML private Label lblBidCount;

    @FXML private Label lblCertBadge;
    @FXML private Label lblPartnerBadge;

    private int itemId;
    private Timeline timerTimeline;
    private LocalDateTime endTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideBadge(lblCertBadge);
        hideBadge(lblPartnerBadge);
        setupClickHandler();
    }

    private void setupClickHandler() {
        if (root != null) {
            root.setOnMouseClicked(e -> onCardClick());
        }
    }

    public void setItem(String id, String name, BigDecimal price, String imageUrl) {
        setItem(id, name, price, imageUrl, LocalDateTime.now().plusHours(2)); // Default 2h if no endtime
    }

    /**
     * Set data and start countdown.
     */
    public void setItem(String id, String name, BigDecimal price, String imageUrl, LocalDateTime endTime) {
        try {
            this.itemId = (id != null) ? Integer.parseInt(id) : 0;
        } catch (NumberFormatException e) {
            this.itemId = 0;
        }

        if (productName != null) productName.setText(name != null ? name : "Unknown");
        if (currentPrice != null) currentPrice.setText(formatPrice(price));
        
        this.endTime = endTime;
        startCountdown();
        loadImage(imageUrl);
    }

    private void startCountdown() {
        if (timerTimeline != null) timerTimeline.stop();
        if (endTime == null) return;

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimerLabel()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
        updateTimerLabel();
    }

    private void updateTimerLabel() {
        if (lblTimer == null || endTime == null) return;

        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
        if (seconds <= 0) {
            lblTimer.setText("Kết thúc");
            lblTimer.setStyle("-fx-text-fill: #ef4444;");
            timerTimeline.stop();
            return;
        }

        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    private void loadImage(String imageUrl) {
        if (productImage == null) return;
        if (imageUrl == null || imageUrl.isBlank()) {
             // Optional: Set default image
             return;
        }

        try {
            Image img = new Image(imageUrl, true);
            productImage.setImage(img);
            
            // Fix: Tránh méo ảnh bằng cách căn giữa trong container
            productImage.setPreserveRatio(true);
            
            img.errorProperty().addListener((obs, oldVal, hasError) -> {
                if (hasError) LOGGER.fine("Error loading image: " + imageUrl);
            });
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Image exception: " + imageUrl, e);
        }
    }

    public void setBidCount(int count) {
        if (lblBidCount != null) {
            lblBidCount.setText(count + " bids");
        }
    }

    @FXML
    private void onCardClick() {
        if (itemId <= 0) return;
        AuctionService.loadDetail(itemId, item -> {
            if (item == null) return;
            ClientModel.getInstance().setCurrentAuctionItem(item);
            navigateToDetail();
        });
    }

    private void navigateToDetail() {
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadView(SceneManager.VIEW_AUCTION_DETAIL);
        }
    }

    private void hideBadge(Label badge) {
        if (badge != null) {
            badge.setVisible(false);
            badge.setManaged(false);
        }
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0đ";
        return String.format("%,.0fđ", price);
    }
}
