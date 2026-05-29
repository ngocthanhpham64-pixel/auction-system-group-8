package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

public class ExploreController {

    private static final Logger log = LoggerFactory.getLogger(ExploreController.class);

    /** Số lượng item tối đa hiển thị trên mỗi trang */
    private static final int PAGE_SIZE = 20;

    @FXML
    private TilePane productContainer;
    @FXML
    private Label lblResultCount;

    @FXML
    private HBox hboxTags;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private ComboBox<String> cbCategory;

    // Pagination controls
    @FXML
    private Button btnPrevPage;
    @FXML
    private Button btnNextPage;
    @FXML
    private Label lblCurrentPage;
    @FXML
    private Label lblTotalPages;

    // State: danh sách đã lọc + sắp xếp, trang hiện tại
    private List<AuctionItemDTO> filteredItems = new ArrayList<>();
    private int currentPage = 1;

    // Listeners để tránh memory leak
    private InvalidationListener itemsListener;
    private ChangeListener<String> searchListener;

    @FXML
    public void initialize() {
        // Init categories
        cbCategory.getItems().addAll(
                "Tất cả danh mục",
                "Đồng hồ cao cấp",
                "Điện tử",
                "Trang sức",
                "Nghệ thuật",
                "Xe cộ",
                "Sách quý",
                "Đồ cổ",
                "Thời trang",
                "Bất động sản",
                "Nhà cửa",
                "Thể thao",
                "Khác");
        cbCategory.getSelectionModel().selectFirst();
        cbCategory.setOnAction(e -> {
            String selected = cbCategory.getValue();
            String catValue = getCategoryValueFromName(selected);
            updateActiveTag(catValue);
            if ("ALL".equals(catValue)) {
                ClientModel.getInstance().setSearchQuery("");
            } else {
                ClientModel.getInstance().setSearchQuery("CATEGORY:" + catValue);
            }
            refreshProducts();
        });

        // Init sorting
        cbSort.getItems().addAll("Mới nhất", "Giá thấp đến cao", "Giá cao đến thấp", "Sắp kết thúc");
        cbSort.getSelectionModel().selectFirst();
        cbSort.setOnAction(e -> refreshProducts());

        // Select tag based on current searchQuery if it's already set
        String query = ClientModel.getInstance().getSearchQuery();
        if (query != null && query.startsWith("CATEGORY:")) {
            String catValue = query.substring(9);
            updateActiveTag(catValue);
            cbCategory.setValue(getCategoryNameFromValue(catValue));
        }

        // Listen to changes in the global auction items list so we update the UI in real-time when the background timeline updates the items
        itemsListener = obs -> {
            Platform.runLater(this::filterAndRenderProducts);
        };
        ClientModel.getInstance().getAuctionItems().addListener(new WeakInvalidationListener(itemsListener));

        // Listen to search query changes (e.g. from search bar in MainLayout)
        searchListener = (obs, oldVal, newVal) -> {
            Platform.runLater(this::filterAndRenderProducts);
        };
        ClientModel.getInstance().searchQueryProperty().addListener(new WeakChangeListener<>(searchListener));

        refreshProducts();
    }

    private String getCategoryValueFromName(String name) {
        if (name == null)
            return "ALL";
        return switch (name) {
            case "Đồng hồ cao cấp" -> "WATCHES";
            case "Điện tử" -> "ELECTRONICS";
            case "Trang sức" -> "JEWELRY";
            case "Nghệ thuật" -> "ART";
            case "Xe cộ" -> "VEHICLES";
            case "Sách quý" -> "BOOKS";
            case "Đồ cổ" -> "ANTIQUES";
            case "Thời trang" -> "FASHION";
            case "Bất động sản" -> "REAL_ESTATE";
            case "Nhà cửa" -> "HOME";
            case "Thể thao" -> "SPORTS";
            case "Khác" -> "OTHER";
            default -> "ALL";
        };
    }

    private String getCategoryNameFromValue(String value) {
        if (value == null)
            return "Tất cả danh mục";
        return switch (value) {
            case "WATCHES" -> "Đồng hồ cao cấp";
            case "ELECTRONICS" -> "Điện tử";
            case "JEWELRY" -> "Trang sức";
            case "ART" -> "Nghệ thuật";
            case "VEHICLES" -> "Xe cộ";
            case "BOOKS" -> "Sách quý";
            case "ANTIQUES" -> "Đồ cổ";
            case "FASHION" -> "Thời trang";
            case "REAL_ESTATE" -> "Bất động sản";
            case "HOME" -> "Nhà cửa";
            case "SPORTS" -> "Thể thao";
            case "OTHER" -> "Khác";
            default -> "Tất cả danh mục";
        };
    }

