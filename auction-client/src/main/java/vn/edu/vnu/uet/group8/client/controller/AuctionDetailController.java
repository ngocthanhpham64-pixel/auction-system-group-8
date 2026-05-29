package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

public class AuctionDetailController {
    
    @FXML private Label lblCategory;
    @FXML private ImageView imgMain;
    @FXML private Label lblDetailStatusBadge;
    @FXML private Button btnPrevImage;
    @FXML private Button btnNextImage;
    @FXML private Label lblProductName;
    @FXML private Label lblConditionBadge;
    @FXML private Label lblBidStats;
    
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;
    
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStartPrice;
    @FXML private TextField tfBidAmount;
    @FXML private Label lblMinBid;
    
    @FXML private Label btnTabDesc;
    @FXML private VBox paneDesc;
    @FXML private VBox paneSpec;
    @FXML private VBox paneOrigin;
    
    @FXML private Label lblDescription;
    @FXML private Label lblCondition;
    @FXML private Label lblBrand;
    @FXML private Label lblModel;
    @FXML private Label lblMaterial;
    @FXML private Label lblOrigin;
    @FXML private Label lblCertificate;
    
    @FXML private javafx.scene.chart.LineChart<String, Number> bidChart;
    
    @FXML private VBox bidHistory;
    
    @FXML private VBox paneBidForm;
    
    @FXML private Label lblSellerName;
    @FXML private Label lblSellerStats;
    
    @FXML private Button btnAutoToggle;
    @FXML private VBox paneAuto;
    @FXML private TextField tfMaxPrice;
    @FXML private Label lblAutoDesc;
    @FXML private Button btnActivateAuto;
    @FXML private Button btnCancelAuto;
    @FXML private Button btnFavorite;
    
    private javafx.animation.AnimationTimer countdownTimer;
    private javafx.animation.Timeline pollingTimeline;
    private java.util.concurrent.atomic.AtomicBoolean isFetchingHistory = new java.util.concurrent.atomic.AtomicBoolean(false);
    private java.util.concurrent.atomic.AtomicBoolean isFetchingAutoBid = new java.util.concurrent.atomic.AtomicBoolean(false);
    /** Fingerprint của bid history lần cuối render — chỉ rebuild UI khi data thực sự thay đổi */
    private volatile String lastBidHistoryFingerprint = "";
    private java.util.function.Consumer<vn.edu.vnu.uet.group8.common.dto.response.ServerResponse> priceUpdateSub;
    private java.util.function.Consumer<vn.edu.vnu.uet.group8.common.dto.response.ServerResponse> auctionEndedSub;

    private java.util.List<String> currentImageUrls = new java.util.ArrayList<>();
    private int currentImageIndex = 0;

    @FXML
    public void initialize() {
        loadData();
        setupRealtimeUpdates();
        setupPolling();
    }


