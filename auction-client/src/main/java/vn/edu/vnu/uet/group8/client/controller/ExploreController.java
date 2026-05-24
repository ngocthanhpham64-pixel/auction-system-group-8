package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;

/**
 * ExploreController — trang chinh hien danh sach phien dau gia.
 * FIX: Đổi FlowPane thành TilePane để khớp với FXML.
 */
public class ExploreController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ExploreController.class.getName());

    @FXML ComboBox<String> cbCategory;
    @FXML ComboBox<String> cbPrice;
    @FXML ComboBox<String> cbSort;
    
    // FIX BUG: Kiểu dữ liệu phải khớp với FXML (TilePane)
    @FXML TilePane productContainer; 
    
    @FXML Label lblResultCount;
    @FXML Button btnTagOpen;

    private Button activeTag;
    private String currentTagFilter = "ALL";
    private final List<AuctionItemDTO> allItems = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComboBoxes();
        activeTag = btnTagOpen;
        bindAuctionItems();
        
        // Sync ban đầu
        allItems.addAll(ClientModel.getInstance().getAuctionItems());
        renderProducts();
        loadProducts();

        ClientModel.getInstance().searchQueryProperty().addListener((obs, oldV, newV) -> {
            Platform.runLater(this::renderProducts);
        });
    }

    void initComboBoxes() {
        if (cbCategory != null) {
            cbCategory.getItems().setAll("Tất cả danh mục",
                    ItemCategory.WATCHES.getLabel(), ItemCategory.ELECTRONICS.getLabel(), 
                    ItemCategory.JEWELRY.getLabel(), ItemCategory.ART.getLabel(), 
                    ItemCategory.VEHICLES.getLabel(), ItemCategory.ANTIQUES.getLabel(),
                    ItemCategory.BOOKS.getLabel(), ItemCategory.FASHION.getLabel(), 
                    ItemCategory.OTHER.getLabel());
            cbCategory.getSelectionModel().selectFirst();
            cbCategory.setOnAction(e -> renderProducts());
        }

        if (cbPrice != null) {
            cbPrice.getItems().setAll("Tất cả mức giá", "Dưới 10 triệu", "10 - 100 triệu", "100 triệu - 1 tỷ", "Trên 1 tỷ");
            cbPrice.getSelectionModel().selectFirst();
            cbPrice.setOnAction(e -> renderProducts());
        }

        if (cbSort != null) {
            cbSort.getItems().setAll("Sắp kết thúc", "Mới nhất", "Giá thấp -> cao", "Giá cao -> thap");
            cbSort.getSelectionModel().selectFirst();
            cbSort.setOnAction(e -> renderProducts());
        }
    }

    void bindAuctionItems() {
        ClientModel.getInstance().auctionItemsProperty().addListener((obs, oldList, newList) ->
                Platform.runLater(() -> {
                    allItems.clear();
                    if (newList != null) allItems.addAll(newList);
                    renderProducts();
                })
        );
    }

    void loadProducts() {
        if (lblResultCount != null) lblResultCount.setText("Đang tải dữ liệu...");
        AuctionService.loadAll(null, () -> {
            LOGGER.info("Data loaded from server.");
        });
    }

    void renderProducts() {
        if (productContainer == null) return;
        productContainer.getChildren().clear();

        List<AuctionItemDTO> filtered = applyFilters(allItems);
        
        if (filtered.isEmpty()) {
            renderEmptyState("Không tìm thấy sản phẩm nào");
            return;
        }

        List<AuctionItemDTO> sorted = applySorting(filtered);
        for (AuctionItemDTO item : sorted) {
            Node card = buildProductCard(item);
            if (card != null) productContainer.getChildren().add(card);
        }

        if (lblResultCount != null) {
            lblResultCount.setText("Hiển thị " + sorted.size() + " kết quả");
        }
    }

    void renderEmptyState(String message) {
        Label empty = new Label(message);
        empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-padding: 50 0;");
        productContainer.getChildren().add(empty);
        if (lblResultCount != null) lblResultCount.setText("0 kết quả");
    }

    private List<AuctionItemDTO> applyFilters(List<AuctionItemDTO> items) {
        String cat = (cbCategory != null) ? cbCategory.getValue() : "Tất cả danh mục";
        String keyword = ClientModel.getInstance().getSearchQuery().toLowerCase();

        return items.stream()
                .filter(it -> matchCategory(it, cat))
                .filter(it -> matchKeyword(it, keyword))
                .filter(this::matchTagFilter)
                .toList();
    }

    private boolean matchKeyword(AuctionItemDTO item, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true;
        return item.getTitle() != null && item.getTitle().toLowerCase().contains(keyword);
    }

    private boolean matchCategory(AuctionItemDTO item, String filterLabel) {
        if (filterLabel == null || filterLabel.startsWith("Tất cả")) return true;
        if (item.getCategory() == null) return false;
        return item.getCategory().getLabel().equalsIgnoreCase(filterLabel);
    }

    private boolean matchTagFilter(AuctionItemDTO item) {
        if (currentTagFilter == null || currentTagFilter.equals("ALL")) return true;
        if (item.getCategory() == null) return false;
        return item.getCategory().name().equalsIgnoreCase(currentTagFilter);
    }

    private List<AuctionItemDTO> applySorting(List<AuctionItemDTO> items) {
        String sortMode = (cbSort != null) ? cbSort.getValue() : "Mới nhất";
        Comparator<AuctionItemDTO> comparator = switch (sortMode) {
            case "Giá thấp -> cao" -> Comparator.comparing(AuctionItemDTO::getCurrentPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Giá cao -> thap" -> Comparator.comparing(AuctionItemDTO::getCurrentPrice, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            default -> Comparator.comparing(AuctionItemDTO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        };
        return items.stream().sorted(comparator).toList();
    }

    private Node buildProductCard(AuctionItemDTO item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductCard.fxml"));
            Node card = loader.load();
            ProductCardController ctrl = loader.getController();

            // Chuyển đổi Instant sang LocalDateTime để đồng hồ chạy
            LocalDateTime endTime = null;
            if (item.getEndTime() != null) {
                endTime = LocalDateTime.ofInstant(item.getEndTime(), ZoneId.systemDefault());
            }

            // Bind data thực tế từ DTO
            ctrl.setItem(
                String.valueOf(item.getItemId()),
                item.getTitle(),
                item.getCurrentPrice(),
                (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) ? item.getImageUrls().get(0) : "",
                endTime
            );
            
            ctrl.setBidCount(item.getBidCount());

            return card;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Loi load ProductCard cho item: " + item.getItemId(), e);
            return null;
        }
    }

    @FXML
    void onTagClick(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof Button btn)) return;
        
        String tag = (String) btn.getUserData();
        this.currentTagFilter = (tag != null) ? tag : "ALL";

        setActiveTag(btn);
        renderProducts();
    }

    void setActiveTag(Button target) {
        if (activeTag != null) {
            activeTag.setStyle("-fx-background-color: white; -fx-text-fill: #4b5563; -fx-border-color: #e5e7eb;");
        }
        target.setStyle("-fx-background-color: #1a2744; -fx-text-fill: white; -fx-border-color: #1a2744;");
        activeTag = target;
    }
}
