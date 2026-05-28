package vn.edu.vnu.uet.group8.client.controller;

import java.math.BigDecimal;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;

public class SalesManagementController {

    private static final Logger log = LoggerFactory.getLogger(SalesManagementController.class);

    @FXML private Label lblMiniRevenue;
    @FXML private VBox vboxInventoryList;

    @FXML
    public void initialize() {
        SellerService.getMyListings(items -> Platform.runLater(() -> {
            vboxInventoryList.getChildren().clear();
            BigDecimal totalRevenue = BigDecimal.ZERO;
            
            if (items == null || items.isEmpty()) {
                Label emptyLabel = new Label("Chưa có sản phẩm nào trong kho.");
                emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 10;");
                vboxInventoryList.getChildren().add(emptyLabel);
                return;
            }

            for (AuctionItemDTO item : items) {
                // Tính toán doanh thu (lọc những sản phẩm đã SOLD và có giá trị thanh toán)
                if ("SOLD".equals(item.getStatus().name()) && item.getCurrentPrice() != null) {
                    totalRevenue = totalRevenue.add(item.getCurrentPrice());
                }

                try {
                    URL url = getClass().getResource("/fxml/GenericListItem.fxml");
                    if (url == null) continue;
                    
                    FXMLLoader loader = new FXMLLoader(url);
                    HBox itemNode = loader.load();
                    GenericListItemController controller = loader.getController();

                    String title = item.getTitle() != null ? item.getTitle() : "Sản phẩm không xác định";
                    String subtitle = "Mã kho: #ITM-" + item.getItemId() + " • " + item.getCondition().name();
                    String valueStr = item.getStatus().name();
                    String icon = "📦";
                    String valueColor = "#64748b";
                    String iconBgColor = "#f1f5f9";

                    if ("LISTED".equals(item.getStatus().name())) {
                        valueColor = "#0284c7";
                        iconBgColor = "#e0f2fe";
                    } else if ("SOLD".equals(item.getStatus().name())) {
                        valueColor = "#16a34a";
                        iconBgColor = "#dcfce3";
                        icon = "🎉";
                    }

                    if (controller != null) {
                        controller.setData(icon, title, subtitle, valueStr, valueColor, iconBgColor);
                    }
                    vboxInventoryList.getChildren().add(itemNode);
                } catch (Exception e) {
                    log.error("Lỗi khi hiển thị sản phẩm trong kho: {}", e.getMessage());
                }
            }
            lblMiniRevenue.setText(UIFormatter.formatPrice(totalRevenue));
        }), err -> Platform.runLater(() -> {
            vboxInventoryList.getChildren().clear();
            Label errLabel = new Label("Lỗi lấy dữ liệu: " + err);
            errLabel.setStyle("-fx-text-fill: #ef4444; -fx-padding: 10;");
            vboxInventoryList.getChildren().add(errLabel);
        }));
    }
}
