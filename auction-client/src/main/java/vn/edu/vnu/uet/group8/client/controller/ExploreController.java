package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ExploreController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbPrice;
    @FXML private ComboBox<String> cbSort;
    @FXML private FlowPane productContainer;
    @FXML private Label lblResultCount;

    // Tag buttons
    @FXML private Button btnTagOpen;

    // ===== STATE =====
    private Button activeTag;

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComboBoxes();
        activeTag = btnTagOpen;
        loadProducts();
    }

    private void initComboBoxes() {
        cbCategory.getItems().addAll(
                "Tất cả danh mục",
                "Đồng hồ cao cấp",
                "Điện tử",
                "Trang sức",
                "Nghệ thuật",
                "Xe cổ"
        );
        cbCategory.getSelectionModel().selectFirst();

        cbPrice.getItems().addAll(
                "Tất cả mức giá",
                "Dưới 10 triệu",
                "10 - 100 triệu",
                "100 triệu - 1 tỷ",
                "Trên 1 tỷ"
        );
        cbPrice.getSelectionModel().selectFirst();

        cbSort.getItems().addAll(
                "Sắp kết thúc",
                "Mới nhất",
                "Giá thấp → cao",
                "Giá cao → thấp",
                "Nhiều lượt đấu nhất"
        );
        cbSort.getSelectionModel().selectFirst();

        // Listener: load lại khi đổi filter
        cbCategory.setOnAction(e -> loadProducts());
        cbPrice.setOnAction(e -> loadProducts());
        cbSort.setOnAction(e -> loadProducts());
    }

    // ===== TAG FILTER =====

    @FXML
    private void onTagClick(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        setActiveTag(clicked);
        loadProducts();
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

    // ===== FILTER NÂNG CAO =====

    @FXML
    private void onAdvancedFilter() {
        // TODO: mở dialog bộ lọc nâng cao
        System.out.println("[ExploreController] Mở bộ lọc nâng cao");
    }

    // ===== LOAD PRODUCT CARDS =====

    private void loadProducts() {
        productContainer.getChildren().clear();

        // TODO: thay bằng dữ liệu thật từ server
        int count = 8;
        for (int i = 0; i < count; i++) {
            try {
                URL resource = getClass().getResource("/fxml/ProductCard.fxml");
                if (resource == null) continue;
                FXMLLoader loader = new FXMLLoader(resource);
                Node card = loader.load();

                // Truyền dữ liệu vào card nếu controller đã implement setItem()
                // ProductCardController ctrl = loader.getController();
                // ctrl.setItem(items.get(i));

                productContainer.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("[ExploreController] Lỗi load ProductCard: " + e.getMessage());
            }
        }

        lblResultCount.setText("Hiển thị " + count + " kết quả");
    }
}
