package vn.edu.vnu.uet.group8.client.controller;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
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
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;

/**
 * FavoriteController — quản lý danh sách yêu thích.
 *
 * <p>Phiên bản tối giản cho mục đích demo UI:
 * <ul>
 *   <li>Đọc trực tiếp từ {@link ClientModel#favoriteItemsProperty()} — không gọi server</li>
 *   <li>3 tab filter: Tất cả / Đang hoạt động / Đã kết thúc (lọc trên client)</li>
 *   <li>Nút "Xoá đã kết thúc" gỡ các item đã hết giờ khỏi ClientModel</li>
 *   <li>Render thẻ sản phẩm bằng cách reuse {@code ProductCard.fxml}</li>
 * </ul>
 *
 * <p>Khi backend bật, có thể tiêm thêm FavoriteService (LOAD/ADD/REMOVE) — kiến trúc
 * hiện tại không khoá điều đó.
 */
public class FavoriteController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(FavoriteController.class.getName());

    @FXML FlowPane favoriteContainer;
    @FXML Label lblActiveCount;
    @FXML Button btnTabAll;
    @FXML Button btnTabActive;
    @FXML Button btnTabEnded;

    private Button activeTab;
    /** "all" | "active" | "ended" */
    private String currentFilter = "all";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        // Lắng nghe ClientModel — tự re-render khi danh sách yêu thích thay đổi
        ClientModel.getInstance().favoriteItemsProperty().addListener((obs, oldList, newList) ->
                Platform.runLater(this::renderFromModel)
        );
        renderFromModel();
    }

    // ===================== RENDER =====================

    void renderFromModel() {
        if (favoriteContainer == null) return;
        favoriteContainer.getChildren().clear();

        List<AuctionItemDTO> all = new ArrayList<>(ClientModel.getInstance().getFavoriteItems());
        List<AuctionItemDTO> shown = filter(all);
        long activeCnt = all.stream().filter(this::isActive).count();

        if (lblActiveCount != null) {
            lblActiveCount.setText("Bạn có " + activeCnt + " món đang còn thời gian đấu giá");
        }

        if (shown.isEmpty()) {
            Label empty = new Label(emptyMessage());
            empty.getStyleClass().add("label-info");
            empty.setStyle("-fx-padding: 40 0; -fx-font-size: 14px;");
            favoriteContainer.getChildren().add(empty);
            return;
        }

        for (AuctionItemDTO item : shown) {
            Node card = buildCard(item);
            if (card != null) favoriteContainer.getChildren().add(card);
        }
    }

    private String emptyMessage() {
        return switch (currentFilter) {
            case "active" -> "Không có món yêu thích nào đang còn hoạt động";
            case "ended"  -> "Chưa có món yêu thích nào đã kết thúc";
            default       -> "Bạn chưa có món yêu thích nào";
        };
    }

    private List<AuctionItemDTO> filter(List<AuctionItemDTO> src) {
        return switch (currentFilter) {
            case "active" -> src.stream().filter(this::isActive).toList();
            case "ended"  -> src.stream().filter(this::isEnded).toList();
            default       -> src;
        };
    }

    private boolean isActive(AuctionItemDTO item) {
        return item.getEndTime() == null || item.getEndTime().isAfter(Instant.now());
    }

    private boolean isEnded(AuctionItemDTO item) {
        return item.getEndTime() != null && !item.getEndTime().isAfter(Instant.now());
    }

    private Node buildCard(AuctionItemDTO item) {
        try {
            URL resource = getClass().getResource("/fxml/ProductCard.fxml");
            if (resource == null) {
                LOGGER.warning("Không tìm thấy /fxml/ProductCard.fxml");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node card = loader.load();
            ProductCardController ctrl = loader.getController();
            ctrl.setItem(String.valueOf(item.getItemId()),
                    item.getTitle(),
                    item.getCurrentPrice(),
                    "");
            return card;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Lỗi build card cho item " + item.getItemId(), e);
            return null;
        }
    }

    // ===================== TABS =====================

    @FXML
    void onTabAll() {
        currentFilter = "all";
        setActiveTab(btnTabAll);
        renderFromModel();
    }

    @FXML
    void onTabActive() {
        currentFilter = "active";
        setActiveTab(btnTabActive);
        renderFromModel();
    }

    @FXML
    void onTabEnded() {
        currentFilter = "ended";
        setActiveTab(btnTabEnded);
        renderFromModel();
    }

    void setActiveTab(Button target) {
        if (target == null) return;
        if (activeTab != null) {
            activeTab.getStyleClass().remove("tag-active");
            if (!activeTab.getStyleClass().contains("tag-inactive")) {
                activeTab.getStyleClass().add("tag-inactive");
            }
        }
        target.getStyleClass().remove("tag-inactive");
        if (!target.getStyleClass().contains("tag-active")) {
            target.getStyleClass().add("tag-active");
        }
        activeTab = target;
    }

    // ===================== ACTIONS =====================

    @FXML
    void onClearEnded() {
        List<AuctionItemDTO> all = new ArrayList<>(ClientModel.getInstance().getFavoriteItems());
        List<AuctionItemDTO> ended = all.stream().filter(this::isEnded).toList();
        if (ended.isEmpty()) {
            AlertUtil.showInfo("Không có món nào đã kết thúc để xoá");
            return;
        }
        boolean ok = AlertUtil.showConfirm("Xác nhận",
                "Xoá " + ended.size() + " món đã kết thúc khỏi danh sách yêu thích?");
        if (!ok) return;

        for (AuctionItemDTO it : ended) {
            ClientModel.getInstance().removeFavorite(it);
        }
        AlertUtil.showInfo("Đã xoá " + ended.size() + " món");
        // listener trên favoriteItemsProperty sẽ tự re-render
    }
}
