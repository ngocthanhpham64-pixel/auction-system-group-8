package vn.edu.vnu.uet.group8.client.controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

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
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

/**
 * SellerDashboardController — quản lý sản phẩm của seller.
 *
 * ⚠️ Phụ thuộc BE bổ sung ActionType.ITEM_MY_LISTINGS.
 * ⚠️ Item entity chưa có getStartTime() — workaround dùng getEndTime() để filter.
 */
public class SellerDashboardController implements Initializable {

    @FXML Label lblListedCount;
    @FXML Label lblSoldCount;
    @FXML Label lblTotalRevenue;
    @FXML Label lblRating;
    @FXML VBox itemListContainer;
    @FXML Label lblEmpty;

    @FXML Button btnTabAll;
    @FXML Button btnTabDraft;
    @FXML Button btnTabListed;
    @FXML Button btnTabSold;

    protected Button activeTab;
    protected String currentFilter = "all";
    protected List<AuctionItemDTO> myItems = List.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        loadMyListings();
    }

    void loadMyListings() {
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

    void renderItems() {
        if (itemListContainer == null) return;

        itemListContainer.getChildren().clear();

        if (myItems == null || myItems.isEmpty()) {
            showEmptyState();
            return;
        }

        if (lblEmpty != null) {
            lblEmpty.setVisible(false);
            lblEmpty.setManaged(false);
        }

        List<AuctionItemDTO> filtered = myItems.stream()
                .filter(this::matchTab)
                .toList();

        for (AuctionItemDTO item : filtered) {
            itemListContainer.getChildren().add(buildItemRow(item));
        }
    }

    protected boolean matchTab(AuctionItemDTO item) {
        return switch (currentFilter) {
            case "draft"  -> item.getStatus() == SessionStatus.UPCOMING;
            case "listed" -> item.getStatus() == SessionStatus.ACTIVE;
            case "sold"   -> item.getStatus() == SessionStatus.SOLD 
                          || item.getStatus() == SessionStatus.ENDED_NO_BID 
                          || item.getStatus() == SessionStatus.CANCELLED;
            default       -> true;
        };
    }

    protected boolean isActive(AuctionItemDTO item) {

        if (item == null) {
            return false;
        }

        if (item.getStatus() != SessionStatus.ACTIVE) {
            return false;
        }

        if (item.getEndTime() == null) {
            return false;
        }

        return item.getEndTime().isAfter(Instant.now());
    }

    protected HBox buildItemRow(AuctionItemDTO item) {
        HBox row = new HBox(16);
        row.getStyleClass().add("card");
        row.setStyle("-fx-padding: 16; -fx-background-radius: 12;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(item.getTitle());
        name.getStyleClass().add("h3");

        Label category = new Label(item.getCategory() != null ? item.getCategory().name() : "--");
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

    void showEmptyState() {
        if (itemListContainer != null) {
            itemListContainer.getChildren().clear();
        }

        if (lblEmpty != null) {
            lblEmpty.setVisible(true);
            lblEmpty.setManaged(true);
        }
    }

    void updateStats() {
        int listed = (int) myItems.stream()
                .filter(it -> it.getStatus() == SessionStatus.ACTIVE)
                .count();

        int sold = (int) myItems.stream()
                .filter(it -> it.getStatus() == SessionStatus.SOLD)
                .count();

        BigDecimal revenue = myItems.stream()
                .filter(it -> it.getStatus() == SessionStatus.SOLD)
                .map(AuctionItemDTO::getCurrentPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lblListedCount != null) {
            lblListedCount.setText(String.valueOf(listed));
        }

        if (lblSoldCount != null) {
            lblSoldCount.setText(String.valueOf(sold));
        }

        if (lblTotalRevenue != null) {
            lblTotalRevenue.setText(String.format("%,.0f đ", revenue));
        }

        if (lblRating != null) {
            lblRating.setText("--");
        }
    }

    @FXML
    void onCreateNew() {
        // TODO: ClientModel cần thêm setEditingItemId để truyền itemId khi edit
        // Hiện tại tạo mới — không cần truyền itemId
        SceneManager.switchTo("CreateItemView.fxml");
    }

    void onEditItem(AuctionItemDTO item) {
        // Tạm dùng setCurrentAuctionItem để truyền item sang CreateItemView
        ClientModel.getInstance().setCurrentAuctionItem(item);
        SceneManager.switchTo("CreateItemView.fxml");
    }

    void onDeleteItem(AuctionItemDTO item) {
        boolean ok = AlertUtil.showConfirm("Xác nhận xóa",
                "Xóa sản phẩm \"" + item.getTitle() + "\"?\n"
                        + "Hành động này không thể hoàn tác.");
        if (!ok) return;

        SellerService.deleteItem(item.getItemId(), success -> {
            if (success) {
                AlertUtil.showInfo("Đã xóa sản phẩm");
                loadMyListings();
            } else {
                AlertUtil.showError("Xóa thất bại");
            }
        });
    }

    @FXML void onTabAll()    { setTab("all", btnTabAll); }
    @FXML void onTabDraft()  { setTab("draft", btnTabDraft); }
    @FXML void onTabListed() { setTab("listed", btnTabListed); }
    @FXML void onTabSold()   { setTab("sold", btnTabSold); }

    void setTab(String filter, Button button) {
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