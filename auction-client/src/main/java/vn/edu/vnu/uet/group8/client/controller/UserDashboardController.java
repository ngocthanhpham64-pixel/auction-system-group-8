package vn.edu.vnu.uet.group8.client.controller;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.ModalUtil;
import vn.edu.vnu.uet.group8.client.util.PopupUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.response.LoginResponse;

public class UserDashboardController implements Initializable {

    // =========================================================================
    // 1. KHAI BÁO CÁC LINH KIỆN ĐỒ HỌA (MẪU ĐỒNG BỘ CHUẨN AERO FXML)
    // =========================================================================
    @FXML private Label lblAvatarLetter;
    @FXML private Label lblAvatarLetter1;
    @FXML private ImageView imgAvatar1;
    @FXML private Label lblOngoingCount;
    
    // Thông tin hồ sơ cá nhân
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileEmail;
    @FXML private Label lblJoinDate;
    
    // Trạng thái bảo mật
    @FXML private Label lblSecuritySummary;
    @FXML private Label lblKycStatus;
    @FXML private Label lblLastLogin;

    // Số liệu tài chính và thống kê (ID mới từ FXML)
    @FXML private Label lblBalance;
    @FXML private Label lblAvailableBalance;
    @FXML private Label lblRatingValue;
    @FXML private Label lblReviewCount;
    @FXML private Label lblPositiveRate;
    @FXML private Label lblTotalWonItems;
    @FXML private Label lblTotalSpent;
    @FXML private Label lblTotalSalesRevenue;
    @FXML private Label lblInventoryStatus;
    @FXML private Label lblTotalSoldItems;
    @FXML private Label lblSalesSuccessRate;
    
    // Biểu đồ
    @FXML private LineChart<String, Number> activityChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    
    // Hệ thống nút bấm chức năng ở Sidebar và Bo mạch điều khiển
    @FXML private Button btnMyBids;
    @FXML private Button btnMyProducts;
    @FXML private Button btnHome;
    @FXML private Button btnBidNow;
    @FXML private Button btnSettings;
    @FXML private Button btnAccount;
    @FXML private Button btnLogout;
    @FXML private Hyperlink lnkEditProfile;

    // Hiệu ứng đổ bóng lơ lửng lấy ra từ thẻ <fx:define> của FXML
    @FXML private DropShadow hoverEffectLift;
    
    private java.util.function.Consumer<vn.edu.vnu.uet.group8.common.dto.response.ServerResponse> auctionEndedSub;

    // Listeners để tránh memory leak
    private ChangeListener<LoginResponse> userListener;
    private ChangeListener<String> avatarListener;
    private ChangeListener<BigDecimal> balanceListener;

    // =========================================================================
    // 2. HÀM KHỞI TẠO HỆ THỐNG VẬN HÀNH (INITIALIZE)
    // =========================================================================
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientModel model = ClientModel.getInstance();
        
        // ---------------------------------------------------------------------
        // LUỒNG 1: BIND DỮ LIỆU ĐỒNG BỘ NGƯỜI DÙNG (Tên, Chữ đại diện Avatar)
        // ---------------------------------------------------------------------
        userListener = (obs, oldUser, newUser) -> {
            if (newUser != null) {
                String fullName = newUser.getFullName() != null && !newUser.getFullName().isEmpty() 
                        ? newUser.getFullName() : newUser.getUsername();
                if (lblProfileName != null) lblProfileName.setText(fullName);
                
                String avatarText = SessionManager.getAvatarText();
                if (lblAvatarLetter != null) lblAvatarLetter.setText(avatarText);
                if (lblAvatarLetter1 != null) lblAvatarLetter1.setText(avatarText);
            }
        };
        model.currentUserProperty().addListener(new WeakChangeListener<>(userListener));

        avatarListener = (obs, oldUrl, newUrl) -> {
            UIFormatter.setCircularAvatar(imgAvatar1, lblAvatarLetter1, newUrl, 80.0);
        };
        model.avatarUrlProperty().addListener(new WeakChangeListener<>(avatarListener));
        
        UIFormatter.setCircularAvatar(imgAvatar1, lblAvatarLetter1, model.getAvatarUrl(), 80.0);

