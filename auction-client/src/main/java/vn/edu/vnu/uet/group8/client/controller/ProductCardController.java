package vn.edu.vnu.uet.group8.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ProductCardController — card san pham trong Explore / Favorite.
 *
 * Tinh nang:
 *  - setItem(): set data tu controller cha
 *  - showCertifiedBadge() (#4): hien badge khi item.isVerified()
 *  - showPartnerBadge() (#3): hien badge khi seller la partner
 *  - Click card: load chi tiet -> navigate sang AuctionDetail trong MainLayout
 *  - Load image error -> dung placeholder
 *
 * Pattern: card tai su dung - controller cha load FXML va goi setItem()
 */
public class ProductCardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ProductCardController.class.getName());

    @FXML private VBox root;
    @FXML private StackPane imageContainer;
    @FXML private ImageView productImage;
    @FXML private Label productName;
    @FXML private Label currentPrice;

    // Badge optional - co the khong co trong FXML
    @FXML private Label lblCertBadge;
    @FXML private Label lblPartnerBadge;

    private int itemId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideBadge(lblCertBadge);
        hideBadge(lblPartnerBadge);
        setupClickHandler();
    }

    /** Bat su kien click vao card (root VBox) -> navigate detail. */
    private void setupClickHandler() {
        if (root != null) {
            root.setOnMouseClicked(e -> onCardClick());
            root.setStyle(root.getStyle() + " -fx-cursor: hand;");
        }
    }

    // ===== PUBLIC API - GOI TU CONTROLLER CHA =====

    /**
     * Set du lieu cho card.
     */
    public void setItem(String id, String name, BigDecimal price, String imageUrl) {
        // Parse id an toan
        try {
            this.itemId = (id != null) ? Integer.parseInt(id) : 0;
        } catch (NumberFormatException e) {
            this.itemId = 0;
            LOGGER.warning("Invalid itemId: " + id);
        }

        if (productName != null) {
            productName.setText(name != null ? name : "Khong co ten");
        }

        if (currentPrice != null) {
            currentPrice.setText(formatPrice(price));
        }

        loadImage(imageUrl);
    }

    /** Load image an toan - fallback neu URL invalid. */
    private void loadImage(String imageUrl) {
        if (productImage == null) return;
        if (imageUrl == null || imageUrl.isBlank()) return;

        try {
            // Lazy load (background=true)
            Image img = new Image(imageUrl, true);
            productImage.setImage(img);

            // Listen error
            img.errorProperty().addListener((obs, oldVal, hasError) -> {
                if (hasError) {
                    LOGGER.fine(() -> "Khong load duoc anh: " + imageUrl);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Exception loading image: " + imageUrl, e);
        }
    }

    /** Tinh nang #4 - Badge "Da kiem dinh" */
    public void showCertifiedBadge() {
        showBadge(lblCertBadge, "✓ Da kiem dinh", "badge-certified");
    }

    /** Tinh nang #3 - Badge doi tac */
    public void showPartnerBadge(String text) {
        if (text == null) return;
        String styleClass = text.contains("Gold") || text.contains("VIP")
                ? "badge-partner-gold"
                : "badge-partner";
        showBadge(lblPartnerBadge, text, styleClass);
    }

    private void showBadge(Label badge, String text, String styleClass) {
        if (badge == null) return;
        badge.setText(text);
        if (!badge.getStyleClass().contains(styleClass)) {
            badge.getStyleClass().add(styleClass);
        }
        badge.setVisible(true);
        badge.setManaged(true);
    }

    private void hideBadge(Label badge) {
        if (badge == null) return;
        badge.setVisible(false);
        badge.setManaged(false);
    }

    // ===== CLICK HANDLER =====

    /**
     * Click vao card -> load chi tiet item + chuyen sang AuctionDetail.
     * Uu tien dung MainController.loadView() de giu sidebar.
     */
    private void onCardClick() {
        if (itemId <= 0) {
            LOGGER.warning("Invalid itemId for navigation");
            return;
        }

        LOGGER.fine(() -> "Click card: itemId=" + itemId);

        AuctionService.loadDetail(itemId, item -> {
            if (item == null) {
                AlertUtil.showWarning("Khong tai duoc chi tiet san pham");
                return;
            }
            ClientModel.getInstance().setCurrentAuctionItem(item);
            navigateToDetail();
        });
    }

    private void navigateToDetail() {
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadView(SceneManager.VIEW_AUCTION_DETAIL);
        } else {
            // Fallback: full scene switch
            SceneManager.switchTo(SceneManager.VIEW_AUCTION_DETAIL);
        }
    }

    // ===== HELPER =====

    private String formatPrice(BigDecimal price) {
        if (price == null) return "--";
        return String.format("%,.0f d", price);
    }
}