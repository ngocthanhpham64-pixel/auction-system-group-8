package vn.edu.vnu.uet.group8.client.controller;

import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.service.SellerService;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

public class MyProductsController {

    // Ánh xạ các nút bấm (Tabs)
    @FXML private Button btnAll;
    @FXML private Button btnActive;
    @FXML private Button btnCompleted;
    @FXML private Button btnDraft;

    // Ánh xạ các danh sách tương ứng
    @FXML private VBox vboxAll;
    @FXML private VBox vboxActive;
    @FXML private VBox vboxCompleted;
    @FXML private VBox vboxDraft;

    @FXML private TextField txtSearch;
    @FXML private Button btnAddNew;

    private final String ACTIVE_STYLE   = "-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;";
    private final String INACTIVE_STYLE = "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand; -fx-border-color: #94a3b8; -fx-border-radius: 20;";

    @FXML
    public void initialize() {
        btnAll.setOnAction(e -> switchTab(btnAll, vboxAll));
        btnActive.setOnAction(e -> switchTab(btnActive, vboxActive));
        btnCompleted.setOnAction(e -> switchTab(btnCompleted, vboxCompleted));
        if (btnDraft != null) btnDraft.setOnAction(e -> switchTab(btnDraft, vboxDraft));

        btnAddNew.setOnAction(event -> {
            if (MainController.getInstance() != null) {
                MainController.getInstance().loadContentView("CreateItem.fxml");
            }
        });

        loadMyProducts();
    }

    /**
     * Gọi SellerService.getMyListings() để lấy danh sách sản phẩm của user,
     * sau đó phân loại và render vào từng VBox tương ứng.
     */
    private void loadMyProducts() {
        SellerService.getMyListings(
                items -> Platform.runLater(() -> renderProducts(items)),
                error -> Platform.runLater(() -> showError(vboxAll, "Không thể tải danh sách sản phẩm: " + error))
        );
    }

    private boolean isDraftItem(AuctionItemDTO item) {
        if (item.getStatus() != null && "DRAFT".equals(item.getStatus().name())) return true;
        // Nếu không có giá hiện tại và thời gian kết thúc -> Chưa tạo phiên đấu giá -> Chắc chắn là Nháp
        if (item.getCurrentPrice() == null && item.getEndTime() == null) return true;
        return false;
    }

    /**
     * Phân loại items theo status và đổ vào 3 VBox.
     */
    private void renderProducts(List<AuctionItemDTO> allItems) {
        List<AuctionItemDTO> draftItems = allItems.stream()
                .filter(this::isDraftItem)
                .collect(Collectors.toList());

        List<AuctionItemDTO> activeItems = allItems.stream()
                .filter(i -> !isDraftItem(i) && (i.getStatus() == SessionStatus.ACTIVE || i.getStatus() == SessionStatus.UPCOMING))
                .collect(Collectors.toList());

        List<AuctionItemDTO> completedItems = allItems.stream()
                .filter(i -> !isDraftItem(i) && (i.getStatus() == SessionStatus.SOLD
                        || i.getStatus() == SessionStatus.ENDED_NO_BID
                        || i.getStatus() == SessionStatus.CANCELLED))
                .collect(Collectors.toList());

        btnAll.setText("Tất cả (" + allItems.size() + ")");
        btnActive.setText("Đang đấu giá (" + activeItems.size() + ")");
        btnCompleted.setText("Đã hoàn thành (" + completedItems.size() + ")");
        if (btnDraft != null) btnDraft.setText("Bản nháp (" + draftItems.size() + ")");

        vboxAll.getChildren().clear();
        vboxActive.getChildren().clear();
        vboxCompleted.getChildren().clear();
        if (vboxDraft != null) vboxDraft.getChildren().clear();

        if (allItems.isEmpty()) {
            showError(vboxAll, "Bạn chưa có sản phẩm nào.");
        } else {
            for (AuctionItemDTO item : allItems) vboxAll.getChildren().add(createItemCard(item));
        }

        if (activeItems.isEmpty()) {
            showError(vboxActive, "Không có sản phẩm nào đang đấu giá.");
        } else {
            for (AuctionItemDTO item : activeItems) vboxActive.getChildren().add(createItemCard(item));
        }

        if (completedItems.isEmpty()) {
            showError(vboxCompleted, "Không có sản phẩm nào đã hoàn thành.");
        } else {
            for (AuctionItemDTO item : completedItems) vboxCompleted.getChildren().add(createItemCard(item));
        }
        
        if (draftItems.isEmpty()) {
            if (vboxDraft != null) showError(vboxDraft, "Không có bản nháp nào.");
        } else {
            for (AuctionItemDTO item : draftItems) if (vboxDraft != null) vboxDraft.getChildren().add(createItemCard(item));
        }
    }