        // ---------------------------------------------------------------------
        // LUỒNG 2: BIND SỐ DƯ VÍ TỰ ĐỘNG (Ví điện tử tự nảy số thời gian thực)
        // ---------------------------------------------------------------------
        if (lblBalance != null) {
            lblBalance.setText(UIFormatter.formatPrice(model.getBalance()));
        }

        balanceListener = (obs, oldVal, newVal) -> {
            String formatted = UIFormatter.formatPrice(newVal);
            if (lblBalance != null) {
                Platform.runLater(() -> lblBalance.setText(formatted));
            }
        };
        model.balanceProperty().addListener(new WeakChangeListener<>(balanceListener));

        // ---------------------------------------------------------------------
        // LUỒNG 4: RÀNG BUỘC SỰ KIỆN CHỦ ĐỘNG CHO TOÀN BỘ HỆ THỐNG NÚT BẤM
        // ---------------------------------------------------------------------
        if (btnLogout != null) btnLogout.setOnAction(e -> SessionManager.logout());
        if (lnkEditProfile != null) lnkEditProfile.setOnAction(e -> onAccountClick());
        if (btnAccount != null) btnAccount.setOnAction(e -> onAccountClick());
        if (btnSettings != null) btnSettings.setOnAction(e -> AlertUtil.showInfo("Hệ thống cài đặt đang được tối ưu hóa."));
        
        if (btnMyBids != null) btnMyBids.setOnAction(e -> ModalUtil.showModal("LỊCH SỬ ĐẤU GIÁ", "AuctionHistoryContent.fxml"));
        if (btnMyProducts != null) btnMyProducts.setOnAction(e -> ModalUtil.showModal("QUẢN LÝ KHO HÀNG", "SalesManagementContent.fxml"));
        
        if (btnHome != null) btnHome.setOnAction(e -> SceneManager.switchTo(SceneManager.VIEW_MAIN, "Auctiva - Live Online Auction"));
        if (btnBidNow != null) btnBidNow.setOnAction(e -> AlertUtil.showInfo("Nút này sẽ chuyển sang màn hình tham gia Đấu giá trực tiếp."));

        // Đăng ký nhận sự kiện khi có phiên đấu giá kết thúc để tự động làm mới thống kê
        auctionEndedSub = vn.edu.vnu.uet.group8.client.service.AuctionService.subscribeAuctionEnded(event -> {
            Platform.runLater(this::loadFreshData);
        });