    private void updateActiveTag(String categoryValue) {
        if (hboxTags == null)
            return;
        for (Node node : hboxTags.getChildren()) {
            if (node instanceof Button btn) {
                String userData = (String) btn.getUserData();
                if (userData != null && userData.equals(categoryValue)) {
                    btn.getStyleClass().remove("tag-pill");
                    if (!btn.getStyleClass().contains("tag-pill-active")) {
                        btn.getStyleClass().add("tag-pill-active");
                    }
                } else {
                    btn.getStyleClass().remove("tag-pill-active");
                    if (!btn.getStyleClass().contains("tag-pill")) {
                        btn.getStyleClass().add("tag-pill");
                    }
                }
            }
        }
    }

    private void refreshProducts() {
        AuctionService.loadAll(null, () -> Platform.runLater(this::filterAndRenderProducts));
    }

    private void filterAndRenderProducts() {
        List<AuctionItemDTO> items = new ArrayList<>(ClientModel.getInstance().getAuctionItems());
        String query = ClientModel.getInstance().getSearchQuery();

        // Filter by category or keyword
        if (query != null && !query.trim().isEmpty()) {
            if (query.startsWith("CATEGORY:")) {
                String catStr = query.substring(9);
                items = items.stream()
                        .filter(i -> i.getCategory() != null && i.getCategory().name().equalsIgnoreCase(catStr))
                        .collect(Collectors.toList());
            } else {
                items = items.stream()
                        .filter(i -> i.getTitle() != null
                                && i.getTitle().toLowerCase().contains(query.toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        // Apply sorting
        String sort = cbSort.getValue();
        if ("Giá thấp đến cao".equals(sort)) {
            items.sort((a, b) -> {
                BigDecimal p1 = a.getCurrentPrice();
                BigDecimal p2 = b.getCurrentPrice();
                if (p1 == null && p2 == null)
                    return 0;
                if (p1 == null)
                    return 1;
                if (p2 == null)
                    return -1;
                return p1.compareTo(p2);
            });
        } else if ("Giá cao đến thấp".equals(sort)) {
            items.sort((a, b) -> {
                BigDecimal p1 = a.getCurrentPrice();
                BigDecimal p2 = b.getCurrentPrice();
                if (p1 == null && p2 == null)
                    return 0;
                if (p1 == null)
                    return 1;
                if (p2 == null)
                    return -1;
                return p2.compareTo(p1);
            });
        } else if ("Sắp kết thúc".equals(sort)) {
            items.sort(Comparator.comparing(AuctionItemDTO::getEndTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        } else { // "Mới nhất" (default)
            items.sort(Comparator.comparing(AuctionItemDTO::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        }

        // Lưu lại danh sách đã lọc và về trang 1 mỗi khi filter/sort thay đổi
        filteredItems = items;
        currentPage = 1;
        renderCurrentPage();
    }

    /**
     * Render đúng slice của trang hiện tại (tối đa PAGE_SIZE items) lên TilePane
     * và cập nhật toàn bộ trạng thái UI phân trang.
     */
    private void renderCurrentPage() {
        int totalItems = filteredItems.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));

        // Clamp currentPage
        if (currentPage < 1)
            currentPage = 1;
        if (currentPage > totalPages)
            currentPage = totalPages;

        // Tính vị trí slice
        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
        List<AuctionItemDTO> pageItems = (totalItems == 0)
                ? new ArrayList<>()
                : filteredItems.subList(fromIndex, toIndex);

        // Render cards
        productContainer.getChildren().clear();
        for (AuctionItemDTO item : pageItems) {
            createProductCard(item);
        }

        // Cập nhật label kết quả
        if (totalItems == 0) {
            lblResultCount.setText("Không có phiên đấu giá nào");
        } else {
            lblResultCount.setText(totalItems + " phiên đấu giá — hiển thị " + (fromIndex + 1) + "–" + toIndex);
        }

        // Cập nhật pagination labels
        if (lblCurrentPage != null)
            lblCurrentPage.setText(String.valueOf(currentPage));
        if (lblTotalPages != null)
            lblTotalPages.setText(String.valueOf(totalPages));

        // Bật/tắt nút điều hướng
        if (btnPrevPage != null)
            btnPrevPage.setDisable(currentPage <= 1);
        if (btnNextPage != null)
            btnNextPage.setDisable(currentPage >= totalPages);
    }

    private void createProductCard(AuctionItemDTO item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductCard.fxml"));
            Node card = loader.load();

            ProductCardController controller = loader.getController();
            controller.setData(item);

            productContainer.getChildren().add(card);
        } catch (IOException e) {
            log.error("Lỗi khi tạo card cho item {}: {}", item.getItemId(), e.getMessage());
        }
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderCurrentPage();
        }
    }

    @FXML
    private void onNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredItems.size() / PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            renderCurrentPage();
        }
    }

    @FXML
    private void onTagClick(ActionEvent event) {
        Node source = (Node) event.getSource();
        String category = (String) source.getUserData();
        updateActiveTag(category);
        cbCategory.setValue(getCategoryNameFromValue(category));

        if ("ALL".equals(category) || category == null) {
            ClientModel.getInstance().setSearchQuery("");
        } else {
            ClientModel.getInstance().setSearchQuery("CATEGORY:" + category);
        }
        refreshProducts();
    }
}