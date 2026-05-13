package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
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
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FavoriteController — danh sach san pham yeu thich.
 *
 * Tinh nang:
 *  - Load tu FavoriteService.loadAll()
 *  - Binding ClientModel.favoriteItems -> tu refresh
 *  - 3 tab: Tat ca / Dang hoat dong / Da ket thuc
 *  - Clear ended: async voi counter de cho tat ca xong moi reload
 *  - lblActiveCount hien so item dang active
 */
public class FavoriteController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(FavoriteController.class.getName());

    @FXML private FlowPane favoriteContainer;
    @FXML private Label lblActiveCount;
    @FXML private Button btnTabAll;
    @FXML private Button btnTabActive;
    @FXML private Button btnTabEnded;

    private Button activeTab;
    private String currentFilter = "all";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        bindFavorites();
        loadFavorites();
    }

    /** Binding ClientModel.favoriteItems -> tu update UI. */
    private void bindFavorites() {
        ClientModel.getInstance().favoriteItemsProperty().addListener((obs, oldList, newList) ->
                Platform.runLater(this::renderFromModel)
        );
    }

    private void loadFavorites() {
        showLoading();
        FavoriteService.loadAll(
                items -> {
                    LOGGER.info(() -> "Da tai " + items.size() + " san pham yeu thich");
                    // ClientModel duoc service update -> listener tu trigger render
                },
                error -> {
                    LOGGER.warning("Loi tai favorites: " + error);
                    renderError(error);
                }
        );
    }

    private void renderFromModel() {
        List<Item> items = ClientModel.getInstance().getFavoriteItems();
        render(items);
    }

    private void showLoading() {
        if (lblActiveCount != null) {
            lblActiveCount.setText("Dang tai...");
        }
    }

    private void renderError(String error) {
        favoriteContainer.getChildren().clear();
        Label err = new Label("Khong the tai danh sach yeu thich");
        err.getStyleClass().add("label-error");
        err.setStyle("-fx-padding: 40 0;");
        favoriteContainer.getChildren().add(err);
        if (lblActiveCount != null) lblActiveCount.setText("0");
    }

    private void render(List<Item> items) {
        if (favoriteContainer == null) return;
        favoriteContainer.getChildren().clear();

        if (items == null || items.isEmpty()) {
            renderEmpty();
            return;
        }

        List<Item> filtered = items.stream().filter(this::matchTab).toList();
        if (filtered.isEmpty()) {
            renderEmpty();
            return;
        }

        for (Item item : filtered) {
            Node card = buildProductCard(item);
            if (card != null) favoriteContainer.getChildren().add(card);
        }

        updateActiveCount(items);
    }

    private void renderEmpty() {
        Label empty = new Label("Chua co san pham yeu thich");
        empty.getStyleClass().add("label-info");
        empty.setStyle("-fx-padding: 40 0;");
        favoriteContainer.getChildren().add(empty);
        if (lblActiveCount != null) lblActiveCount.setText("0");
    }

    private boolean matchTab(Item item) {
        return switch (currentFilter) {
            case "active" -> isActive(item);
            case "ended"  -> !isActive(item);
            default       -> true;
        };
    }

    private boolean isActive(Item item) {
        return item.getEndTime() != null && item.getEndTime().isAfter(Instant.now());
    }

    private void updateActiveCount(List<Item> items) {
        if (lblActiveCount == null) return;
        long active = items.stream().filter(this::isActive).count();
        lblActiveCount.setText(active + " dang dau gia");
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
            LOGGER.log(Level.WARNING, "Loi build card cho item: " + item.getId(), e);
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
            if (!activeTab.getStyleClass().contains("tag-inactive")) {
                activeTab.getStyleClass().add("tag-inactive");
            }
        }
        button.getStyleClass().remove("tag-inactive");
        if (!button.getStyleClass().contains("tag-active")) {
            button.getStyleClass().add("tag-active");
        }
        activeTab = button;
        renderFromModel();
    }

    /**
     * Xoa tat ca san pham da ket thuc khoi yeu thich.
     * Async safe: dung counter de cho tat ca remove xong moi alert thanh cong.
     */
    @FXML
    private void onClearEnded() {
        List<Item> endedFavs = ClientModel.getInstance().getFavoriteItems()
                .stream()
                .filter(it -> !isActive(it))
                .toList();

        if (endedFavs.isEmpty()) {
            AlertUtil.showInfo("Khong co san pham da ket thuc");
            return;
        }

        boolean ok = AlertUtil.showConfirm("Xac nhan",
                "Xoa " + endedFavs.size() + " san pham da ket thuc khoi yeu thich?");
        if (!ok) return;

        // Async counter: cho tat ca remove xong moi notify
        final int total = endedFavs.size();
        final AtomicInteger completed = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);

        LOGGER.info(() -> "Bat dau xoa " + total + " san pham");

        for (Item item : endedFavs) {
            FavoriteService.remove(item.getId(),
                    () -> {
                        int done = completed.incrementAndGet();
                        if (done == total) onClearComplete(total, errors.get());
                    },
                    err -> {
                        errors.incrementAndGet();
                        int done = completed.incrementAndGet();
                        if (done == total) onClearComplete(total, errors.get());
                    }
            );
        }
    }

    private void onClearComplete(int total, int errorCount) {
        Platform.runLater(() -> {
            int success = total - errorCount;
            if (errorCount == 0) {
                AlertUtil.showInfo("Da xoa " + success + " san pham khoi yeu thich");
            } else {
                AlertUtil.showWarning("Da xoa " + success + "/" + total
                        + ". " + errorCount + " san pham loi.");
            }
            // ClientModel da tu update -> listener trigger render
        });
    }
}