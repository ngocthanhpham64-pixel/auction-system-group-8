package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private Label lblNewCount;
    @FXML private VBox notificationList;

    // Tabs
    @FXML private Button btnTabAll;
    @FXML private Button btnTabUnread;
    @FXML private Button btnTabAuction;
    @FXML private Button btnTabSystem;

    // ===== STATE =====
    private Button activeTab;
    private int unreadCount = 3;
    /** "all" | "unread" | "auction" | "system" */
    private String currentFilter = "all";

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        updateUnreadLabel();
    }

    // ===== TABS =====

    @FXML private void onTabAll()     { setTab(btnTabAll,     "all");     }
    @FXML private void onTabUnread()  { setTab(btnTabUnread,  "unread");  }
    @FXML private void onTabAuction() { setTab(btnTabAuction, "auction"); }
    @FXML private void onTabSystem()  { setTab(btnTabSystem,  "system");  }

    private void setTab(Button target, String filter) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            if (!activeTab.getStyleClass().contains("tag-inactive")) {
                activeTab.getStyleClass().add("tag-inactive");
            }
        }
        target.getStyleClass().remove("tag-inactive");
        if (!target.getStyleClass().contains("tag-active")) {
            target.getStyleClass().add("tag-active");
        }
        activeTab = target;
        // TODO: filter danh sách notificationList theo currentFilter
    }

    // ===== ACTIONS =====

    @FXML
    private void onMarkAllRead() {
        unreadCount = 0;
        updateUnreadLabel();
        // TODO: đánh dấu tất cả đã đọc trên server
        // Đổi style tất cả card-unread → card-soft
        notificationList.getChildren().forEach(node -> {
            node.getStyleClass().remove("card-unread");
            if (!node.getStyleClass().contains("card-soft")) {
                node.getStyleClass().add("card-soft");
            }
        });
        System.out.println("[NotificationController] Đã đánh dấu tất cả đã đọc");
    }

    @FXML
    private void onClearRead() {
        // Xóa tất cả node KHÔNG có class "card-unread" (tức là đã đọc)
        notificationList.getChildren().removeIf(node ->
                !node.getStyleClass().contains("card-unread")
        );
        System.out.println("[NotificationController] Đã xóa thông báo đã đọc");
    }

    @FXML
    private void onBidNow() {
        // TODO: điều hướng sang AuctionDetailView với item liên quan
        System.out.println("[NotificationController] Đặt giá ngay");
    }

    @FXML
    private void onViewDetail() {
        // TODO: điều hướng sang AuctionDetailView
        System.out.println("[NotificationController] Xem chi tiết");
    }

    @FXML
    private void onDeleteNotification() {
        // TODO: xác định node cha HBox và xóa khỏi notificationList
        // Cách dùng: truyền event và traverse lên
        System.out.println("[NotificationController] Xóa thông báo");
    }

    // ===== HELPERS =====

    private void updateUnreadLabel() {
        if (unreadCount > 0) {
            lblNewCount.setText(unreadCount + " thông báo mới");
        } else {
            lblNewCount.setText("Không có thông báo mới");
        }
    }
}
