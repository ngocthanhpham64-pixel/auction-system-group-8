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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExploreController — trang chinh hien danh sach phien dau gia.
 *
 * Tinh nang:
 *  - Load items tu server qua AuctionService.loadAll()
 *  - Binding voi ClientModel.auctionItems -> tu refresh khi co data moi
 *  - Filter 3 chieu: category / price range / status tag
 *  - Sort 4 kieu: sap ket thuc, moi nhat, gia thap-cao, cao-thap
 *  - Loading indicator + empty state
 *  - Render lai khong xoa scroll position
 */
public class ExploreController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ExploreController.class.getName());

    // Price range constants (VND)
    private static final BigDecimal PRICE_10M  = new BigDecimal("10000000");
    private static final BigDecimal PRICE_100M = new BigDecimal("100000000");
    private static final BigDecimal PRICE_1B   = new BigDecimal("1000000000");

    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbPrice;
    @FXML private ComboBox<String> cbSort;
    @FXML private FlowPane productContainer;
    @FXML private Label lblResultCount;
    @FXML private Button btnTagOpen;

    private Button activeTag;
    private String currentTagFilter = "open";  // open / ending / hot / new
    private final List<Item> allItems = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComboBoxes();
        activeTag = btnTagOpen;
        bindAuctionItems();
        loadProducts();
    }

    private void initComboBoxes() {
        cbCategory.getItems().setAll("Tat ca danh muc",
                "Dong ho cao cap", "Dien tu", "Trang suc",
                "Nghe thuat", "Xe co", "Do co");
        cbCategory.getSelectionModel().selectFirst();

        cbPrice.getItems().setAll("Tat ca muc gia",
                "Duoi 10 trieu", "10 - 100 trieu",
                "100 trieu - 1 ty", "Tren 1 ty");
        cbPrice.getSelectionModel().selectFirst();

        cbSort.getItems().setAll("Sap ket thuc", "Moi nhat",
                "Gia thap -> cao", "Gia cao -> thap");
        cbSort.getSelectionModel().selectFirst();

        // Re-render khi user doi filter
        cbCategory.setOnAction(e -> renderProducts());
        cbPrice.setOnAction(e -> renderProducts());
        cbSort.setOnAction(e -> renderProducts());
    }

    /** Binding ClientModel -> tu re-render khi co items moi tu server. */
    private void bindAuctionItems() {
        ClientModel.getInstance().auctionItemsProperty().addListener((obs, oldList, newList) ->
                Platform.runLater(() -> {
                    allItems.clear();
                    if (newList != null) allItems.addAll(newList);
                    renderProducts();
                })
        );
    }

    // ===== LOAD DATA =====

    /**
     * Load items async, hien loading indicator.
     */
    private void loadProducts() {
        showLoading();
        AuctionService.loadAll(null, () -> {
            // Callback chay sau khi AuctionService set ClientModel.auctionItems
            // Listener da xu ly render -> chi can update count
            LOGGER.info(() -> "Loaded " + allItems.size() + " items");
        });
    }

    private void showLoading() {
        if (lblResultCount != null) lblResultCount.setText("Dang tai...");
    }

    // ===== RENDER =====

    /**
     * Render products theo filter + sort hien tai.
     * Empty state neu khong co item.
     */
    private void renderProducts() {
        if (productContainer == null) return;
        productContainer.getChildren().clear();

        if (allItems.isEmpty()) {
            renderEmptyState("Chua co san pham nao");
            return;
        }

        List<Item> filtered = applyFilters(allItems);
        if (filtered.isEmpty()) {
            renderEmptyState("Khong tim thay san pham phu hop");
            return;
        }

        List<Item> sorted = applySorting(filtered);
        for (Item item : sorted) {
            Node card = buildProductCard(item);
            if (card != null) productContainer.getChildren().add(card);
        }

        if (lblResultCount != null) {
            lblResultCount.setText("Hien thi " + sorted.size() + " ket qua");
        }
    }

    private void renderEmptyState(String message) {
        Label empty = new Label(message);
        empty.getStyleClass().add("label-info");
        empty.setStyle("-fx-padding: 40 0; -fx-font-size: 14px;");
        productContainer.getChildren().add(empty);
        if (lblResultCount != null) lblResultCount.setText("0 ket qua");
    }

    private List<Item> applyFilters(List<Item> items) {
        String cat = cbCategory.getValue();
        String price = cbPrice.getValue();
        return items.stream()
                .filter(it -> matchCategory(it, cat))
                .filter(it -> matchPrice(it, price))
                .filter(this::matchTagFilter)
                .toList();
    }

    private boolean matchCategory(Item item, String filter) {
        if (filter == null || filter.startsWith("Tat ca")) return true;
        return filter.equalsIgnoreCase(item.getCategory());
    }

    private boolean matchPrice(Item item, String filter) {
        if (filter == null || filter.startsWith("Tat ca")) return true;
        BigDecimal price = item.getCurrentPrice();
        if (price == null) return true;
        return switch (filter) {
            case "Duoi 10 trieu"     -> price.compareTo(PRICE_10M) < 0;
            case "10 - 100 trieu"    -> price.compareTo(PRICE_10M) >= 0 && price.compareTo(PRICE_100M) < 0;
            case "100 trieu - 1 ty"  -> price.compareTo(PRICE_100M) >= 0 && price.compareTo(PRICE_1B) < 0;
            case "Tren 1 ty"         -> price.compareTo(PRICE_1B) >= 0;
            default -> true;
        };
    }

    private boolean matchTagFilter(Item item) {
        return switch (currentTagFilter) {
            case "open"   -> isOpen(item);
            case "ending" -> isEnding(item);
            case "hot"    -> true;  // TODO: BE bo sung bidCount field
            case "new"    -> isNew(item);
            default       -> true;
        };
    }

    private boolean isOpen(Item item) {
        return item.getEndTime() == null
                || item.getEndTime().isAfter(Instant.now());
    }

    private boolean isEnding(Item item) {
        if (item.getEndTime() == null) return false;
        long secondsLeft = item.getEndTime().getEpochSecond() - Instant.now().getEpochSecond();
        return secondsLeft > 0 && secondsLeft < 3600;  // < 1 hour
    }

    private boolean isNew(Item item) {
        if (item.getCreatedAt() == null) return false;
        long secondsAgo = Instant.now().getEpochSecond() - item.getCreatedAt().getEpochSecond();
        return secondsAgo < 86400;  // < 24h
    }

    private List<Item> applySorting(List<Item> items) {
        Comparator<Item> comparator = switch (cbSort.getValue() != null ? cbSort.getValue() : "") {
            case "Gia thap -> cao" -> Comparator.comparing(
                    Item::getCurrentPrice,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "Gia cao -> thap" -> Comparator.comparing(
                    Item::getCurrentPrice,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case "Moi nhat" -> Comparator.comparing(
                    Item::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            default -> Comparator.comparing(
                    Item::getEndTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));  // sap ket thuc
        };
        return items.stream().sorted(comparator).toList();
    }

    /**
     * Build product card tu FXML template.
     * Set data + badges (cert, partner).
     */
    private Node buildProductCard(Item item) {
        try {
            URL resource = getClass().getResource("/fxml/ProductCard.fxml");
            if (resource == null) {
                LOGGER.warning("Khong tim thay ProductCard.fxml");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node card = loader.load();
            ProductCardController ctrl = loader.getController();

            ctrl.setItem(String.valueOf(item.getId()),
                    item.getName(),
                    item.getCurrentPrice(),
                    item.getImageUrl());

            // Tinh nang #4 Kiem dinh
            if (item.isVerified()) {
                ctrl.showCertifiedBadge();
            }
            return card;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Loi build product card cho item: " + item.getId(), e);
            return null;
        }
    }

    // ===== TAG FILTER =====

    @FXML
    private void onTagClick(javafx.event.ActionEvent event) {
        Object src = event.getSource();
        if (!(src instanceof Button btn)) return;

        Object data = btn.getUserData();
        currentTagFilter = data instanceof String s ? s : "open";

        setActiveTag(btn);
        renderProducts();
    }

    private void setActiveTag(Button target) {
        if (activeTag != null) {
            activeTag.getStyleClass().remove("tag-active");
            if (!activeTag.getStyleClass().contains("tag-inactive")) {
                activeTag.getStyleClass().add("tag-inactive");
            }
        }
        target.getStyleClass().remove("tag-inactive");
        if (!target.getStyleClass().contains("tag-active")) {
            target.getStyleClass().add("tag-active");
        }
        activeTag = target;
    }

    @FXML
    private void onAdvancedFilter() {
        LOGGER.info("Mo bo loc nang cao");
        // TODO: open advanced filter dialog
    }
}