package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.service.NotificationService;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;

public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @FXML private VBox vboxNotificationContainer;
    @FXML private VBox vboxNoNotifications;

    @FXML
    public void initialize() {
        NotificationService.loadAll(notifications -> Platform.runLater(() -> {
            vboxNotificationContainer.getChildren().clear();
            if (notifications == null || notifications.isEmpty()) {
                vboxNoNotifications.setVisible(true);
                vboxNoNotifications.setManaged(true);
                return;
            }
            vboxNoNotifications.setVisible(false);
            vboxNoNotifications.setManaged(false);

            for (NotificationDTO notif : notifications) {
                try {
                    URL url = getClass().getResource("/fxml/GenericListItem.fxml");
                    if (url == null) continue;
                    FXMLLoader loader = new FXMLLoader(url);
                    HBox itemNode = loader.load();
                    GenericListItemController controller = loader.getController();

                    String icon = "🔔";
                    String title = notif.getTitle() != null ? notif.getTitle() : "Thông báo mới";
                    String msg = notif.getMessage() != null ? notif.getMessage() : "";
                    String subtitle = UIFormatter.formatInstant(notif.getCreatedAt()) + " • " + msg;
                    
                    String valueStr = notif.isRead() ? "" : "Mới";
                    String valueColor = "#ef4444";
                    String iconBgColor = notif.isRead() ? "#f1f5f9" : "#fee2e2";

                    controller.setData(icon, title, subtitle, valueStr, valueColor, iconBgColor);

                    // Sự kiện Click vào thông báo
                    itemNode.setOnMouseClicked(e -> {
                        // 1. Nếu là thông báo thắng thầu, mở popup đánh giá
                        // Giả sử type từ server trả về cho việc thắng thầu là "AUCTION_WIN"
                        if ("AUCTION_WIN".equals(notif.getType())) {
                            int itemId = notif.getRelatedId();
                            if (itemId > 0) {
                                AuctionService.loadDetail(itemId, item -> {
                                    Platform.runLater(() -> {
                                        if (item != null) {
                                            ClientModel.getInstance().setCurrentAuctionItem(item);
                                            vn.edu.vnu.uet.group8.client.util.ModalUtil.showModal("ĐÁNH GIÁ NGƯỜI BÁN", "ReviewView.fxml");
                                        }
                                    });
                                });
                            }
                        }

                        // 2. Nếu chưa đọc thì mark read
                        if (!notif.isRead()) {
                            NotificationService.markRead(notif.getId(), () -> Platform.runLater(() -> {
                            controller.setData(icon, title, subtitle, "", valueColor, "#f1f5f9");
                            }), null);
                        }
                    });

                    vboxNotificationContainer.getChildren().add(itemNode);
                } catch (Exception e) {
                    log.error("Lỗi khi hiển thị thông báo: {}", e.getMessage());
                }
            }
        }), err -> {});
    }
}