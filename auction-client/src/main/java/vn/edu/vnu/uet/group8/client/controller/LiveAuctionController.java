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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.service.BidService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LiveAuctionController — phong dau gia truc tiep (#5 Live).
 *
 * Tinh nang:
 *  - Load item tu ClientModel.currentAuctionItem
 *  - Subscribe AuctionStatus realtime -> tu update gia khi co bid moi
 *  - Countdown thuc tu Item.endTime
 *  - Bid voi validate + confirm + goi BidService that
 *  - Quick bid: +10tr / +20tr / +50tr / +100tr
 *  - Chat local (cho BE bo sung CHAT_SEND API)
 *  - View count gia lap (cho BE bo sung)
 *  - Cleanup khi roi view -> unsubscribe + stop Timeline (tranh memory leak)
 */
public class LiveAuctionController implements Initializable {

    protected static final Logger LOGGER = Logger.getLogger(LiveAuctionController.class.getName());

    protected static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML Label lblProductName;
    @FXML Label lblCategory;
    @FXML Label lblDescription;
    @FXML Label lblCurrentPrice;
    @FXML Label lblPriceChange;
    @FXML Label lblViewers;
    @FXML Label lblChatCount;

    @FXML Label lblMinutes;
    @FXML Label lblSeconds;

    @FXML ImageView imgLive;
    @FXML VBox bidHistory;
    @FXML VBox chatMessages;
    @FXML TextField tfBidAmount;
    @FXML TextField tfChatInput;

    // ===== STATE =====
    protected AuctionItemDTO currentItem;
    protected BigDecimal currentPrice = BigDecimal.ZERO;
    protected final BigDecimal bidStep = new BigDecimal("10000000");
    protected Timeline countdown;
    protected int remainSeconds = 0;
    protected Consumer<ServerResponse> subscription;
    protected int viewerCount = 100;  // demo
    protected int chatCount = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadItem();
        startCountdown();
        subscribeRealtime();
        initViewerCount();
    }

    void loadItem() {
        currentItem = ClientModel.getInstance().getCurrentAuctionItem();
        if (currentItem == null) {
            LOGGER.info("Khong co currentAuctionItem - dung data demo tu FXML");
            return;
        }

        if (lblProductName != null && currentItem.getTitle() != null) {
            lblProductName.setText(currentItem.getTitle());
        }
        if (lblCategory != null && currentItem.getCategory() != null) {
            lblCategory.setText(currentItem.getCategory().name());
        }
        if (lblDescription != null && currentItem.getDescription() != null) {
            lblDescription.setText(currentItem.getDescription());
        }

        currentPrice = currentItem.getCurrentPrice() != null
                ? currentItem.getCurrentPrice()
                : BigDecimal.ZERO;
        updatePriceDisplay(null);
    }

    void initViewerCount() {
        if (lblViewers != null) {
            lblViewers.setText(viewerCount + " nguoi xem");
        }
        if (lblChatCount != null) {
            lblChatCount.setText(chatCount + " tin nhan");
        }
    }

    // ===== REALTIME SUBSCRIBE =====

    /**
     * Subscribe AuctionStatus broadcast. An toan: try-catch tranh crash neu BE chua co.
     */
    void subscribeRealtime() {
        try {
            subscription = AuctionService.subscribeAuctionStatus(this::handleStatusUpdate);
            LOGGER.info("Da subscribe realtime auction status");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Khong the subscribe realtime (BE co the chua broadcast)", e);
        }
    }

    /**
     * Handler khi server push status update.
     * Chay tren FX Thread (AuctionService da dam bao).
     */
    void handleStatusUpdate(AuctionStatusDTO status) {
        if (status == null) return;

        // FIX: currentItem null cũng phải skip
        if (currentItem == null || status.getItemId() != currentItem.getItemId()) {
            return;
        }

        BigDecimal oldPrice = currentPrice;
        currentPrice = status.getCurrentPrice() != null
                ? status.getCurrentPrice()
                : currentPrice;

        updatePriceDisplay(oldPrice);
        addBidHistoryEntry(status);
    }

    void addBidHistoryEntry(AuctionStatusDTO status) {
        if (bidHistory == null) return;

        HBox row = new HBox(8);
        row.getStyleClass().add("bid-entry");
        row.setStyle("-fx-padding: 8; -fx-background-radius: 6;");

        Label time = new Label(LocalTime.now().format(TIME_FORMAT));
        time.getStyleClass().add("label-info");

        Label price = new Label(formatPrice(status.getCurrentPrice()));
        price.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");

        row.getChildren().addAll(time, price);
        bidHistory.getChildren().add(0, row);

        // Gioi han 20 entry moi nhat
        if (bidHistory.getChildren().size() > 20) {
            bidHistory.getChildren().remove(20, bidHistory.getChildren().size());
        }
    }

    // ===== ĐẶT GIÁ =====

    @FXML
    void onBidNow() {
        BigDecimal input = parseBidInput();

        // Neu khong nhap gi -> default = current + step
        if (input == null) {
            BigDecimal defaultBid = currentPrice.add(bidStep);
            if (tfBidAmount != null) tfBidAmount.setText(formatPrice(defaultBid));
            return;
        }

        final BigDecimal amount = input;  // final - dung an toan trong lambda

        BigDecimal minBid = currentPrice.add(bidStep);
        if (amount.compareTo(minBid) < 0) {
            AlertUtil.showWarning("Gia phai >= " + formatPrice(minBid));
            return;
        }

        if (currentItem == null) {
            // Demo mode
            currentPrice = amount;
            updatePriceDisplay(null);
            if (tfBidAmount != null) tfBidAmount.clear();
            LOGGER.info(() -> "Demo bid: " + amount);
            return;
        }

        // Real bid
        BidService.placeBid(currentItem.getItemId(), amount, response -> {
            if (response != null && response.isSuccess()) {
                if (tfBidAmount != null) tfBidAmount.clear();
                LOGGER.info(() -> "Bid thanh cong: " + amount);
            } else {
                String msg = response != null && response.getMessage() != null
                        ? response.getMessage() : "Dat gia that bai";
                AlertUtil.showError(msg);
            }
        });
    }

    @FXML void onQuickBid1() { setBidInput(currentPrice.add(new BigDecimal("10000000"))); }
    @FXML void onQuickBid2() { setBidInput(currentPrice.add(new BigDecimal("20000000"))); }
    @FXML void onQuickBid3() { setBidInput(currentPrice.add(new BigDecimal("50000000"))); }
    @FXML void onQuickBid4() { setBidInput(currentPrice.add(new BigDecimal("100000000"))); }

    void setBidInput(BigDecimal amount) {
        if (tfBidAmount != null) tfBidAmount.setText(formatPrice(amount));
    }

    protected BigDecimal parseBidInput() {
        if (tfBidAmount == null) return null;
        String text = tfBidAmount.getText();
        if (text == null || text.isBlank()) return null;
        try {
            String cleaned = text.replaceAll("[^\\d]", "");
            if (cleaned.isEmpty()) return null;
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("So tien khong hop le");
            return null;
        }
    }

    void updatePriceDisplay(BigDecimal oldPrice) {
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(formatPrice(currentPrice));
        }

        // Hien % thay doi
        if (lblPriceChange != null && oldPrice != null
                && oldPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = currentPrice.subtract(oldPrice);
            double percent = diff.doubleValue() / oldPrice.doubleValue() * 100;
            lblPriceChange.setText(String.format("+%.1f%%", percent));
            lblPriceChange.setStyle("-fx-text-fill: #22C55E;");
        }
    }

    // ===== CHAT =====

    @FXML
    void onSendMessage() {
        if (tfChatInput == null) return;
        String msg = tfChatInput.getText().trim();
        if (msg.isEmpty()) return;

        addChatMessage(SessionManager.getUsername(), msg, true);
        tfChatInput.clear();
        chatCount++;
        if (lblChatCount != null) {
            lblChatCount.setText(chatCount + " tin nhan");
        }

        // TODO: goi ChatService.send() khi BE bo sung
        LOGGER.info(() -> "Chat: " + msg);
    }

    void addChatMessage(String username, String message, boolean isMe) {
        if (chatMessages == null) return;
        HBox row = new HBox(8);
        row.setStyle("-fx-padding: 6;");

        Label nameLabel = new Label(username != null ? username : "Guest");
        nameLabel.setStyle(isMe
                ? "-fx-text-fill: #F97316; -fx-font-weight: bold;"
                : "-fx-text-fill: #666; -fx-font-weight: bold;");

        Label msgLabel = new Label(": " + message);
        msgLabel.setWrapText(true);

        row.getChildren().addAll(nameLabel, msgLabel);
        chatMessages.getChildren().add(row);

        // Gioi han 50 tin nhan
        if (chatMessages.getChildren().size() > 50) {
            chatMessages.getChildren().remove(0);
        }
    }

    // ===== COUNTDOWN =====

    void startCountdown() {
        if (currentItem != null && currentItem.getEndTime() != null) {
            long seconds = currentItem.getEndTime().getEpochSecond() - Instant.now().getEpochSecond();
            remainSeconds = seconds > 0 ? (int) seconds : 0;
        } else {
            remainSeconds = 8 * 60 + 25;  // demo 8m25s
        }

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    void tick() {
        if (remainSeconds <= 0) {
            countdown.stop();
            setTimeDisplay(0, 0);
            onAuctionEnded();
            return;
        }
        remainSeconds--;
        int m = remainSeconds / 60;
        int s = remainSeconds % 60;
        setTimeDisplay(m, s);

        // Tang/giam viewer ngau nhien (demo realistic)
        if (remainSeconds % 5 == 0) {
            viewerCount += (int) (Math.random() * 5 - 2);  // -2 toi +2
            if (viewerCount < 1) viewerCount = 1;
            if (lblViewers != null) lblViewers.setText(viewerCount + " nguoi xem");
        }
    }

    void setTimeDisplay(int m, int s) {
        if (lblMinutes != null) lblMinutes.setText(String.format("%02d", m));
        if (lblSeconds != null) lblSeconds.setText(String.format("%02d", s));
    }

    void onAuctionEnded() {
        LOGGER.info("Live auction ket thuc");
        if (tfBidAmount != null) tfBidAmount.setDisable(true);
        if (tfChatInput != null) tfChatInput.setDisable(true);
        AlertUtil.showInfo("Phien dau gia da ket thuc!");
    }

    // ===== HELPER =====

    protected String formatPrice(BigDecimal price) {
        return price == null ? "--" : String.format("%,.0f d", price);
    }

    /**
     * Cleanup khi roi view - DAY LA QUAN TRONG.
     * - Unsubscribe khoi broadcast (tranh callback chay sau khi view die)
     * - Stop Timeline (tranh background CPU)
     */
    public void cleanup() {
        if (subscription != null) {
            try {
                AuctionService.unsubscribeAuctionStatus(subscription);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Loi khi unsubscribe", e);
            }
            subscription = null;
        }
        if (countdown != null) {
            countdown.stop();
            countdown = null;
        }
        LOGGER.fine("LiveAuction cleanup completed");
    }
}