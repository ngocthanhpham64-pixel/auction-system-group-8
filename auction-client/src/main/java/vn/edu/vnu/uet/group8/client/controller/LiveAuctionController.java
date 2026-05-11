package vn.edu.vnu.uet.group8.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class LiveAuctionController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private ImageView imgLive;
    @FXML private Label lblViewers;
    @FXML private Label lblCategory;
    @FXML private Label lblProductName;
    @FXML private Label lblDescription;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblPriceChange;
    @FXML private Label lblChatCount;
    @FXML private TextField tfBidAmount;
    @FXML private TextField tfChatInput;
    @FXML private VBox chatMessages;
    @FXML private VBox bidHistory;

    // ===== STATE =====
    private BigDecimal currentPrice = new BigDecimal("1860000000");
    private final BigDecimal bidStep = new BigDecimal("10000000");
    private Timeline countdown;
    private int remainSeconds = 8 * 60 + 25; // 8m25s
    private int chatCount = 6;

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updatePriceDisplay();
        startCountdown();
    }

    // ===== ĐẤU GIÁ NHANH =====

    @FXML private void onQuickBid1() { setQuickAmount(new BigDecimal("10000000")); }
    @FXML private void onQuickBid2() { setQuickAmount(new BigDecimal("20000000")); }
    @FXML private void onQuickBid3() { setQuickAmount(new BigDecimal("50000000")); }
    @FXML private void onQuickBid4() { setQuickAmount(new BigDecimal("100000000")); }

    private void setQuickAmount(BigDecimal add) {
        BigDecimal newBid = currentPrice.add(add);
        tfBidAmount.setText(String.format("%,.0f đ", newBid));
        tfBidAmount.setStyle("");
    }

    @FXML
    private void onBidNow() {
        String input = tfBidAmount.getText().replaceAll("[^\\d]", "");
        if (input.isBlank()) {
            setQuickAmount(bidStep);
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(input);
            if (amount.compareTo(currentPrice.add(bidStep)) < 0) {
                tfBidAmount.setStyle(tfBidAmount.getStyle() + "; -fx-border-color: #E03030;");
                return;
            }
            BigDecimal prev = currentPrice;
            currentPrice = amount;
            updatePriceDisplay();
            BigDecimal diff = amount.subtract(prev);
            lblPriceChange.setText("Tăng " + String.format("%,.0f đ", diff) + " so với lần trước");
            addBidHistoryEntry("Bạn", amount, true);
            tfBidAmount.clear();
            tfBidAmount.setStyle("");
            // TODO: gửi lên server
        } catch (NumberFormatException ignored) {
            tfBidAmount.setStyle(tfBidAmount.getStyle() + "; -fx-border-color: #E03030;");
        }
    }

    // ===== CHAT =====

    @FXML
    private void onSendMessage() {
        String msg = tfChatInput.getText().trim();
        if (msg.isBlank()) return;

        addChatMessage("Bạn", "B", "#FFF3EB", "#F97316", msg);
        chatCount++;
        lblChatCount.setText(chatCount + " tin nhắn");
        tfChatInput.clear();
        // TODO: gửi lên server
    }

    // ===== HELPERS =====

    private void updatePriceDisplay() {
        lblCurrentPrice.setText(String.format("%,.0f đ", currentPrice));
        BigDecimal minNext = currentPrice.add(bidStep);
        tfBidAmount.setPromptText(String.format("%,.0f đ", minNext));
    }

    /**
     * Thêm một dòng vào lịch sử đấu giá live.
     * @param name    Tên người dùng hiển thị
     * @param amount  Số tiền
     * @param isMe    true nếu là lượt của người dùng hiện tại
     */
    private void addBidHistoryEntry(String name, BigDecimal amount, boolean isMe) {
        String bg    = isMe ? "#E8F0FE" : "#FAFAFA";
        String badge = isMe ? "Lượt của bạn" : "";
        String time  = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        HBox row = new HBox();
        row.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8; -fx-padding: 8 10;");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(2);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-font-size: 9px; -fx-text-fill: #888;");
        left.getChildren().addAll(lblName, lblTime);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label lblAmount = new Label(String.format("%,.0f đ", amount));
        lblAmount.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        row.getChildren().addAll(left, lblAmount);
        // Thêm vào đầu danh sách (mới nhất lên trên)
        bidHistory.getChildren().add(0, row);
    }

    /**
     * Thêm tin nhắn chat vào panel.
     */
    private void addChatMessage(String username, String initial, String bgColor, String textColor, String message) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label(initial);
        avatar.getStyleClass().add("avatar-chat");
        avatar.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + ";");

        VBox content = new VBox(2);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label(username);
        lblName.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-font-size: 9px; -fx-text-fill: #888;");
        meta.getChildren().addAll(lblName, lblTime);

        Label lblMsg = new Label(message);
        lblMsg.setStyle("-fx-font-size: 11px; -fx-text-fill: #444;");
        lblMsg.setWrapText(true);

        content.getChildren().addAll(meta, lblMsg);
        row.getChildren().addAll(avatar, content);
        chatMessages.getChildren().add(row);
    }

    // ===== ĐỒNG HỒ ĐẾM NGƯỢC =====

    private void startCountdown() {
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (remainSeconds <= 0) {
                countdown.stop();
                lblMinutes.setText("00");
                lblSeconds.setText("00");
                // TODO: hiện banner kết thúc phiên
                return;
            }
            remainSeconds--;
            int m = remainSeconds / 60;
            int s = remainSeconds % 60;
            lblMinutes.setText(String.format("%02d", m));
            lblSeconds.setText(String.format("%02d", s));
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }
}