        // Hủy đăng ký khi rời khỏi view (Node bị tháo khỏi Scene)
        if (lblBalance != null) {
            lblBalance.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null && auctionEndedSub != null) {
                    vn.edu.vnu.uet.group8.client.service.AuctionService.unsubscribeAuctionEnded(auctionEndedSub);
                }
            });
        }

        // Kích hoạt lấy thông tin mới nhất từ máy chủ SQLite/Network
        loadFreshData();
        loadActivityChart();
    }

    // =========================================================================
    // 3. CÁC PHƯƠNG THỨC BỔ TRỢ & XỬ LÝ SỰ KIỆN ĐỘNG
    // =========================================================================
    
    /**
     * Hàm nội bộ xử lý "Under the hood" việc ép cấu trúc nổi khối lơ lửng khi di chuột
     */
    private void setupAeroHoverEffect(VBox card) {
        if (card == null) return;
        
        // Lưu lại lớp đổ bóng Aero tĩnh ban đầu của thẻ
        Effect staticShadow = card.getEffect();

        // Di chuột vào: Bơm hiệu ứng kính lỏng nổi khối sâu
        card.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            if (hoverEffectLift != null) {
                card.setEffect(hoverEffectLift);
            }
        });

        // Rút chuột ra: Trả lại trạng thái cân bằng ảnh bóng ban đầu
        card.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            card.setEffect(staticShadow);
        });
    }

    /**
     * Lấy dữ liệu tươi từ database và đổ vào các thẻ phân khúc Aero
     */
    private void loadFreshData() {
        UserService.loadProfile(profile -> {
            if (profile != null) {
                Platform.runLater(() -> {
                    if (lblProfileEmail != null) lblProfileEmail.setText(profile.getEmail());
                    // Cập nhật số liệu cho các thẻ phân phối Aero mới
                    if (lblRatingValue != null) {
                        lblRatingValue.setText(profile.getSellerRating() != null ? profile.getSellerRating() + " ★" : "0.0 ★");
                    }
                    if (lblJoinDate != null) {
                        if (profile.getCreatedAt() != null) {
                            lblJoinDate.setText(UIFormatter.formatInstant(profile.getCreatedAt()));
                        } else {
                            lblJoinDate.setText("Đang tải...");
                        }
                    }
                    // Đồng bộ số dư hiển thị nếu property chưa kịp nhảy
                    if (lblBalance != null && profile.getBalance() != null) {
                        lblBalance.setText(UIFormatter.formatPrice(profile.getBalance()));
                    }
                    // Cập nhật biểu đồ hoạt động
                    loadActivityChart();
                });
            }
        });

        // 1. Tải thống kê mua hàng (Tổng chi tiêu, số phiên thắng)
        UserService.loadPurchaseHistory(items -> {
            Platform.runLater(() -> {
                if (lblTotalWonItems != null) {
                    int count = (items != null) ? items.size() : 0;
                    lblTotalWonItems.setText(count + " Phiên thầu thắng");
                }
                if (lblTotalSpent != null) {
                    BigDecimal totalSpent = BigDecimal.ZERO;
                    if (items != null) {
                        for (var item : items) {
                            if (item.getCurrentPrice() != null) {
                                totalSpent = totalSpent.add(item.getCurrentPrice());
                            }
                        }
                    }
                    lblTotalSpent.setText(UIFormatter.formatPrice(totalSpent));
                }
            });
        });

        // 2. Tải thống kê bán hàng (Doanh thu, tỉ lệ chốt, kho hàng)
        vn.edu.vnu.uet.group8.client.service.SellerService.getMyListings(items -> {
            Platform.runLater(() -> {
                int soldCount = 0;
                int activeCount = 0;
                int draftCount = 0;
                BigDecimal totalRevenue = BigDecimal.ZERO;
                int totalCompleted = 0; // Số phiên đã xong (cả bán được và móm)
                
                if (items != null) {
                    for (var item : items) {
                        // Logic xác định bản nháp đồng bộ với MyProductsController
                        boolean isDraft = (item.getStatus() == null) || 
                                         "DRAFT".equals(item.getStatus().name()) || 
                                         (item.getCurrentPrice() == null && item.getEndTime() == null);
                        
                        if (isDraft) {
                            draftCount++;
                            continue;
                        }

                        String statusName = item.getStatus().name();
                        if ("SOLD".equals(statusName)) {
                            soldCount++;
                            totalCompleted++;
                            if (item.getCurrentPrice() != null) {
                                totalRevenue = totalRevenue.add(item.getCurrentPrice());
                            }
                        } else if ("ENDED_NO_BID".equals(statusName)) {
                            totalCompleted++;
                        } else if ("ACTIVE".equals(statusName) || "UPCOMING".equals(statusName)) {
                            activeCount++;
                        }
                    }
                }
                
                if (lblTotalSalesRevenue != null) lblTotalSalesRevenue.setText(UIFormatter.formatPrice(totalRevenue));
                if (lblTotalSoldItems != null) lblTotalSoldItems.setText(soldCount + " sản phẩm");
                if (lblInventoryStatus != null) lblInventoryStatus.setText(activeCount + " đang sàn | " + draftCount + " nháp");
                
                if (lblSalesSuccessRate != null) {
                    int rate = totalCompleted == 0 ? 0 : (int)((soldCount * 100.0) / totalCompleted);
                    lblSalesSuccessRate.setText(rate + "%");
                }
            });
        }, err -> {});

        // 3. Tải thống kê đánh giá
        vn.edu.vnu.uet.group8.client.service.RatingService.loadSellerReviews(SessionManager.getUserId(), reviews -> {
            Platform.runLater(() -> {
                if (reviews == null || reviews.isEmpty()) {
                    if (lblReviewCount != null) lblReviewCount.setText("0 lượt");
                    if (lblPositiveRate != null) lblPositiveRate.setText("0%");
                } else {
                    if (lblReviewCount != null) lblReviewCount.setText(reviews.size() + " lượt");
                    int positive = (int) reviews.stream().filter(r -> r.getScore() >= 4).count();
                    int rate = (int)((positive * 100.0) / reviews.size());
                    if (lblPositiveRate != null) lblPositiveRate.setText(rate + "%");
                }
            });
        });
    }

    @FXML
    public void onDepositClick() {
        ModalUtil.showModal("NẠP TIỀN VÀO VÍ ĐIỆN TỬ", "DepositContent.fxml");
    }

    @FXML
    public void onWithdrawClick() {
        ModalUtil.showModal("YÊU CẦU RÚT TIỀN", "WithdrawContent.fxml");
    }

    @FXML
    public void onAccountClick() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().switchView("DetailUserDashboard.fxml", "PROFILE");
        } else {
            SceneManager.switchTo("DetailUserDashboard.fxml", "Auctiva - Hồ sơ cá nhân");
        }
    }

    @FXML 
    public void onNavFavoritesClick(MouseEvent event) {
        PopupUtil.showPopup((Node) event.getSource(), "FavoriteContent.fxml");
    }
    
    @FXML 
    public void onNavNotificationsClick(MouseEvent event) {
        PopupUtil.showPopup((Node) event.getSource(), "NotificationContent.fxml");
    }

    @FXML
    public void onChangePasswordClick() {
        ModalUtil.showModal("ĐỔI MẬT KHẨU", "ChangePasswordContent.fxml");
    }

    @FXML
    public void onViewTransactionHistoryClick() {
        ModalUtil.showModal("LỊCH SỬ BIẾN ĐỘNG SỐ DƯ", "ListContainer.fxml"); // Đã sửa tên file
    }

    @FXML
    public void onViewReviewHistoryClick() {
        ModalUtil.showModal("ĐÁNH GIÁ TỪ NGƯỜI MUA", "BuyerReviewsContent.fxml");
    }

    @FXML
    public void onViewPurchaseHistoryClick() {
        ModalUtil.showModal("NHẬT KÝ MUA HÀNG", "PurchaseHistoryContent.fxml");
    }

    @FXML
    public void onViewSalesHistoryClick() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().switchView("ItemDashboard.fxml", "SELLER");
        } else {
            ModalUtil.showModal("QUẢN LÝ BÁN HÀNG", "ItemDashboard.fxml");
        }
    }

    private void loadActivityChart() {
        if (activityChart == null) return;
        
        activityChart.getData().clear();
        
        XYChart.Series<String, Number> bidSeries = new XYChart.Series<>();
        bidSeries.setName("Lượt đặt giá");
        
        XYChart.Series<String, Number> winSeries = new XYChart.Series<>();
        winSeries.setName("Lượt thắng thầu");
        
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startDate = today.minusDays(6);
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
        
        java.util.Map<java.time.LocalDate, Integer> bidCounts = new java.util.HashMap<>();
        java.util.Map<java.time.LocalDate, Integer> winCounts = new java.util.HashMap<>();
        
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate date = startDate.plusDays(i);
            bidCounts.put(date, 0);
            winCounts.put(date, 0);
        }
        
        UserService.loadMyBids(bids -> {
            if (bids != null) {
                for (var bid : bids) {
                    if (bid.getBidTime() != null) {
                        java.time.LocalDate bidDate = bid.getBidTime().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        if (bidCounts.containsKey(bidDate)) {
                            bidCounts.put(bidDate, bidCounts.get(bidDate) + 1);
                        }
                    }
                }
            }
            
            UserService.loadPurchaseHistory(purchases -> {
                if (purchases != null) {
                    for (var item : purchases) {
                        if (item.getEndTime() != null) {
                            java.time.LocalDate winDate = item.getEndTime().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                            if (winCounts.containsKey(winDate)) {
                                winCounts.put(winDate, winCounts.get(winDate) + 1);
                            }
                        }
                    }
                }
                
                Platform.runLater(() -> {
                    for (int i = 0; i < 7; i++) {
                        java.time.LocalDate date = startDate.plusDays(i);
                        String label = date.format(dtf);
                        bidSeries.getData().add(new XYChart.Data<>(label, bidCounts.get(date)));
                        winSeries.getData().add(new XYChart.Data<>(label, winCounts.get(date)));
                    }
                    activityChart.getData().addAll(bidSeries, winSeries);
                });
            });
        });
    }
}