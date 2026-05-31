package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;
import java.util.logging.Logger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.PopupUtil;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;

/**
 * Controller chính điều phối layout và navigation cho toàn bộ ứng dụng.
 */
public class MainController {
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML private VBox vboxMainContentArea;
    @FXML private Label lblAvatar;
    @FXML private ImageView imgAvatar;
    @FXML private Label lblOngoingCount;
    @FXML private TextField tfSearch;
    @FXML private Label lblFavCount;
    @FXML private Label lblNotiCount;

    @FXML private Button btnHome;
    @FXML private Button btnExplore;
    @FXML private Button btnMyProducts;
    @FXML private Button btnAccount;
    @FXML private Button btnSettings;

    private static MainController instance;
    private javafx.animation.Timeline syncTimeline;
    private ChangeListener<String> avatarListener;

    @FXML
    public void initialize() {
        instance = this;
        ClientModel model = ClientModel.getInstance();

        // 1. Đồng bộ thông tin Header
        lblAvatar.setText(SessionManager.getAvatarText());
        
        avatarListener = (obs, oldUrl, newUrl) -> {
            UIFormatter.setCircularAvatar(imgAvatar, lblAvatar, newUrl, 42.0);
        };
        model.avatarUrlProperty().addListener(new WeakChangeListener<>(avatarListener));
        UIFormatter.setCircularAvatar(imgAvatar, lblAvatar, model.getAvatarUrl(), 42.0);
        
        // Tải lại thông tin profile để đồng bộ avatar/ví lúc khởi động
        UserService.loadProfile(null);
        
        // Tải thông báo ban đầu
        vn.edu.vnu.uet.group8.client.service.NotificationService.loadAll(null, null);
        
        // Đăng ký nhận thông báo real-time khi đang mở app
        vn.edu.vnu.uet.group8.client.service.NotificationService.subscribePush(notif -> {
            javafx.application.Platform.runLater(() -> {
                
                
                // Trừ khi bị vượt giá tiền max (autobid bị tắt) thì mới hiện popup
                if ("OUTBID".equals(notif.getType())) {
                    int itemId = notif.getRelatedId();
                    if (itemId <= 0 && notif.getMessage() != null) {
                        // Fallback: parse itemId từ message dạng "... sản phẩm #123..."
                        int hashIdx = notif.getMessage().indexOf('#');
                        if (hashIdx != -1) {
                            int spaceIdx = notif.getMessage().indexOf(' ', hashIdx);
                            String idStr = spaceIdx != -1 ? notif.getMessage().substring(hashIdx + 1, spaceIdx) : notif.getMessage().substring(hashIdx + 1);
                            try {
                                itemId = Integer.parseInt(idStr.trim());
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                    if (itemId > 0) {
                        vn.edu.vnu.uet.group8.client.service.BidService.getAutoBidStatus(itemId, isActive -> {
                            javafx.application.Platform.runLater(() -> {
                                if (!isActive) {
                                    // Chỉ hiển thị popup khi Auto-bid đã bị tắt (vượt giá max) hoặc người dùng không bật Auto-bid
                                    vn.edu.vnu.uet.group8.client.util.AlertUtil.showInfo("🔔 " + notif.getTitle() + "\n" + notif.getMessage());
                                }
                            });
                        });
                        return; // Bỏ qua hiển thị popup mặc định
                    }
                }
                
                // Mặc định hiển thị popup cho các thông báo khác
                vn.edu.vnu.uet.group8.client.util.AlertUtil.showInfo("🔔 " + notif.getTitle() + "\n" + notif.getMessage());
            });
        });

        // Bind các con số thống kê (Badge) từ Model để tự động cập nhật UI
        lblFavCount.textProperty().bind(model.favCountProperty().asString());
        lblNotiCount.textProperty().bind(model.unreadNotificationCountProperty().asString());
        lblOngoingCount.textProperty().bind(model.auctionItemsProperty().sizeProperty().asString());

        // 2. Load View mặc định khi vừa vào App là Homepage (HomeView)
        switchView("HomeView.fxml", "HOME");

        // 3. Bắt sự kiện Enter ở ô Tìm kiếm
        if (tfSearch != null) {
            tfSearch.setOnAction(e -> {
                ClientModel.getInstance().setSearchQuery(tfSearch.getText().trim());
                switchView("ExploreView.fxml", null);
            });
        }

        // 4. Định kỳ đồng bộ các con số thống kê (Badge) từ server mỗi 10 giây
        syncTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(10), event -> {
                if (SessionManager.isLoggedIn() && vn.edu.vnu.uet.group8.client.networking.AuctionClient.getInstance().isConnected()) {
                    vn.edu.vnu.uet.group8.client.service.AuctionService.loadAll(null, null);
                    vn.edu.vnu.uet.group8.client.service.NotificationService.loadAll(null, null);
                    vn.edu.vnu.uet.group8.client.service.FavoriteService.loadAll(null, null);
                }
            })
        );
        syncTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        syncTimeline.play();
    }

    public static MainController getInstance() {
        return instance;
    }

    /**
     * Xử lý điều hướng khi bấm vào Menu Sidebar dựa trên userData của Button.
     */
    @FXML
    private void onNavClick(ActionEvent event) {
        Node source = (Node) event.getSource();
        String targetView = (String) source.getUserData();
        
        if (targetView == null) return;

        switch (targetView) {
            case "HOME": switchView("HomeView.fxml", targetView); break;
            case "PROFILE": switchView("UserDashboard.fxml", targetView); break;
            case "EXPLORE": switchView("ExploreView.fxml", targetView); break;
        case "SELLER": switchView("ItemDashboard.fxml", targetView); break;
        case "LIVE": switchView("ExploreView.fxml", "EXPLORE"); break;
            case "SETTINGS": switchView("SettingsView.fxml", targetView); break;
            default: 
                LOGGER.warning("Nav target '" + targetView + "' chưa được xử lý.");
        }
    }

    public void switchView(String fxml, String navId) {
        loadContentView(fxml);
        updateNavStyles(navId);
    }

    public void updateNavStyles(String navId) {
        if (btnHome == null) return;
        
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-alignment: center-left; -fx-padding: 10 22 10 22; -fx-font-size: 14px; -fx-cursor: hand;";
        btnHome.setStyle(baseStyle);
        if (btnExplore != null) btnExplore.setStyle(baseStyle);
        if (btnMyProducts != null) btnMyProducts.setStyle(baseStyle);
        if (btnAccount != null) btnAccount.setStyle(baseStyle);
        if (btnSettings != null) btnSettings.setStyle(baseStyle);

        if (navId == null) return;
        
        String activeStyle = "-fx-text-fill: #ffffff; -fx-alignment: center-left; -fx-padding: 12 24 12 24; -fx-font-size: 15px; -fx-font-weight: bold; -fx-border-width: 0 0 0 4px; -fx-cursor: hand;";

        switch (navId) {
            case "HOME":
                btnHome.setStyle("-fx-background-color: linear-gradient(to right, rgba(249,115,22,0.15), transparent); -fx-border-color: #f97316; " + activeStyle);
                break;
            case "PROFILE":
                if (btnAccount != null) btnAccount.setStyle("-fx-background-color: linear-gradient(to right, rgba(239,68,68,0.15), transparent); -fx-border-color: #ef4444; " + activeStyle);
                break;
            case "EXPLORE":
                if (btnExplore != null) btnExplore.setStyle("-fx-background-color: linear-gradient(to right, rgba(59,130,246,0.15), transparent); -fx-border-color: #3b82f6; " + activeStyle);
                break;
            case "SELLER":
                if (btnMyProducts != null) btnMyProducts.setStyle("-fx-background-color: linear-gradient(to right, rgba(34,197,94,0.15), transparent); -fx-border-color: #22c55e; " + activeStyle);
                break;
            case "SETTINGS":
                if (btnSettings != null) btnSettings.setStyle("-fx-background-color: linear-gradient(to right, rgba(168,85,247,0.15), transparent); -fx-border-color: #a855f7; " + activeStyle);
                break;
        }
    }

    /**
     * Nạp FXML con vào vùng center của MainLayout (vboxMainContentArea).
     */
    public void loadContentView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Node view = loader.load();
            vboxMainContentArea.getChildren().setAll(view);
            
            // Cuộn lên đầu trang mỗi khi chuyển view
            if (vboxMainContentArea.getParent() != null) {
                vboxMainContentArea.getParent().requestLayout();
            }
        } catch (IOException e) {
            LOGGER.severe("Không thể nạp view " + fxml + ": " + e.getMessage());
        }
    }

    @FXML private void onLogoClick() { switchView("HomeView.fxml", "HOME"); }
    
    @FXML private void onProfileClick(MouseEvent event) { switchView("UserDashboard.fxml", "PROFILE"); }

    @FXML
    private void onLogout() {
        SessionManager.logout();
    }

    @FXML
    private void onFavoriteClick(MouseEvent event) {
        PopupUtil.showPopup((Node) event.getSource(), "FavoriteContent.fxml");
    }

    @FXML
    private void onNotificationClick(MouseEvent event) {
        PopupUtil.showPopup((Node) event.getSource(), "NotificationContent.fxml");
    }
}