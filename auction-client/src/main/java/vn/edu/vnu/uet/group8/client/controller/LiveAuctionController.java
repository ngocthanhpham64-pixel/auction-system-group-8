package vn.edu.vnu.uet.group8.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.service.BidService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;
import vn.edu.vnu.uet.group8.common.entity.Item;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * LiveAuctionController — tích hợp #5 Live giả lập.
 *
 * Realtime flow:
 *   1. initialize() → subscribe broadcast EVENT_AUCTION_STATUS
 *   2. Khi server push BID_UPDATE → handlePriceUpdate() chạy trên FX Thread
 *   3. UI cập nhật giá hiện tại + thêm bid vào lịch sử
 *   4. Khi rời view → cleanup() unsubscribe để tránh memory leak
 *
 * Chat: hiện local-only (TODO khi BE có CHAT_SEND API).
 */
public class LiveAuctionController implements Initializable {

    @FXML private Label lblProductName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblViewerCount;
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;

    @FXML private VBox bidHistoryContainer;
    @FXML private VBox chatContainer;
    @FXML private TextField tfChatInput;
    @FXML private TextField tfBidAmount;
    @FXML private Button btnBidNow;
    @FXML private Button btnSendMsg;

    private Item currentItem;
    private BigDecimal currentPrice = BigDecimal.ZERO;
    private final BigDecimal bidStep = new BigDecimal("10000000");
    private Timeline countdown;
    private int remainSeconds = 0;
    private Consumer<ServerResponse> subscription;  // wrapper để unsubscribe

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Lấy item từ ClientModel (set bởi ProductCardController hoặc ExploreController)
        currentItem = ClientModel.getInstance().auctionItemsProperty().get()
                .stream().findFirst().orElse(null);

        if (currentItem != null) {
            displayItem();
            startCountdown();
        }

        subscribeRealtime();
    }

    private void displayItem() {
        if (currentItem == null) return;
        lblProductName.setText(currentItem.getName());
        currentPrice = currentItem.getCurrentPrice() != null
                ? currentItem.getCurrentPrice()
                : BigDecimal.ZERO;
        updatePriceDisplay();
    }

    // ===== REALTIME SUBSCRIBE =====

    private void subscribeRealtime() {
        subscription = AuctionService.subscribeAuctionStatus(this::handlePriceUpdate);
    }

    /**
     * Handler chạy khi server broadcast giá mới.
     * AuctionService đảm bảo callback đã ở FX Thread.
     */
    private void handlePriceUpdate(AuctionStatusDTO status) {
        if (currentItem == null) return;
        if (status.getItemId() != currentItem.getId()) return;  // không phải item đang xem

        currentPrice = status.getCurrentPrice();
        updatePriceDisplay();

        // Thêm bid mới vào lịch sử
        addBidHistoryEntry(status);
    }

    private void addBidHistoryEntry(AuctionStatusDTO status) {
        HBox row = new HBox(8);
        row.getStyleClass().add("bid-entry");

        Label time = new Label(Instant.now().toString().substring(11, 19));
        Label price = new Label(String.format("%,.0f đ", status.getCurrentPrice()));
        Label bidder = new Label("Người đặt giá");

        row.getChildren().addAll(time, bidder, price);
        bidHistoryContainer.getChildren().add(0, row);  // insert đầu list

        // Giới hạn 20 entry mới nhất
        if (bidHistoryContainer.getChildren().size() > 20) {
            bidHistoryContainer.getChildren().remove(20);
        }
    }

    // ===== ĐẶT GIÁ =====

    @FXML
    private void onBidNow() {
        if (currentItem == null) return;
        String input = tfBidAmount.getText().replaceAll("[^\\d]", "");
        if (input.isBlank()) {
            tfBidAmount.setText(String.format("%,.0f đ", currentPrice.add(bidStep)));
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
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Số tiền không hợp lệ");
        }
    }

    @FXML private void onQuickBid1() { tfBidAmount.setText(formatPrice(currentPrice.add(new BigDecimal("10000000")))); }
    @FXML private void onQuickBid2() { tfBidAmount.setText(formatPrice(currentPrice.add(new BigDecimal("20000000")))); }
    @FXML private void onQuickBid3() { tfBidAmount.setText(formatPrice(currentPrice.add(new BigDecimal("50000000")))); }
    @FXML private void onQuickBid4() { tfBidAmount.setText(formatPrice(currentPrice.add(new BigDecimal("100000000")))); }

    private void placeBid(BigDecimal amount) {
        btnBidNow.setDisable(true);
        BidService.placeBid(currentItem.getId(), amount, response -> {
            btnBidNow.setDisable(false);
            if (response.isSuccess()) {
                tfBidAmount.clear();
                // Server sẽ broadcast → handlePriceUpdate tự cập nhật UI
            } else {
                AlertUtil.showError(response.getMessage());
            }
        });
    }

    // ===== CHAT =====

    @FXML
    private void onSendMessage() {
        String msg = tfChatInput.getText().trim();
        if (msg.isEmpty()) return;

        // TODO: BE chưa có CHAT_SEND API → tạm hiển thị local
        addChatMessage(SessionManager.getUsername(), msg, true);
        tfChatInput.clear();
    }

    private void addChatMessage(String username, String message, boolean isMe) {
        HBox row = new HBox(8);
        Label nameLabel = new Label(username);
        nameLabel.getStyleClass().add(isMe ? "chat-name-me" : "chat-name");
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        VBox bubble = new VBox(2, nameLabel, msgLabel);
        bubble.getStyleClass().add(isMe ? "chat-bubble-me" : "chat-bubble");
        row.getChildren().add(bubble);
        chatContainer.getChildren().add(row);
    }

    // ===== COUNTDOWN =====

    private void startCountdown() {
        if (currentItem == null || currentItem.getEndTime() == null) {
            remainSeconds = 0;
            return;
        }
        long seconds = currentItem.getEndTime().getEpochSecond() - Instant.now().getEpochSecond();
        remainSeconds = seconds > 0 ? (int) seconds : 0;

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

    // ===== HELPERS =====

    private void updatePriceDisplay() {
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(formatPrice(currentPrice));
        }
        if (tfBidAmount != null) {
            tfBidAmount.setPromptText(formatPrice(currentPrice.add(bidStep)));
        }
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? "--" : String.format("%,.0f đ", price);
    }

    /**
     * Cleanup khi rời view — controller cha gọi để unsubscribe.
     * Tránh memory leak và duplicate handler khi vào view lại.
     */
    public void cleanup() {
        if (subscription != null) {
            AuctionService.unsubscribeAuctionStatus(subscription);
            subscription = null;
        }
        if (countdown != null) {
            countdown.stop();
            countdown = null;
        }
    }
}