    private void loadData() {
        vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
        if (item != null) {
            // Load bid history
            reloadBidHistory(item.getItemId());

            lblCategory.setText(item.getCategory() != null ? item.getCategory().name() : "Other");
            lblProductName.setText(item.getTitle() != null ? item.getTitle() : "");
            
            String formattedPrice = vn.edu.vnu.uet.group8.client.util.UIFormatter.formatPrice(item.getCurrentPrice());
            lblCurrentPrice.setText(formattedPrice);
            lblStartPrice.setText("Hiện tại: " + formattedPrice);
            lblMinBid.setText("Tối thiểu: " + formattedPrice);
            
            lblDescription.setText(item.getDescription() != null ? item.getDescription() : "Chưa có mô tả");
            if (lblConditionBadge != null) lblConditionBadge.setText(item.getCondition() != null ? item.getCondition().name() : "USED");
            lblCondition.setText(item.getCondition() != null ? item.getCondition().name() : "USED");
            lblSellerName.setText(item.getSellerUsername() != null ? item.getSellerUsername() : "Người bán ẩn danh");
            
            if (item.getSellerRating() != null && item.getSellerRating().compareTo(java.math.BigDecimal.ZERO) > 0) {
                lblSellerStats.setText("Đánh giá: " + item.getSellerRating() + "/5 (" + item.getTotalItemsSold() + " giao dịch)");
            } else {
                lblSellerStats.setText("Chưa có đánh giá (" + item.getTotalItemsSold() + " giao dịch)");
            }
            
            // Bỏ vô hiệu hóa (khóa) giao diện vì Client chỉ cần validate bằng backend
            if (paneBidForm != null) paneBidForm.setDisable(false);
            if (tfBidAmount != null) tfBidAmount.setDisable(false);
            if (btnAutoToggle != null) btnAutoToggle.setDisable(false);
            if (tfMaxPrice != null) tfMaxPrice.setDisable(false);

            if (item.getStatus() != null) {
                if ("UPCOMING".equals(item.getStatus().name())) {
                    if (lblDetailStatusBadge != null) {
                        lblDetailStatusBadge.setText("SẮP DIỄN RA");
                        lblDetailStatusBadge.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 12;");
                    }
                } else if ("ACTIVE".equals(item.getStatus().name())) {
                    if (lblDetailStatusBadge != null) {
                        lblDetailStatusBadge.setText("🔴 LIVE");
                        lblDetailStatusBadge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 12;");
                    }
                } else {
                    if (lblDetailStatusBadge != null) {
                        lblDetailStatusBadge.setText("ĐÃ KẾT THÚC");
                        lblDetailStatusBadge.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 12;");
                    }
                }
            }

            if (lblBidStats != null) {
                lblBidStats.setText((item.getBidCount() != 0 ? item.getBidCount() : 0) + " lượt đấu giá");
            }
            
            if (item.getImageUrls() != null) {
                currentImageUrls = new java.util.ArrayList<>(item.getImageUrls());
            } else {
                currentImageUrls = new java.util.ArrayList<>();
            }
            currentImageIndex = 0;
            updateImageGallery();

            if (item.getStatus() != null && "UPCOMING".equals(item.getStatus().name())) {
                if (countdownTimer != null) countdownTimer.stop();
                lblHours.setText("00");
                lblMinutes.setText("00");
                lblSeconds.setText("00");
                // Label có thể được set trực tiếp từ ProductCardController nhưng ở đây UI có lblHours, lblMinutes...
            } else if (item.getEndTime() != null) {
                if (countdownTimer != null) countdownTimer.stop();
                countdownTimer = new javafx.animation.AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        long remainingMillis = item.getEndTime().toEpochMilli() - System.currentTimeMillis();
                        if (remainingMillis <= 0) {
                            lblHours.setText("00");
                            lblMinutes.setText("00");
                            lblSeconds.setText("00");
                            this.stop();
                        } else {
                            long totalSeconds = remainingMillis / 1000;
                            long hours = totalSeconds / 3600;
                            long minutes = (totalSeconds % 3600) / 60;
                            long seconds = totalSeconds % 60;
                            lblHours.setText(String.format("%02d", hours));
                            lblMinutes.setText(String.format("%02d", minutes));
                            lblSeconds.setText(String.format("%02d", seconds));
                        }
                    }
                };
                countdownTimer.start();
            }
            
