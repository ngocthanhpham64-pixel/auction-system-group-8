package vn.edu.vnu.uet.group8.client.controller;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;

public class TransactionHistoryController {

    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryController.class);

    @FXML private ScrollPane scrollPaneList;
    @FXML private VBox vboxContainer;
    @FXML private VBox vboxEmptyState;

    @FXML
    public void initialize() {
        // Tăng tốc độ cuộn chuột lên 1.5 lần
        if (scrollPaneList != null) {
            scrollPaneList.getContent().setOnScroll(event -> {
                double speedMultiplier = 1.5;
                double deltaY = event.getDeltaY() * speedMultiplier;
                scrollPaneList.setVvalue(scrollPaneList.getVvalue() - deltaY / scrollPaneList.getHeight());
                event.consume(); // Ngăn sự kiện cuộn mặc định
            });
        }
        UserService.loadTransactions(transactions -> Platform.runLater(() -> {
            vboxContainer.getChildren().clear();
            
            // Xử lý khi người dùng chưa có giao dịch nào
            if (transactions == null || transactions.isEmpty()) {
                scrollPaneList.setVisible(false);
                scrollPaneList.setManaged(false);
                vboxEmptyState.setVisible(true);
                vboxEmptyState.setManaged(true);
                return;
            }

            // Hiển thị danh sách nếu có
            scrollPaneList.setVisible(true);
            scrollPaneList.setManaged(true);
            vboxEmptyState.setVisible(false);
            vboxEmptyState.setManaged(false);

            for (TransactionHistoryEntry tx : transactions) {
                try {
                    URL url = getClass().getResource("/fxml/GenericListItem.fxml");
                    if (url == null) continue;
                    
                    FXMLLoader loader = new FXMLLoader(url);
                    HBox itemNode = loader.load();
                    GenericListItemController controller = loader.getController();

                    // Trích xuất dữ liệu từ DTO
                    String typeStr = String.valueOf(tx.type());
                    String title = tx.description() != null && !tx.description().isBlank() ? tx.description() : "Giao dịch hệ thống";
                    
                    // Rút gọn ID Giao dịch (Lấy đoạn đầu trước dấu gạch ngang)
                    String shortId = tx.transactionId() != null && tx.transactionId().contains("-") ? tx.transactionId().split("-")[0].toUpperCase() : tx.transactionId();
                    
                    String subtitle = "Mã GD: #" + shortId + " • " + UIFormatter.formatInstant(tx.createdAt());
                    
                    String valueStr = UIFormatter.formatPrice(tx.amount());
                    String icon = "💸";
                    String valueColor = "#475569";
                    String iconBgColor = "#f8fafc";

                    // Xử lý UI và màu sắc theo loại GD
                    if (typeStr.contains("DEPOSIT") || typeStr.contains("REFUND") || typeStr.contains("REWARD")) {
                        valueStr = "+" + valueStr;
                        valueColor = "#10b981"; // Xanh lá
                        icon = "📥";
                        iconBgColor = "#f0fdf4";
                    } else if (typeStr.contains("WITHDRAW") || typeStr.contains("HOLD") || typeStr.contains("FEE") || typeStr.contains("PAYMENT")) {
                        valueStr = "-" + valueStr;
                        valueColor = "#ef4444"; // Đỏ
                        icon = "📤";
                        iconBgColor = "#fef2f2";
                    } else if (typeStr.contains("BID")) {
                        icon = "🔨";
                    }

                    if (controller != null) {
                        controller.setData(icon, title, subtitle, valueStr, valueColor, iconBgColor);
                    }
                    
                    // Thêm sự kiện click để hiện popup đầy đủ thông tin
                    itemNode.setOnMouseClicked(e -> {
                        String details = String.format("Mã Giao Dịch: %s\nLoại: %s\nSố tiền: %s\nThời gian: %s\nNội dung: %s",
                            tx.transactionId(), tx.type(), UIFormatter.formatPrice(tx.amount()), 
                            UIFormatter.formatInstant(tx.createdAt()), tx.description());
                        AlertUtil.showInfo(details);
                    });
                    vboxContainer.getChildren().add(itemNode);
                } catch (Exception e) {
                    log.error("Lỗi khi hiển thị lịch sử giao dịch: {}", e.getMessage());
                }
            }
        }));
    }
}