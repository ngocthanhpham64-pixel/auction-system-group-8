package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.NotificationService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.common.dto.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * NotificationController — wire NotificationService.
 *
 * Tabs: Tất cả / Chưa đọc / Đấu giá / Hệ thống.
 * Subscribe push notification realtime — tự render khi server đẩy notif mới.
 */
public class NotificationController implements Initializable {

    @FXML private VBox notificationList;
    @FXML private Label lblTotal;
    @FXML private Button btnTabAll;
    @FXML private Button btnTabUnread;
    @FXML private Button btnTabAuction;
    @FXML private Button btnTabSystem;

    private Button activeTab;
    private String currentFilter = "all";  // "all" / "unread" / "auction" / "system"

    /** Wrapper subscribe — giữ để unsubscribe khi rời view */
    private Consumer<ServerResponse> pushSubscription;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        loadNotifications();
        subscribePush();
    }

    // ===== LOAD =====

    private void loadNotifications() {
        NotificationService.loadAll(
                list -> render(list),
                error -> {
                    lblTotal.setText("Lỗi: " + error);
                    notificationList.getChildren().clear();
                }
        );
    }

    private void subscribePush() {
        pushSubscription = NotificationService.subscribePush(notif -> {
            // Notif mới đến → ClientModel đã update, reload list
            render(ClientModel.getInstance().getNotifications());
        });
    }

    // ===== RENDER =====

    private void render(List<NotificationDTO> all) {
        notificationList.getChildren().clear();
        if (all == null || all.isEmpty()) {
            lblTotal.setText("Chưa có thông báo");
            return;
        }

        List<NotificationDTO> filtered = all.stream()
                .filter(this::matchFilter)
                .toList();

        for (NotificationDTO n : filtered) {
            notificationList.getChildren().add(buildItem(n));
        }

        long unread = all.stream().filter(n -> !n.isRead()).count();
        lblTotal.setText("Tổng: " + all.size() + " | Chưa đọc: " + unread);
    }

    private boolean matchFilter(NotificationDTO n) {
        return switch (currentFilter) {
            case "unread"  -> !n.isRead();
            case "auction" -> "AUCTION".equalsIgnoreCase(n.getType());
            case "system"  -> "SYSTEM".equalsIgnoreCase(n.getType());
            default        -> true;
        };
    }

    private HBox buildItem(NotificationDTO n) {
        HBox row = new HBox(10);
        row.getStyleClass().add(n.isRead() ? "card-soft" : "card-unread");

        Label title = new Label(n.getTitle());
        title.getStyleClass().add("h3");

        Label content = new Label("(Noi dung)");
        content.setWrapText(true);

        VBox box = new VBox(4, title, content);

        Button markRead = new Button(n.isRead() ? "✓" : "Đánh dấu đã đọc");
        markRead.setDisable(n.isRead());
        markRead.setOnAction(e -> markAsRead(n.getId()));

        Button delete = new Button("Xóa");
        delete.setOnAction(e -> deleteNotification(n.getId()));

        row.getChildren().addAll(box, markRead, delete);
        return row;
    }

    private void markAsRead(int notifId) {
        NotificationService.markRead(notifId,
                () -> render(ClientModel.getInstance().getNotifications()),
                err -> AlertUtil.showError(err)
        );
    }

    private void deleteNotification(int notifId) {
        // TODO: NotificationService chưa có deleteNotification — báo BE bổ sung
        // Tạm thời chỉ đánh dấu đã đọc
        markAsRead(notifId);
    }

    // ===== TABS =====

    @FXML private void onTabAll()     { setTab("all", btnTabAll); }
    @FXML private void onTabUnread()  { setTab("unread", btnTabUnread); }
    @FXML private void onTabAuction() { setTab("auction", btnTabAuction); }
    @FXML private void onTabSystem()  { setTab("system", btnTabSystem); }

    private void setTab(String filter, Button button) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            activeTab.getStyleClass().add("tag-inactive");
        }
        button.getStyleClass().remove("tag-inactive");
        button.getStyleClass().add("tag-active");
        activeTab = button;
        render(ClientModel.getInstance().getNotifications());
    }

    @FXML
    private void onMarkAllRead() {
        List<NotificationDTO> unread = ClientModel.getInstance().getNotifications()
                .stream().filter(n -> !n.isRead()).toList();
        for (NotificationDTO n : unread) {
            NotificationService.markRead(n.getId(), () -> {}, err -> {});
        }
    }

    @FXML
    private void onClearRead() {
        // TODO: BE chưa có API xóa hàng loạt
        AlertUtil.showInfo("Tính năng đang phát triển");
    }

    @FXML
    private void onDeleteNotification() {
        // Bị xóa khi click nút Xóa trong card — không xử lý chung
    }

    /**
     * Cleanup khi rời view — controller cha gọi.
     */
    public void cleanup() {
        if (pushSubscription != null) {
            NotificationService.unsubscribePush(pushSubscription);
        }
    }
}