            boolean isFav = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().isFavorite(item.getItemId());
            updateFavoriteButtonUI(isFav);
        } else {
            // Mock data fallback if accessed directly
            lblCategory.setText("Đồ điện tử");
            lblProductName.setText("MacBook Pro M3 Max 16-inch 2024");
            lblCurrentPrice.setText("65.000.000 đ");
            lblStartPrice.setText("Khởi điểm: 50.000.000 đ");
            lblMinBid.setText("Tối thiểu: 65.500.000 đ");
            
            lblDescription.setText("Máy còn rất mới, đầy đủ phụ kiện zin theo máy. Hoạt động hoàn hảo.");
            lblCondition.setText("Đã qua sử dụng (99%)");
            lblSellerName.setText("Nguyễn Văn A");
            lblSellerName.setText("Nguyễn Văn A");
            lblSellerStats.setText("Đánh giá: 4.9/5 (120 giao dịch)");
        }
        
        // Load autobid status
        if (item != null) {
            refreshAutoBidStatus(item.getItemId());
        }
    }

    private void setupPolling() {
        // Polling chỉ là BACKUP — real-time updates đã có qua event subscription (PRICE_UPDATE)
        // Bid history đã được reload khi nhận PRICE_UPDATE, polling chỉ cần refresh auto-bid status
        pollingTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(5000), event -> {
                vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
                if (item != null) {
                    refreshAutoBidStatus(item.getItemId());
                }
            })
        );
        pollingTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pollingTimeline.play();
    }

    
    private void updateAutoBidUI(boolean isActive) {
        if (isActive) {
            if (btnAutoToggle != null) btnAutoToggle.setText("🤖 Đặt giá tự động (ĐANG BẬT) ▼");
            if (btnActivateAuto != null) { btnActivateAuto.setVisible(false); btnActivateAuto.setManaged(false); }
            if (btnCancelAuto != null) { btnCancelAuto.setVisible(true); btnCancelAuto.setManaged(true); }
            if (tfMaxPrice != null) { tfMaxPrice.setVisible(false); tfMaxPrice.setManaged(false); }
            if (lblAutoDesc != null) lblAutoDesc.setText("Hệ thống Auto-bid đang hoạt động cho phiên này. Bấm hủy nếu bạn không muốn tiếp tục.");
        } else {
            if (btnAutoToggle != null) btnAutoToggle.setText("🤖 Đặt giá tự động (ĐANG TẮT) ▼");
            if (btnActivateAuto != null) { btnActivateAuto.setVisible(true); btnActivateAuto.setManaged(true); }
            if (btnCancelAuto != null) { btnCancelAuto.setVisible(false); btnCancelAuto.setManaged(false); }
            if (tfMaxPrice != null) { tfMaxPrice.setVisible(true); tfMaxPrice.setManaged(true); }
            if (lblAutoDesc != null) lblAutoDesc.setText("Hệ thống tự động đấu giá giúp bạn đến mức tối đa bạn thiết lập.");
        }
    }

    private void updateImageGallery() {
        if (currentImageUrls == null || currentImageUrls.isEmpty()) {
            imgMain.setImage(null);
            if (btnPrevImage != null) { btnPrevImage.setVisible(false); btnPrevImage.setManaged(false); }
            if (btnNextImage != null) { btnNextImage.setVisible(false); btnNextImage.setManaged(false); }
            return;
        }

        if (currentImageIndex < 0) currentImageIndex = 0;
        if (currentImageIndex >= currentImageUrls.size()) currentImageIndex = currentImageUrls.size() - 1;

        String url = currentImageUrls.get(currentImageIndex);
        try {
            imgMain.setImage(new javafx.scene.image.Image(url, true));
        } catch (Exception e) {
            // Ignore
        }

        boolean showArrows = currentImageUrls.size() > 1;
        if (btnPrevImage != null) {
            btnPrevImage.setVisible(showArrows);
            btnPrevImage.setManaged(showArrows);
        }
        if (btnNextImage != null) {
            btnNextImage.setVisible(showArrows);
            btnNextImage.setManaged(showArrows);
        }
    }
    
    private void setupRealtimeUpdates() {
        // 1. Đăng ký nhận sự kiện cập nhật giá mỗi khi ai đó Bid
        priceUpdateSub = vn.edu.vnu.uet.group8.client.service.AuctionService.subscribeAuctionStatus(status -> {
            java.util.logging.Logger.getLogger(AuctionDetailController.class.getName()).info("Nhận PRICE_UPDATE: itemId=" + status.getItemId() + ", newPrice=" + status.getCurrentPrice() + ", newEndTime=" + status.getEndTime());
            javafx.application.Platform.runLater(() -> {
                vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
                // Chỉ update nếu Broadcast trả về đúng với Item đang xem
                if (item != null && item.getItemId() == status.getItemId()) {
                    item.setCurrentPrice(status.getCurrentPrice());
                    if (status.getEndTime() != null) {
                        item.setEndTime(status.getEndTime());
                    }
                    String formatted = vn.edu.vnu.uet.group8.client.util.UIFormatter.formatPrice(status.getCurrentPrice());
                    lblCurrentPrice.setText(formatted);
                    lblStartPrice.setText("Hiện tại: " + formatted);
                    lblMinBid.setText("Tối thiểu: " + formatted);
                    
                    // Khi có giá mới, reload lại lịch sử và làm mới trạng thái AutoBid ngay lập tức
                    reloadBidHistory(item.getItemId());
                    refreshAutoBidStatus(item.getItemId());
                }
            });
        });

        // 2. Đăng ký nhận sự kiện khi thời gian hết hoặc bị Admin đóng
        auctionEndedSub = vn.edu.vnu.uet.group8.client.service.AuctionService.subscribeAuctionEnded(event -> {
            javafx.application.Platform.runLater(() -> {
                vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
                if (item != null && item.getItemId() == event.getItemId()) {
                    String msg = "Phiên đấu giá đã khép lại.\nGiá chốt: " + vn.edu.vnu.uet.group8.client.util.UIFormatter.formatPrice(event.getFinalPrice());
                    showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Phiên đã kết thúc", msg);
                    if (tfBidAmount != null) tfBidAmount.setDisable(true); // Khóa nút nhập tiền
                    if (btnAutoToggle != null) btnAutoToggle.setDisable(true);
                    if (lblMinBid != null) lblMinBid.setText("🔒 Phiên đấu giá đã kết thúc");
                }
            });
        });

        // 3. Quan Trọng: Hủy lắng nghe khi người dùng chuyển trang (Node bị gỡ khỏi Scene) để tránh memory leak
        paneDesc.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                vn.edu.vnu.uet.group8.client.service.AuctionService.unsubscribeAuctionStatus(priceUpdateSub);
                vn.edu.vnu.uet.group8.client.service.AuctionService.unsubscribeAuctionEnded(auctionEndedSub);
                if (countdownTimer != null) {
                    countdownTimer.stop();
                }
                if (pollingTimeline != null) {
                    pollingTimeline.stop();
                }
            }
        });
    }

    /**
     * Tạo fingerprint cho bid history data để phát hiện thay đổi.
     * Chỉ rebuild UI khi data thực sự khác — tránh tạo hàng trăm JavaFX node objects vô ích.
     */
    private String computeBidHistoryFingerprint(java.util.List<vn.edu.vnu.uet.group8.common.dto.model.BidRecord> history) {
        if (history == null || history.isEmpty()) return "empty";
        StringBuilder sb = new StringBuilder();
        sb.append(history.size()).append(':');
        // Chỉ cần hash vài record đầu + cuối là đủ nhận biết thay đổi
        int limit = Math.min(history.size(), 3);
        for (int i = 0; i < limit; i++) {
            vn.edu.vnu.uet.group8.common.dto.model.BidRecord r = history.get(i);
            sb.append(r.getBidId()).append('-').append(r.getAmount()).append('-').append(r.getPlacedAt()).append('|');
        }
        return sb.toString();
    }

    private void reloadBidHistory(int itemId) {
        if (!isFetchingHistory.compareAndSet(false, true)) return;
        vn.edu.vnu.uet.group8.client.service.BidService.loadHistory(itemId, history -> {
            isFetchingHistory.set(false);

            // === FINGERPRINT CHECK: Skip UI rebuild nếu data không thay đổi ===
            // Đây là fix chính cho memory leak — tránh tạo hàng trăm HBox/Label/Region mỗi giây
            String newFingerprint = computeBidHistoryFingerprint(history);
            if (newFingerprint.equals(lastBidHistoryFingerprint)) {
                return; // Data giống lần trước → không rebuild UI
            }
            lastBidHistoryFingerprint = newFingerprint;

            javafx.application.Platform.runLater(() -> {
                if (bidHistory != null) {
                    bidHistory.getChildren().clear();
                    if (history == null || history.isEmpty()) {
                        Label emptyLbl = new Label("Chưa có lượt đặt giá nào");
                        emptyLbl.getStyleClass().add("text-sub");
                        emptyLbl.setStyle("-fx-padding: 10 20;");
                        bidHistory.getChildren().add(emptyLbl);
                    } else {
                        // Cập nhật biểu đồ
                        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
                        series.setName("Giá thầu");
                        // Tạo một danh sách đảo ngược để hiển thị trên biểu đồ theo thứ tự thời gian tăng dần
                        java.util.List<vn.edu.vnu.uet.group8.common.dto.model.BidRecord> chartDataList = new java.util.ArrayList<>(history);
                        chartDataList.sort((r1, r2) -> r1.getPlacedAt().compareTo(r2.getPlacedAt()));
                        
                        // Giới hạn 10 điểm gần nhất trên đồ thị để tránh chữ đè lên nhau
                        if (chartDataList.size() > 10) {
                            chartDataList = chartDataList.subList(chartDataList.size() - 10, chartDataList.size());
                        }

                        for (vn.edu.vnu.uet.group8.common.dto.model.BidRecord record : chartDataList) {
                            String timeStr = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                                    .withZone(java.time.ZoneId.systemDefault())
                                    .format(record.getPlacedAt());
                            series.getData().add(new javafx.scene.chart.XYChart.Data<>(timeStr, record.getAmount()));
                        }
                        if (bidChart != null) {
                            bidChart.getData().clear();
                            bidChart.getData().add(series);
                        }

                        // Giới hạn tối đa 50 lượt đặt giá hiển thị danh sách để tránh tràn bộ nhớ và lag UI
                        int count = 0;
                        for (vn.edu.vnu.uet.group8.common.dto.model.BidRecord record : history) {
                            if (count >= 50) break;
                            count++;
                            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
                            row.setSpacing(10);
                            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            row.getStyleClass().add("bid-history-row");
                            
                            Label nameLbl = new Label(record.getDisplayName());
                            nameLbl.getStyleClass().add("text-heading");
                            nameLbl.setPrefWidth(160);
                            
                            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                            
                            Label priceLbl = new Label(vn.edu.vnu.uet.group8.client.util.UIFormatter.formatPrice(record.getAmount()));
                            priceLbl.getStyleClass().add("text-brand");
                            priceLbl.setStyle("-fx-font-weight: bold;");
                            priceLbl.setPrefWidth(130);
                            
                            Label timeLbl = new Label(vn.edu.vnu.uet.group8.client.util.UIFormatter.formatInstant(record.getPlacedAt()));
                            timeLbl.getStyleClass().add("text-sub");
                            timeLbl.setPrefWidth(90);
                            
                            row.getChildren().addAll(nameLbl, spacer, priceLbl, timeLbl);
                            bidHistory.getChildren().add(row);
                        }
                    }
                }
            });
        });
    }

    private void refreshAutoBidStatus(int itemId) {
        if (!isFetchingAutoBid.compareAndSet(false, true)) return;
        vn.edu.vnu.uet.group8.client.service.BidService.getAutoBidStatus(itemId, isActive -> {
            isFetchingAutoBid.set(false);
            javafx.application.Platform.runLater(() -> {
                updateAutoBidUI(isActive);
            });
        });
    }
    
    @FXML
    public void onBack() {
        if (countdownTimer != null) countdownTimer.stop();
        if (MainController.getInstance() != null) {
            MainController.getInstance().loadContentView(SceneManager.VIEW_EXPLORE);
        } else {
            SceneManager.switchTo(SceneManager.VIEW_MAIN);
        }
    }
    
    private void updateFavoriteButtonUI(boolean isFav) {
        if (btnFavorite != null) {
            if (isFav) {
                btnFavorite.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);");
            } else {
                btnFavorite.setStyle("-fx-background-color: white; -fx-text-fill: #ef4444; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);");
            }
        }
    }

    @FXML 
    public void onFavorite() {
        vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
        if (item == null) return;

        boolean currentlyFavorite = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().isFavorite(item.getItemId());

        if (currentlyFavorite) {
            vn.edu.vnu.uet.group8.client.service.FavoriteService.remove(item.getItemId(), () -> {
                showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Thông báo", "Đã xóa khỏi danh sách yêu thích");
                javafx.application.Platform.runLater(() -> updateFavoriteButtonUI(false));
            }, err -> showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", err));
        } else {
            vn.edu.vnu.uet.group8.client.service.FavoriteService.add(item.getItemId(), () -> {
                showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Thông báo", "Đã thêm vào danh sách yêu thích");
                javafx.application.Platform.runLater(() -> updateFavoriteButtonUI(true));
            }, err -> showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", err));
        }
    }
    
    @FXML public void onShare() {}

    @FXML
    public void onPrevImage() {
        if (currentImageUrls != null && currentImageUrls.size() > 1) {
            currentImageIndex--;
            if (currentImageIndex < 0) {
                currentImageIndex = currentImageUrls.size() - 1;
            }
            updateImageGallery();
        }
    }

    @FXML
    public void onNextImage() {
        if (currentImageUrls != null && currentImageUrls.size() > 1) {
            currentImageIndex++;
            if (currentImageIndex >= currentImageUrls.size()) {
                currentImageIndex = 0;
            }
            updateImageGallery();
        }
    }
    
    @FXML public void onTabDesc() {
        paneDesc.setVisible(true); paneDesc.setManaged(true);
        paneSpec.setVisible(false); paneSpec.setManaged(false);
        paneOrigin.setVisible(false); paneOrigin.setManaged(false);
    }
    
    private void addQuickBid(long amount) {
        vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
        if (item != null) {
            String currentText = tfBidAmount.getText();
            long baseAmount = item.getCurrentPrice() != null ? item.getCurrentPrice().longValue() : 0;
            if (currentText != null && !currentText.isEmpty()) {
                try {
                    baseAmount = Long.parseLong(currentText.replace(".", "").replace(",", ""));
                } catch (Exception e) {}
            }
            tfBidAmount.setText(String.valueOf(baseAmount + amount));
        }
    }
    
    @FXML public void onQuickBid1() { addQuickBid(100000); }
    @FXML public void onQuickBid2() { addQuickBid(500000); }
    @FXML public void onQuickBid3() { addQuickBid(1000000); }
    
    @FXML public void onBidNow() {
        String amountText = tfBidAmount.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập số tiền!");
            return;
        }
        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(amountText.trim());
            vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
            if (item != null) {
                vn.edu.vnu.uet.group8.client.service.BidService.placeBid(item.getItemId(), amount, response -> {
                    javafx.application.Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Thành công", response.getMessage());
                            tfBidAmount.clear();
                            // Reload detail to update current price, bid count, and bid history
                            vn.edu.vnu.uet.group8.client.service.AuctionService.loadDetail(item.getItemId(), updatedItem -> {
                                if (updatedItem != null) {
                                    vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().setCurrentAuctionItem(updatedItem);
                                    javafx.application.Platform.runLater(this::loadData);
                                }
                            });
                        } else {
                            String msg = response != null ? response.getMessage() : "Đặt giá thất bại";
                            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", msg);
                            // Cập nhật lại thông tin mới nhất (để hiển thị giá mới do Auto-bid nâng lên)
                            vn.edu.vnu.uet.group8.client.service.AuctionService.loadDetail(item.getItemId(), updatedItem -> {
                                if (updatedItem != null) {
                                    vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().setCurrentAuctionItem(updatedItem);
                                    javafx.application.Platform.runLater(this::loadData);
                                }
                            });
                        }
                    });
                });
            }
        } catch (NumberFormatException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ!");
        }
    }
    
    @FXML public void onToggleAuto() {
        boolean isVisible = paneAuto.isVisible();
        paneAuto.setVisible(!isVisible);
        paneAuto.setManaged(!isVisible);
    }
    
    @FXML public void onActivateAuto() {
        String amountText = tfMaxPrice.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập giá tối đa!");
            return;
        }
        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(amountText.trim());
            vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
            if (item != null) {
                vn.edu.vnu.uet.group8.client.service.BidService.setAutoBid(item.getItemId(), amount, response -> {
                    javafx.application.Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Thành công", response.getMessage() != null ? response.getMessage() : "Đã kích hoạt Auto-bid thành công!");
                            tfMaxPrice.clear();
                            updateAutoBidUI(true);
                            // Cập nhật lại số dư từ user profile nếu muốn, nhưng hiện tại đã update balance lúc trừ
                        } else {
                            String msg = (response != null && response.getMessage() != null) ? response.getMessage() : "Không thể kích hoạt Auto-bid!";
                            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", msg);
                        }
                    });
                });
            }
        } catch (NumberFormatException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ!");
        }
    }

    @FXML public void onCancelAuto() {
        vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO item = vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().getCurrentAuctionItem();
        if (item != null) {
            vn.edu.vnu.uet.group8.client.service.BidService.setAutoBid(item.getItemId(), java.math.BigDecimal.ZERO, response -> {
                javafx.application.Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Thành công", response.getMessage() != null ? response.getMessage() : "Đã hủy tính năng Auto-bid!");
                        updateAutoBidUI(false);
                    } else {
                        String msg = (response != null && response.getMessage() != null) ? response.getMessage() : "Không thể hủy Auto-bid!";
                        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", msg);
                    }
                });
            });
        }
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        if (type == javafx.scene.control.Alert.AlertType.INFORMATION) {
            vn.edu.vnu.uet.group8.client.util.ModalUtil.showSuccessModal(title, content);
        } else if (type == javafx.scene.control.Alert.AlertType.ERROR) {
            vn.edu.vnu.uet.group8.client.util.ModalUtil.showFailureModal(title, content);
        } else {
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
    }
}
