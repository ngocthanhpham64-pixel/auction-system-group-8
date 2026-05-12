package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.common.entity.Item;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SellerDashboardController — quản lý sản phẩm của seller.
 *
 * ⚠️ Phụ thuộc BE bổ sung ActionType.ITEM_MY_LISTINGS.
 * ⚠️ Item entity chưa có getStartTime() — workaround dùng getEndTime() để filter.
 */
public class SellerDashboardController implements Initializable {

    @FXML private Label lblListedCount;
    @FXML private Label lblSoldCount;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblRating;
    @FXML private VBox itemListContainer;
    @FXML private Label lblEmpty;

    @FXML private Button btnTabAll;
    @FXML private Button btnTabDraft;
    @FXML private Button btnTabListed;
    @FXML private Button btnTabSold;

    private Button activeTab;
    private String currentFilter = "all";
    private List<Item> myItems = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        loadMyListings();
    }

    private void loadMyListings() {
        SellerService.getMyListings(
                items -> {
                    myItems = items;
                    renderItems();
                    updateStats();
                },
                error -> {
                    AlertUtil.showError(error);
                    showEmptyState();
                }
        );
    }

    private void renderItems() {
        itemListContainer.getChildren().clear();

        if (myItems == null || myItems.isEmpty()) {
            showEmptyState();
            return;
        }

        lblEmpty.setVisible(false);
        lblEmpty.setManaged(false);

        List<Item> filtered = myItems.stream()
                .filter(this::matchTab)
                .toList();

        for (Item item : filtered) {
            itemListContainer.getChildren().add(buildItemRow(item));
        }
    }

    private boolean matchTab(Item item) {
        // TODO: BE cần thêm Item.status (DRAFT/LISTED/SOLD) để filter chính xác
        return switch (currentFilter) {
            case "draft"  -> false;  // chưa có cách phân biệt draft
            case "listed" -> isActive(item);
            case "sold"   -> !isActive(item);
            default       -> true;
        };
    }

    private boolean isActive(Item item) {
        if (item.getEndTime() == null) return false;
        return item.getEndTime().isAfter(Instant.now());
    }

    private HBox buildItemRow(Item item) {
        HBox row = new HBox(16);
        row.getStyleClass().add("card");
        row.setStyle("-fx-padding: 16; -fx-background-radius: 12;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(item.getName());
        name.getStyleClass().add("h3");

        Label category = new Label(item.getCategory() != null ? item.getCategory() : "--");
        category.getStyleClass().add("label-info");

        Label price = new Label(String.format("%,.0f đ",
                item.getCurrentPrice() != null ? item.getCurrentPrice() : BigDecimal.ZERO));
        price.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");

        info.getChildren().addAll(name, category, price);

        Button btnEdit = new Button("Sửa");
        btnEdit.getStyleClass().add("btn-secondary");
        btnEdit.setOnAction(e -> onEditItem(item));

        Button btnDelete = new Button("Xóa");
        btnDelete.getStyleClass().add("btn-danger");
        btnDelete.setOnAction(e -> onDeleteItem(item));

        row.getChildren().addAll(info, btnEdit, btnDelete);
        return row;
    }

    private void showEmptyState() {
        itemListContainer.getChildren().clear();
        lblEmpty.setVisible(true);
        lblEmpty.setManaged(true);
    }

    private void updateStats() {
        int listed = (int) myItems.stream().filter(this::isActive).count();
        int sold = (int) myItems.stream().filter(it -> !isActive(it)).count();
        BigDecimal revenue = myItems.stream()
                .filter(it -> !isActive(it))
                .map(Item::getCurrentPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblListedCount.setText(String.valueOf(listed));
        lblSoldCount.setText(String.valueOf(sold));
        lblTotalRevenue.setText(String.format("%,.0f đ", revenue));
        lblRating.setText("--");
    }

    @FXML
    private void onCreateNew() {
        // TODO: ClientModel cần thêm setEditingItemId để truyền itemId khi edit
        // Hiện tại tạo mới — không cần truyền itemId
        SceneManager.switchTo("CreateItemView.fxml");
    }

    private void onEditItem(Item item) {
        // Tạm dùng setCurrentAuctionItem để truyền item sang CreateItemView
        ClientModel.getInstance().setCurrentAuctionItem(item);
        SceneManager.switchTo("CreateItemView.fxml");
    }

    private void onDeleteItem(Item item) {
        boolean ok = AlertUtil.showConfirm("Xác nhận xóa",
                "Xóa sản phẩm \"" + item.getName() + "\"?\n"
                        + "Hành động này không thể hoàn tác.");
        if (!ok) return;

        SellerService.deleteItem(item.getId(), success -> {
            if (success) {
                AlertUtil.showInfo("Đã xóa sản phẩm");
                loadMyListings();
            } else {
                AlertUtil.showError("Xóa thất bại");
            }
        });
    }

    @FXML private void onTabAll()    { setTab("all", btnTabAll); }
    @FXML private void onTabDraft()  { setTab("draft", btnTabDraft); }
    @FXML private void onTabListed() { setTab("listed", btnTabListed); }
    @FXML private void onTabSold()   { setTab("sold", btnTabSold); }

    private void setTab(String filter, Button button) {
        currentFilter = filter;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            activeTab.getStyleClass().add("tag-inactive");
        }
        button.getStyleClass().remove("tag-inactive");
        button.getStyleClass().add("tag-active");
        activeTab = button;
        renderItems();
    }
}