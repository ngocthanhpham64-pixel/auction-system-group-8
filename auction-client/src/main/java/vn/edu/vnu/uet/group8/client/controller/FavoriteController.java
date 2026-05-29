package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.service.FavoriteService;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;

public class FavoriteController {
    private static final Logger log = LoggerFactory.getLogger(FavoriteController.class);
    @FXML private VBox vboxFavoriteContainer;
    @FXML private VBox vboxNoFavorites;

    @FXML
    public void initialize() {
        FavoriteService.loadAll(favorites -> Platform.runLater(() -> {
            vboxFavoriteContainer.getChildren().clear();
            if (favorites == null || favorites.isEmpty()) {
                vboxNoFavorites.setVisible(true);
                vboxNoFavorites.setManaged(true);
                return;
            }
            vboxNoFavorites.setVisible(false);
            vboxNoFavorites.setManaged(false);

            for (AuctionItemDTO item : favorites) {
                try {
                    URL url = getClass().getResource("/fxml/GenericListItem.fxml");
                    if (url == null) continue;
                    FXMLLoader loader = new FXMLLoader(url);
                    HBox itemNode = loader.load();
                    GenericListItemController controller = loader.getController();

                    String icon = "❤️";
                    String title = item.getTitle() != null ? item.getTitle() : "Sản phẩm";
                    String subtitle = "Mã: #ITM-" + item.getItemId() + " • Trạng thái: " + item.getStatus();
                    String valueStr = UIFormatter.formatPrice(item.getCurrentPrice());
                    String valueColor = "#0f172a";
                    String iconBgColor = "#ffe4e6";

                    controller.setData(icon, title, subtitle, valueStr, valueColor, iconBgColor);

                    // Bấm vào sản phẩm yêu thích để chuyển trang hoặc báo đã kết thúc
                    itemNode.setOnMouseClicked(e -> {
                        Window window = vboxFavoriteContainer.getScene().getWindow();
                        if (window != null) window.hide();
                        
                        AuctionService.loadDetail(item.getItemId(), detail -> Platform.runLater(() -> {
                            if (detail == null) {
                                vn.edu.vnu.uet.group8.client.util.AlertUtil.showError("Không tìm thấy sản phẩm hoặc lỗi kết nối!");
                                return;
                            }
                            if (detail.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.ACTIVE || 
                                detail.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.UPCOMING) {
                                if (MainController.getInstance() != null) {
                                    MainController.getInstance().loadContentView("AuctionDetailView.fxml");
                                }
                            } else {
                                vn.edu.vnu.uet.group8.client.util.AlertUtil.showWarning("Phiên đấu giá này đã kết thúc!");
                            }
                        }));
                    });

                    vboxFavoriteContainer.getChildren().add(itemNode);
                } catch (Exception e) {
                    log.error("Lỗi khi hiển thị sản phẩm yêu thích: {}", e.getMessage());
                }
            }
        }), err -> {});
    }
}