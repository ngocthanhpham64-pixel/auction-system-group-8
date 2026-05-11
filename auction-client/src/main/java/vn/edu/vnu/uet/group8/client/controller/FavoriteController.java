package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FavoriteController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private FlowPane favoriteContainer;
    @FXML private Label lblActiveCount;
    @FXML private Button btnTabAll;
    @FXML private Button btnTabActive;
    @FXML private Button btnTabEnded;

    // ===== STATE =====
    private Button activeTab;
    /** "all" | "active" | "ended" */
    private String currentFilter = "all";

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeTab = btnTabAll;
        loadFavorites();
    }

    // ===== TABS =====

    @FXML private void onTabAll()    { setTab(btnTabAll,    "all");    loadFavorites(); }
    @FXML private void onTabActive() { setTab(btnTabActive, "active"); loadFavorites(); }
    @FXML private void onTabEnded()  { setTab(btnTabEnded,  "ended");  loadFavorites(); }

    private void setTab(Button target, String filter) {
        currentFilter = filter;
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

    // ===== XÓA ĐÃ KẾT THÚC =====

    @FXML
    private void onClearEnded() {
        // TODO: xóa các phiên đã kết thúc khỏi danh sách yêu thích
        System.out.println("[FavoriteController] Xóa các mục đã kết thúc");
        loadFavorites();
    }

    // ===== LOAD CARDS =====

    private void loadFavorites() {
        favoriteContainer.getChildren().clear();

        // TODO: lấy dữ liệu thật từ server theo currentFilter
        int count = switch (currentFilter) {
            case "active" -> 8;
            case "ended"  -> 4;
            default       -> 12;
        };

        for (int i = 0; i < count; i++) {
            try {
                URL resource = getClass().getResource("/fxml/ProductCard.fxml");
                if (resource == null) continue;
                FXMLLoader loader = new FXMLLoader(resource);
                Node card = loader.load();
                favoriteContainer.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("[FavoriteController] Lỗi load card: " + e.getMessage());
            }
        }

        lblActiveCount.setText("Bạn có " + count + " món"
                + (currentFilter.equals("all") ? " trong danh sách yêu thích" : ""));
    }
}
