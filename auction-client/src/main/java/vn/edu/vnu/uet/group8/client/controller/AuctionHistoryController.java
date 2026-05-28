package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.UserBidHistoryDTO;

public class AuctionHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(AuctionHistoryController.class);

    @FXML
    private VBox vboxAuctionHistoryList;

    @FXML
    public void initialize() {
        // Tăng tốc độ cuộn nếu parent là ScrollPane
        Platform.runLater(() -> {
            if (vboxAuctionHistoryList.getParent() instanceof ScrollPane scrollPane) {
                vboxAuctionHistoryList.setOnScroll(event -> {
                    double deltaY = event.getDeltaY() * 1.5;
                    scrollPane.setVvalue(scrollPane.getVvalue() - deltaY / scrollPane.getHeight());
                    event.consume();
                });
            }
        });
        UserService.loadMyBids(records -> Platform.runLater(() -> {
            vboxAuctionHistoryList.getChildren().clear();
            if (records == null || records.isEmpty()) {
                Label emptyLabel = new Label("Chưa có lịch sử đấu giá nào gần đây.");
                emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 10;");
                vboxAuctionHistoryList.getChildren().add(emptyLabel);
                return;
            }

            for (UserBidHistoryDTO record : records) {
                try {
                    URL url = getClass().getResource("/fxml/GenericListItem.fxml");
                    if (url == null) continue;
                    
                    FXMLLoader loader = new FXMLLoader(url);
                    HBox itemNode = loader.load();
                    GenericListItemController controller = loader.getController();

                    String title = "Phiên đấu giá #" + record.getSessionId();
                    String subtitle = "Vào lúc: " + UIFormatter.formatInstant(record.getBidTime());
                    String valueStr = UIFormatter.formatPrice(record.getBidAmount());
                    String icon = "🔨";
                    String valueColor = "#0284c7";
                    String iconBgColor = "#e0f2fe";

                    if (controller != null) {
                        controller.setData(icon, title, subtitle, valueStr, valueColor, iconBgColor);
                    }
                    vboxAuctionHistoryList.getChildren().add(itemNode);
                } catch (Exception e) {
                    logger.error("Lỗi khi hiển thị lịch sử đấu giá: {}", e.getMessage());
                }
            }
        }));
    }
}