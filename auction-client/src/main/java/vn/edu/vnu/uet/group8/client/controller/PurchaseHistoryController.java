package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;

public class PurchaseHistoryController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseHistoryController.class);

    @FXML private VBox vboxPurchaseHistory;

    @FXML
    public void initialize() {
        UserService.loadPurchaseHistory(items -> Platform.runLater(() -> {
            vboxPurchaseHistory.getChildren().clear();
            
            if (items == null || items.isEmpty()) {
                vboxPurchaseHistory.getChildren().add(new Label("Bạn chưa có sản phẩm trúng thầu nào."));
                return;
            }

            for (AuctionItemDTO item : items) {
                try {
                    URL fxmlUrl = getClass().getResource("/fxml/GenericListItem.fxml");
                    FXMLLoader loader = new FXMLLoader(fxmlUrl);
                    Parent row = loader.load();
                    GenericListItemController controller = loader.getController();

                    String dateStr = item.getEndTime() != null ? UIFormatter.formatInstant(item.getEndTime()) : "Không xác định";
                    controller.setData("🎉", item.getTitle(), "Kết thúc: " + dateStr, UIFormatter.formatPrice(item.getCurrentPrice()), "#16a34a", "#f0fdf4");

                    // Khi click vào đồ là hiện ra trang thông tin của nó thật
                    row.setOnMouseClicked(e -> {
                        // Đóng pop-up hiện tại
                        Stage stage = (Stage) vboxPurchaseHistory.getScene().getWindow();
                        if (stage != null) stage.close();
                        
                        // Chuyển sang form đánh giá
                        vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().setCurrentAuctionItem(item);
                        Platform.runLater(() -> {
                            vn.edu.vnu.uet.group8.client.util.ModalUtil.showModal("ĐÁNH GIÁ NGƯỜI BÁN", "ReviewView.fxml");
                        });
                    });

                    vboxPurchaseHistory.getChildren().add(row);
                } catch (IOException e) {
                    log.error("Lỗi khi hiển thị lịch sử mua hàng: {}", e.getMessage());
                }
            }
        }));
    }
}