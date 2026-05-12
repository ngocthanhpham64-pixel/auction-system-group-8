package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.FavoriteService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.common.entity.Item;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FavoriteController — wire FavoriteService.
 *
 * Tabs: Tất cả / Đang HĐ / Đã KT — filter từ list local.
 */
public class FavoriteController implements Initializable {

    @FXML private FlowPane productContainer;
    @FXML private Label lblResultCount;
    @FXML private Button btnTabAll;
    @FXML private Button btnTabActive;
    @FXML private Button btnTabEnded;

    private Button activeTab;
    private String currentFilter = "all";  // "all" / "active" / "ended"

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        loadFavorites();
    }

    private void loadFavorites() {
        lblResultCount.setText("Đang tải...");
        FavoriteService.loadAll(
                items -> renderFavorites(items),
                error -> {
                    lblResultCount.setText("Lỗi: " + error);
                    productContainer.getChildren().clear();
                }
        );
    }

    private void renderFavorites(List<Item> items) {
        productContainer.getChildren().clear();
        if (items == null || items.isEmpty()) {
            lblResultCount.setText("Chưa có sản phẩm yêu thích");
            return;
        }

        // Filter theo tab
        List<Item> filtered = items.stream()
                .filter(this::matchTab)
                .toList();

        for (Item item : filtered) {
            Node card = buildProductCard(item);
            if (card != null) productContainer.getChildren().add(card);
        }

        lblResultCount.setText("Hiển thị " + filtered.size() + " sản phẩm");
    }

    private boolean matchTab(Item item) {
        return switch (currentFilter) {
            case "active" -> isActive(item);
            case "ended"  -> !isActive(item);
            default       -> true;
        };
    }

    private boolean isActive(Item item) {
        if (item.getEndTime() == null) return true;
        return item.getEndTime().isAfter(java.time.Instant.now());
    }

    private Node buildProductCard(Item item) {
        try {
            URL resource = getClass().getResource("/fxml/ProductCard.fxml");
            if (resource == null) return null;
            FXMLLoader loader = new FXMLLoader(resource);
            Node card = loader.load();
            ProductCardController ctrl = loader.getController();
            ctrl.setItem(String.valueOf(item.getId()), item.getName(),
                    item.getCurrentPrice(), item.getImageUrl());
            if (item.isVerified()) ctrl.showCertifiedBadge();
            return card;
        } catch (IOException e) {
            System.err.println("[Favorite] Lỗi load card: " + e.getMessage());
            return null;
        }
    }

    // ===== TABS =====

    @FXML private void onTabAll()    { setTab("all", btnTabAll); }
    @FXML private void onTabActive() { setTab("active", btnTabActive); }
    @FXML private void onTabEnded()  { setTab("ended", btnTabEnded); }

    private void setTab(String filter, Button button) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            activeTab.getStyleClass().add("tag-inactive");
        }
        button.getStyleClass().remove("tag-inactive");
        button.getStyleClass().add("tag-active");
        activeTab = button;
        // Render lại từ ClientModel
        renderFavorites(ClientModel.getInstance().getFavoriteItems());
    }

    @FXML
    private void onClearEnded() {
        boolean ok = AlertUtil.showConfirm("Xác nhận",
                "Xóa tất cả sản phẩm đã kết thúc khỏi yêu thích?");
        if (!ok) return;

        // Lấy danh sách đã kết thúc và remove từng cái
        List<Item> favs = ClientModel.getInstance().getFavoriteItems();
        favs.stream()
                .filter(it -> !isActive(it))
                .forEach(it -> FavoriteService.remove(it.getId(),
                        () -> {},  // success: ClientModel tự cập nhật
                        err -> AlertUtil.showError(err)
                ));
        // Reload để render lại
        loadFavorites();
    }
}