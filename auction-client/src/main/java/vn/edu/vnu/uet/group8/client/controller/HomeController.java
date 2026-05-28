package vn.edu.vnu.uet.group8.client.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @FXML private Label lblHeroName;
    @FXML private Label lblHeroPrice;
    @FXML private Label lblHeroTimer;
    @FXML private javafx.scene.layout.VBox categoryContainer;

    private AuctionItemDTO heroItem;

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        AuctionService.loadAll(null, () -> Platform.runLater(() -> {
            List<AuctionItemDTO> items = ClientModel.getInstance().getAuctionItems();

            if (items != null && !items.isEmpty()) {
                // Chọn hero: ưu tiên item ACTIVE gần hết giờ nhất
                heroItem = items.stream()
                        .filter(it -> it.getStatus() == SessionStatus.ACTIVE && it.getEndTime() != null)
                        .min(Comparator.comparing(AuctionItemDTO::getEndTime))
                        .orElse(items.get(0));

                lblHeroName.setText(heroItem.getTitle());
                if (heroItem.getCurrentPrice() != null) {
                    lblHeroPrice.setText(String.format("%,.0f VNĐ", heroItem.getCurrentPrice()));
                }
                lblHeroTimer.setText(formatTimer(heroItem.getEndTime()));

                // Populate categories
                categoryContainer.getChildren().clear();
                buildCategorySection("⌚ Đồng hồ cao cấp", "WATCHES", items);
                buildCategorySection("💻 Đồ điện tử", "ELECTRONICS", items);
                buildCategorySection("👗 Thời trang", "FASHION", items);
                buildCategorySection("🎨 Nghệ thuật", "ART", items);
            }
        }));
    }

    /** Định dạng thời gian còn lại từ Instant endTime */
    private String formatTimer(Instant endTime) {
        if (endTime == null) return "⏳ Đang diễn ra";
        Duration remaining = Duration.between(Instant.now(), endTime);
        if (remaining.isNegative()) return "⏳ Đã kết thúc";
        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();
        long seconds = remaining.toSecondsPart();
        if (hours > 0) {
            return String.format("⏳ Còn lại: %dh %02dm", hours, minutes);
        } else {
            return String.format("⏳ Còn lại: %02d:%02d", minutes, seconds);
        }
    }

    private void buildCategorySection(String title, String categoryEnum, List<AuctionItemDTO> allItems) {
        List<AuctionItemDTO> categoryItems = allItems.stream()
                .filter(it -> it.getCategory() != null && it.getCategory().name().equals(categoryEnum))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());

        if (categoryItems.isEmpty()) return;

        javafx.scene.layout.VBox section = new javafx.scene.layout.VBox(15);
        section.getStyleClass().add("home-category-container");
        section.setPadding(new javafx.geometry.Insets(20));

        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        shadow.setOffsetY(8.0);
        shadow.setRadius(16.0);
        shadow.setColor(javafx.scene.paint.Color.color(0.0, 0.0, 0.0, 0.15));
        section.setEffect(shadow);
        
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        titleLabel.getStyleClass().add("text-heading");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        javafx.scene.control.Button btnViewAll = new javafx.scene.control.Button("Xem tất cả →");
        btnViewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #ea580c; -fx-font-weight: bold; -fx-cursor: hand;");
        btnViewAll.setOnAction(e -> {
            ClientModel.getInstance().setSearchQuery("CATEGORY:" + categoryEnum);
            if (MainController.getInstance() != null) {
                MainController.getInstance().loadContentView("ExploreView.fxml");
            }
        });
        
        header.getChildren().addAll(titleLabel, spacer, btnViewAll);
        
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane();
        scroll.setFitToHeight(true);
        scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scroll.setPannable(true);
        scroll.setPrefHeight(380.0);
        scroll.setMinHeight(380.0);
        
        javafx.scene.layout.HBox hBox = new javafx.scene.layout.HBox(24);
        hBox.setStyle("-fx-padding: 10 5 20 5; -fx-background-color: transparent;");
        
        for (AuctionItemDTO item : categoryItems) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductCard.fxml"));
                Node cardNode = loader.load();
                ProductCardController controller = loader.getController();
                controller.setData(item);
                hBox.getChildren().add(cardNode);
            } catch (Exception ex) {
                log.error("Lỗi khi tạo card cho item {}: {}", item.getItemId(), ex.getMessage());
            }
        }
        
        scroll.setContent(hBox);
        section.getChildren().addAll(header, scroll);
        
        categoryContainer.getChildren().add(section);
    }

    @FXML
    public void onCategoryClick(ActionEvent event) {
        Node source = (Node) event.getSource();
        String category = (String) source.getUserData();

        // Lưu category vào ClientModel để ExploreController lấy làm bộ lọc
        ClientModel.getInstance().setSearchQuery("CATEGORY:" + category);

        // Chuyển sang ExploreView
        if (MainController.getInstance() != null) {
            MainController.getInstance().loadContentView("ExploreView.fxml");
        }
    }

    @FXML
    public void onHeroJoinClick(ActionEvent event) {
        if (heroItem != null) {
            if (heroItem.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.ENDED_NO_BID || 
                heroItem.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.SOLD ||
                heroItem.getStatus() == vn.edu.vnu.uet.group8.common.enums.SessionStatus.CANCELLED) {
                vn.edu.vnu.uet.group8.client.util.AlertUtil.showError("Phiên đấu giá đã kết thúc. Bạn không thể xem chi tiết.");
                return;
            }

            ClientModel.getInstance().setCurrentAuctionItem(heroItem);
            if (MainController.getInstance() != null) {
                MainController.getInstance().loadContentView("AuctionDetailView.fxml");
            } else {
                SceneManager.switchTo(SceneManager.VIEW_AUCTION_DETAIL);
            }
        }
    }
}
