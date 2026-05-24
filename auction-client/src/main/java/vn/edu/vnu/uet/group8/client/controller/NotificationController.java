package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.NotificationService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;

/**
 * NotificationController — quan ly thong bao.
 *
 * Tinh nang:
 *  - Load notification tu server qua NotificationService
 *  - Binding voi ClientModel.notifications -> tu update khi co notif moi (realtime)
 *  - Tab filter: Tat ca / Chua doc / Dau gia / He thong
 *  - Mark read tung notif hoac mark all
 *  - Hien lblNewCount = so chua doc
 */
public class NotificationController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(NotificationController.class.getName());

    @FXML VBox notificationList;
    @FXML Label lblNewCount;
    @FXML Button btnTabAll;
    @FXML Button btnTabUnread;
    @FXML Button btnTabAuction;
    @FXML Button btnTabSystem;

    private Button activeTab;
    private String currentFilter = "all";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        bindNotifications();
        loadNotifications();
    }

    /** Binding voi ClientModel -> tu re-render khi co notif moi. */
    void bindNotifications() {
        ClientModel.getInstance().notificationsProperty().addListener((obs, oldList, newList) ->
                Platform.runLater(this::renderFromModel)
        );

        ClientModel.getInstance().unreadNotificationCountProperty().addListener((obs, oldVal, newVal) ->
                Platform.runLater(() -> updateNewCount(newVal.intValue()))
        );
    }

    void loadNotifications() {
        NotificationService.loadAll(
                list -> {
                    LOGGER.info(() -> "Tai " + list.size() + " thong bao");
                    renderFromModel();
                },
                error -> {
                    LOGGER.warning("Loi tai thong bao: " + error);
                    renderEmpty();
                }
        );
    }

    void renderFromModel() {
        List<NotificationDTO> all = ClientModel.getInstance().getNotifications();
        render(all);
    }

    void render(List<NotificationDTO> all) {
        if (notificationList == null) return;
        notificationList.getChildren().clear();

        if (all == null || all.isEmpty()) {
            renderEmpty();
            return;
        }

        // Filter theo tab
        List<NotificationDTO> filtered = all.stream()
                .filter(this::matchFilter)
                .toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("Khong co thong bao trong muc nay");
            empty.getStyleClass().add("label-info");
            notificationList.getChildren().add(empty);
            return;
        }

        for (NotificationDTO n : filtered) {
            notificationList.getChildren().add(buildItem(n));
        }

        // Update count chua doc
        long unread = all.stream().filter(n -> !n.isRead()).count();
        updateNewCount((int) unread);
    }

    void renderEmpty() {
        notificationList.getChildren().clear();
        Label empty = new Label("Chua co thong bao nao");
        empty.getStyleClass().add("label-info");
        notificationList.getChildren().add(empty);
        updateNewCount(0);
    }

    void updateNewCount(int count) {
        if (lblNewCount == null) return;
        lblNewCount.setText(count + " moi");
        lblNewCount.setVisible(count > 0);
        lblNewCount.setManaged(count > 0);
    }

    private boolean matchFilter(NotificationDTO n) {
        return switch (currentFilter) {
            case "unread"  -> !n.isRead();
            case "auction" -> "AUCTION".equalsIgnoreCase(n.getType());
            case "system"  -> "SYSTEM".equalsIgnoreCase(n.getType());
            default        -> true;
        };
    }

    /** Build 1 row notification voi title + actions. */
    private HBox buildItem(NotificationDTO n) {
        HBox row = new HBox(10);
        row.getStyleClass().add(n.isRead() ? "card-soft" : "card-unread");
        row.setStyle("-fx-padding: 12; -fx-background-radius: 8;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(n.getTitle() != null ? n.getTitle() : "Thong bao");
        title.getStyleClass().add("h3");
        info.getChildren().add(title);

        Button btnRead = new Button(n.isRead() ? "✓ Da doc" : "Danh dau da doc");
        btnRead.setDisable(n.isRead());
        btnRead.setOnAction(e -> markAsRead(n.getId()));

        Button btnDelete = new Button("Xóa");
        btnDelete.getStyleClass().add("btn-danger");
        btnDelete.setOnAction(e -> {
            NotificationService.deleteNotification(n.getId(), 
                () -> LOGGER.fine("Deleted notif " + n.getId()),
                error -> AlertUtil.showError("Lỗi xóa thông báo: " + error));
        });

        row.getChildren().addAll(info, btnRead, btnDelete);
        return row;
    }

    // ===== ACTIONS =====

    /** Danh dau 1 notif la da doc. */
    void markAsRead(int notifId) {
        NotificationService.markRead(notifId,
                () -> {
                    LOGGER.fine(() -> "Marked read: " + notifId);
                },
                error -> AlertUtil.showError("Loi: " + error)
        );
    }

    /** Danh dau TAT CA la da doc. */
    @FXML
    void onMarkAllRead() {
        List<NotificationDTO> unread = ClientModel.getInstance().getNotifications()
                .stream().filter(n -> !n.isRead()).toList();

        if (unread.isEmpty()) {
            AlertUtil.showInfo("Khong co thong bao chua doc");
            return;
        }

        boolean ok = AlertUtil.showConfirm("Xac nhan",
                "Danh dau " + unread.size() + " thong bao la da doc?");
        if (!ok) return;

        LOGGER.info(() -> "Mark all read: " + unread.size() + " items");
        for (NotificationDTO n : unread) {
            NotificationService.markRead(n.getId(), () -> {}, err -> {});
        }
    }

    @FXML
    void onClearRead() {
        boolean ok = AlertUtil.showConfirm("Xac nhan",
                "Xoa tat ca thong bao da doc?");
        if (!ok) return;
        AlertUtil.showInfo("Tinh nang dang phat trien - cho BE bo sung API delete");
    }

    @FXML
    void onDeleteNotification() {
        // Được gọi nếu có nút xóa tất cả (tùy chọn UI FXML)
        AlertUtil.showInfo("Tính năng xóa hàng loạt đang phát triển");
    }

    @FXML
    void onViewDetail() {
        LOGGER.fine("onViewDetail triggered");
        AlertUtil.showInfo("Chi tiet thong bao se hien o day");
    }

    @FXML
    void onBidNow() {
        // Tu thong bao -> nhay sang trang dau gia
        // Can context notif co itemId
        LOGGER.info("Dau gia ngay tu thong bao");
        AlertUtil.showInfo("Chuyen sang trang dau gia (can context tu notif)");
    }

    // ===== TABS =====

    @FXML void onTabAll()     { setTab("all", btnTabAll); }
    @FXML void onTabUnread()  { setTab("unread", btnTabUnread); }
    @FXML void onTabAuction() { setTab("auction", btnTabAuction); }
    @FXML void onTabSystem()  { setTab("system", btnTabSystem); }

    void setTab(String filter, Button button) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            if (!activeTab.getStyleClass().contains("tag-inactive")) {
                activeTab.getStyleClass().add("tag-inactive");
            }
        }
        button.getStyleClass().remove("tag-inactive");
        if (!button.getStyleClass().contains("tag-active")) {
            button.getStyleClass().add("tag-active");
        }
        activeTab = button;
        renderFromModel();
    }
}