package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductCardController implements Initializable {

    // ===== FXML BINDINGS =====
    @FXML private VBox root;          // root container (styleClass="product-card")
    @FXML private ImageView productImage;
    @FXML private Label productName;
    @FXML private Label currentPrice;

    // ===== DATA =====
    private String itemId;

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Hover được xử lý hoàn toàn bởi CSS class "product-card" và "product-card:hover"
        // Không cần code Java thêm — CSS đã có:
        //   .product-card:hover { -fx-translate-y: -3; -fx-border-color: #F97316; ... }
    }

    // ===== PUBLIC API — được gọi từ ExploreController / FavoriteController =====

    /**
     * Nạp dữ liệu item vào card.
     * Gọi sau khi FXMLLoader.load() để truyền dữ liệu từ controller cha.
     *
     * @param id          ID item
     * @param name        Tên sản phẩm
     * @param price       Giá hiện tại
     * @param imageUrl    URL ảnh (null = dùng placeholder)
     */
    public void setItem(String id, String name, BigDecimal price, String imageUrl) {
        this.itemId = id;

        productName.setText(name);
        currentPrice.setText(formatPrice(price));

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                productImage.setImage(new Image(imageUrl, true));
            } catch (Exception e) {
                System.err.println("[ProductCardController] Không load được ảnh: " + imageUrl);
            }
        }
    }

    // ===== ACTIONS =====

    @FXML
    private void onBidNow() {
        // Điều hướng sang AuctionDetailView với itemId
        System.out.println("[ProductCard] Đặt giá ngay — item: " + itemId);
        // TODO: gọi MainController.loadView("AuctionDetailView.fxml") với itemId
    }

    // ===== HELPERS =====

    private String formatPrice(BigDecimal price) {
        if (price == null) return "--";
        // Định dạng: 50,000,000 đ
        return String.format("%,.0f đ", price);
    }
}