    /**
     * Tạo card hiển thị một sản phẩm.
     * - Thumbnail: ảnh thật từ imageUrls (fallback về emoji nếu không có).
     * - Click vào card: load detail rồi chuyển sang AuctionDetailView.
     */
    private HBox createItemCard(AuctionItemDTO item) {
        // --- Badge trạng thái ---
        boolean isDraft   = isDraftItem(item);
        boolean isActive  = !isDraft && (item.getStatus() == SessionStatus.ACTIVE || item.getStatus() == SessionStatus.UPCOMING);
        
        String badgeText;
        String badgeBg;
        String badgeColor;
        
        if (isDraft) {
            badgeText = "BẢN NHÁP";
            badgeBg = "rgba(245, 158, 11, 0.1)";
            badgeColor = "#d97706";
        } else if (item.getStatus() == SessionStatus.UPCOMING) {
            badgeText = "CHUẨN BỊ ĐẤU GIÁ";
            badgeBg = "rgba(245, 158, 11, 0.1)"; 
            badgeColor = "#d97706";
        } else if (item.getStatus() == SessionStatus.ACTIVE) {
            badgeText = "ĐANG ĐẤU GIÁ";
            badgeBg = "rgba(34, 197, 94, 0.1)";
            badgeColor = "#16a34a";
        } else {
            badgeText = "ĐÃ KẾT THÚC";
            badgeBg = "rgba(148, 163, 184, 0.1)";
            badgeColor = "#64748b";
        }

        // Tên sản phẩm + badge
        Label lblName = new Label(item.getTitle());
        lblName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label lblBadge = new Label(badgeText);
        lblBadge.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px;" +
                        "-fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 10;",
                badgeBg, badgeColor));

        HBox hboxTitle = new HBox(10, lblName, lblBadge);
        hboxTitle.setAlignment(Pos.CENTER_LEFT);

        // Danh mục + tình trạng
        String categoryStr  = item.getCategory()  != null ? item.getCategory().name()  : "Khác";
        String conditionStr = item.getCondition()  != null ? item.getCondition().name() : "USED";
        Label lblMeta = new Label("Danh mục: " + categoryStr + "  |  Tình trạng: " + conditionStr);
        lblMeta.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");

        // Giá
        Label lblPriceTitle = new Label("Giá hiện tại: ");
        lblPriceTitle.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
        Label lblPrice = new Label(item.getCurrentPrice() != null ? UIFormatter.formatPrice(item.getCurrentPrice()) : "Chưa thiết lập giá");
        lblPrice.setStyle("-fx-text-fill: #f97316; -fx-font-size: 15px; -fx-font-weight: bold;");

        HBox hboxPrice = new HBox(20, lblPriceTitle, lblPrice);
        hboxPrice.setStyle("-fx-padding: 4 0 0 0;");

        VBox infoBox = new VBox(6, hboxTitle, lblMeta, hboxPrice);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // --- Thumbnail ảnh sản phẩm ---
        StackPane iconPane = buildThumbnail(item);

        // --- Card tổng ---
        HBox card = new HBox(20, iconPane, infoBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20));
        card.setCursor(Cursor.HAND);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2);" +
                        "-fx-border-color: #f1f5f9; -fx-border-radius: 16;" +
                        (isActive ? "" : "-fx-opacity: 0.8;")
        );

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #fff7ed; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(249,115,22,0.15), 12, 0, 0, 3);" +
                        "-fx-border-color: #f97316; -fx-border-radius: 16;" +
                        (isActive ? "" : "-fx-opacity: 0.8;")
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2);" +
                        "-fx-border-color: #f1f5f9; -fx-border-radius: 16;" +
                        (isActive ? "" : "-fx-opacity: 0.8;")
        ));

        // --- Click: load detail rồi navigate sang AuctionDetailView ---
        card.setOnMouseClicked(e -> {
            if (isDraft) {
                vn.edu.vnu.uet.group8.client.model.ClientModel.getInstance().setCurrentAuctionItem(item);
                if (MainController.getInstance() != null) MainController.getInstance().loadContentView("CreateItem.fxml");
            } else if (item.getStatus() == SessionStatus.UPCOMING) {
                vn.edu.vnu.uet.group8.client.util.AlertUtil.showInfo("Sản phẩm đang chuẩn bị đấu giá.");
            } else {
                navigateToDetail(item.getItemId());
            }
        });

        return card;
    }

    /**
     * Tạo StackPane thumbnail: dùng ImageView nếu có ảnh, fallback về emoji.
     */
    private StackPane buildThumbnail(AuctionItemDTO item) {
        StackPane pane = new StackPane();
        pane.setPrefSize(100, 100);
        pane.setMinSize(100, 100);
        pane.setMaxSize(100, 100);
        pane.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12;");

        List<String> urls = item.getImageUrls();
        if (urls != null && !urls.isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image(urls.get(0), 100, 100, true, true, true));
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
                // Bo góc ảnh bằng clip
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(100, 100);
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                imageView.setClip(clip);
                pane.getChildren().add(imageView);
            } catch (Exception ex) {
                pane.getChildren().add(fallbackIcon());
            }
        } else {
            pane.getChildren().add(fallbackIcon());
        }

        return pane;
    }

    /** Fallback khi không có ảnh */
    private Label fallbackIcon() {
        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 30px;");
        return icon;
    }

    /**
     * Gọi AuctionService.loadDetail() để set CurrentAuctionItem trong ClientModel,
     * sau đó chuyển sang trang AuctionDetailView.
     */
    private void navigateToDetail(int itemId) {
        AuctionService.loadDetail(itemId, detail -> Platform.runLater(() -> {
            if (MainController.getInstance() != null) {
                MainController.getInstance().loadContentView("AuctionDetailView.fxml");
            }
        }));
    }

    /** Hiện thông báo rỗng / lỗi vào một VBox */
    private void showError(VBox target, String message) {
        Label lbl = new Label(message);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 15px; -fx-padding: 30 0;");
        target.getChildren().setAll(lbl);
    }

    /** Xử lý chuyển đổi Tab */
    private void switchTab(Button selectedButton, VBox selectedVBox) {
        btnAll.setStyle(INACTIVE_STYLE);
        btnActive.setStyle(INACTIVE_STYLE);
        btnCompleted.setStyle(INACTIVE_STYLE);
        if (btnDraft != null) btnDraft.setStyle(INACTIVE_STYLE);
        selectedButton.setStyle(ACTIVE_STYLE);

        vboxAll.setVisible(false);       vboxAll.setManaged(false);
        vboxActive.setVisible(false);    vboxActive.setManaged(false);
        vboxCompleted.setVisible(false); vboxCompleted.setManaged(false);
        if (vboxDraft != null) { vboxDraft.setVisible(false); vboxDraft.setManaged(false); }

        selectedVBox.setVisible(true);
        selectedVBox.setManaged(true);
    }
}