package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.common.entity.Item;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * ExploreController — wire AuctionService.loadAll() thật.
 *
 * Flow:
 *   1. initialize() → loadProducts()
 *   2. AuctionService.loadAll() gửi server, callback chạy khi xong
 *   3. Server response → AuctionService set vào ClientModel.getAuctionItems()
 *   4. Callback lấy data từ ClientModel → renderProducts()
 *
 * Tích hợp:
 *   #4 Kiểm định: showCertifiedBadge() trên card khi item.isVerified()
 *   #3 Đối tác: showPartnerBadge() (TODO khi BE có SellerStatsDTO)
 */
public class ExploreController implements Initializable {

    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbPrice;
    @FXML private ComboBox<String> cbSort;
    @FXML private FlowPane productContainer;
    @FXML private Label lblResultCount;
    @FXML private Button btnTagOpen;

    private Button activeTag;
    private List<Item> allItems = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComboBoxes();
        activeTag = btnTagOpen;
        loadProducts();
    }

    private void initComboBoxes() {
        cbCategory.getItems().addAll("Tất cả danh mục", "Đồng hồ cao cấp",
                "Điện tử", "Trang sức", "Nghệ thuật", "Xe cổ");
        cbCategory.getSelectionModel().selectFirst();

        cbPrice.getItems().addAll("Tất cả mức giá", "Dưới 10 triệu",
                "10 - 100 triệu", "100 triệu - 1 tỷ", "Trên 1 tỷ");
        cbPrice.getSelectionModel().selectFirst();

        cbSort.getItems().addAll("Sắp kết thúc", "Mới nhất",
                "Giá thấp → cao", "Giá cao → thấp");
        cbSort.getSelectionModel().selectFirst();

        cbCategory.setOnAction(e -> renderProducts());
        cbPrice.setOnAction(e -> renderProducts());
        cbSort.setOnAction(e -> renderProducts());
    }

    // ===== LOAD DATA THẬT TỪ SERVER =====

    /**
     * Gọi AuctionService.loadAll() bất đồng bộ.
     * Callback chạy trên FX Thread sau khi server response.
     */
    private void loadProducts() {
        lblResultCount.setText("Đang tải...");
        AuctionService.loadAll(null, () -> {
            // Sau callback, AuctionService đã set data vào ClientModel
            allItems = new ArrayList<>(ClientModel.getInstance().getAuctionItems());
            renderProducts();
        });
    }

    /**
     * Render allItems lên FlowPane, áp dụng filter + sort.
     */
    private void renderProducts() {
        productContainer.getChildren().clear();

        if (allItems == null || allItems.isEmpty()) {
            lblResultCount.setText("Chưa có sản phẩm nào");
            return;
        }

        // Filter
        String cat = cbCategory.getValue();
        String priceFilter = cbPrice.getValue();
        List<Item> filtered = allItems.stream()
                .filter(it -> matchCategory(it, cat))
                .filter(it -> matchPrice(it, priceFilter))
                .toList();

        // Sort
        List<Item> sorted = new ArrayList<>(filtered);
        sorted.sort(buildComparator(cbSort.getValue()));

        // Render
        for (Item item : sorted) {
            Node card = buildProductCard(item);
            if (card != null) productContainer.getChildren().add(card);
        }

        lblResultCount.setText("Hiển thị " + sorted.size() + " kết quả");
    }

    private boolean matchCategory(Item item, String filter) {
        if (filter == null || filter.startsWith("Tất cả")) return true;
        return filter.equalsIgnoreCase(item.getCategory());
    }

    private boolean matchPrice(Item item, String filter) {
        if (filter == null || filter.startsWith("Tất cả")) return true;
        BigDecimal price = item.getCurrentPrice();
        if (price == null) return true;
        BigDecimal tenM = new BigDecimal("10000000");
        BigDecimal hundredM = new BigDecimal("100000000");
        BigDecimal billion = new BigDecimal("1000000000");
        return switch (filter) {
            case "Dưới 10 triệu"     -> price.compareTo(tenM) < 0;
            case "10 - 100 triệu"    -> price.compareTo(tenM) >= 0 && price.compareTo(hundredM) < 0;
            case "100 triệu - 1 tỷ"  -> price.compareTo(hundredM) >= 0 && price.compareTo(billion) < 0;
            case "Trên 1 tỷ"         -> price.compareTo(billion) >= 0;
            default -> true;
        };
    }

    private Comparator<Item> buildComparator(String sort) {
        return switch (sort != null ? sort : "") {
            case "Giá thấp → cao" -> Comparator.comparing(Item::getCurrentPrice);
            case "Giá cao → thấp" -> Comparator.comparing(Item::getCurrentPrice).reversed();
            case "Mới nhất"       -> Comparator.comparing(Item::getCreatedAt).reversed();
            default               -> Comparator.comparing(Item::getEndTime);
        };
    }

    private Node buildProductCard(Item item) {
        try {
            URL resource = getClass().getResource("/fxml/ProductCard.fxml");
            if (resource == null) return null;

            FXMLLoader loader = new FXMLLoader(resource);
            Node card = loader.load();
            ProductCardController ctrl = loader.getController();

            // Set data cơ bản
            ctrl.setItem(String.valueOf(item.getId()), item.getName(),
                    item.getCurrentPrice(), item.getImageUrl());

            // Tính năng #4 — Kiểm định
            if (item.isVerified()) {
                ctrl.showCertifiedBadge();
            }

            // Tính năng #3 — Đối tác (khi BE có SellerStatsDTO)
            // ReviewService.loadSellerStats(item.getSellerId(), stats -> {
            //     if (stats != null && stats.isPartner()) {
            //         ctrl.showPartnerBadge(stats.getPartnerBadgeText());
            //     }
            // });

            return card;
        } catch (IOException e) {
            System.err.println("[Explore] Lỗi load card: " + e.getMessage());
            return null;
        }
    }

    // ===== TAG FILTER =====

    @FXML
    private void onTagClick(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        setActiveTag(clicked);
        renderProducts();
    }

    private void setActiveTag(Button target) {
        if (activeTag != null) {
            activeTag.getStyleClass().remove("tag-active");
            activeTag.getStyleClass().add("tag-inactive");
        }
        target.getStyleClass().remove("tag-inactive");
        target.getStyleClass().add("tag-active");
        activeTag = target;
    }

    @FXML
    private void onAdvancedFilter() {
        // TODO: mở dialog filter nâng cao
    }
}