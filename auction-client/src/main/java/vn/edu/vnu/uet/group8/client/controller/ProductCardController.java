package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * ProductCardController — card sản phẩm tái sử dụng.
 *
 * Tích hợp:
 *   - onBidNow() navigate thật sang AuctionDetailView (load item trước khi switch)
 *   - showCertifiedBadge() (#4)
 *   - showPartnerBadge() (#3)
 *
 * Pattern: Controller cha (Explore, Favorite) load FXML này → gọi setItem() + show*Badge().
 */
public class ProductCardController implements Initializable {

    @FXML private VBox root;
    @FXML private ImageView productImage;
    @FXML private Label productName;
    @FXML private Label currentPrice;
    @FXML private Label lblCertBadge;
    @FXML private Label lblPartnerBadge;

    private int itemId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideBadge(lblCertBadge);
        hideBadge(lblPartnerBadge);
    }

    // ===== PUBLIC API — gọi từ Controller cha =====

    public void setItem(String id, String name, BigDecimal price, String imageUrl) {
        try {
            this.itemId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            this.itemId = 0;
        }
        productName.setText(name);
        currentPrice.setText(formatPrice(price));

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                productImage.setImage(new Image(imageUrl, true));
            } catch (Exception e) {
                System.err.println("[ProductCard] Không load ảnh: " + imageUrl);
            }
        }
    }

    /** Tính năng #4 — Badge "✓ Đã kiểm định" */
    public void showCertifiedBadge() {
        if (lblCertBadge == null) return;
        lblCertBadge.setText("✓ Đã kiểm định");
        if (!lblCertBadge.getStyleClass().contains("badge-certified")) {
            lblCertBadge.getStyleClass().add("badge-certified");
        }
        lblCertBadge.setVisible(true);
        lblCertBadge.setManaged(true);
    }

    /** Tính năng #3 — Badge đối tác */
    public void showPartnerBadge(String badgeText) {
        if (lblPartnerBadge == null || badgeText == null) return;
        lblPartnerBadge.setText(badgeText);

        String styleClass = badgeText.contains("Gold") ? "badge-partner-gold" : "badge-partner";
        if (!lblPartnerBadge.getStyleClass().contains(styleClass)) {
            lblPartnerBadge.getStyleClass().add(styleClass);
        }

        lblPartnerBadge.setVisible(true);
        lblPartnerBadge.setManaged(true);
    }

    // ===== CLICK CARD — Navigate sang AuctionDetail =====

    @FXML
    private void onBidNow() {
        if (itemId <= 0) {
            System.err.println("[ProductCard] itemId không hợp lệ");
            return;
        }

        // Load chi tiết trước để ClientModel có data sẵn
        // Sau đó switch view — AuctionDetailController sẽ đọc từ ClientModel
        AuctionService.loadDetail(itemId, item -> {
            if (item != null) {
                ClientModel.getInstance().setCurrentAuctionItem(item);
                SceneManager.switchTo(SceneManager.VIEW_AUCTION_DETAIL);
            }
        });
    }

    // ===== HELPERS =====

    private String formatPrice(BigDecimal price) {
        if (price == null) return "--";
        return String.format("%,.0f đ", price);
    }

    private void hideBadge(Label badge) {
        if (badge == null) return;
        badge.setVisible(false);
        badge.setManaged(false);
    }
}