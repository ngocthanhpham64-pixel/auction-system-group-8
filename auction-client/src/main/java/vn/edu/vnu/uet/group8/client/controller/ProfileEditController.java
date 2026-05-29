package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.UserService;
import vn.edu.vnu.uet.group8.client.util.AlertUtil;
import vn.edu.vnu.uet.group8.client.util.ModalUtil;
import vn.edu.vnu.uet.group8.client.util.PopupUtil;
import vn.edu.vnu.uet.group8.client.util.SceneManager;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.client.util.UIFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.scene.Node;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import vn.edu.vnu.uet.group8.common.dto.response.LoginResponse;
import java.math.BigDecimal;

public class ProfileEditController {

    // Navigation Top & Sidebar
    @FXML private Label lblAvatarLetter;
    @FXML private Button btnHome;
    @FXML private Button btnLogout;
    
    // Sidebar Buttons
    @FXML private Button btnBidNow;
    @FXML private Button btnMyBids;
    @FXML private Button btnMyProducts;

    // Readonly Labels (Chế độ xem)
    @FXML private Label lblFullName;
    @FXML private Label lblPhone;
    @FXML private Label lblAddress;
    @FXML private Label lblUsername;
    @FXML private Label lblEmail;
    
    // Info Labels (Thống kê)
    @FXML private Label lblUserId;
    @FXML private Label lblBalance;
    @FXML private Label lblSellerRating;
    @FXML private Label lblTotalBidsPlaced;
    @FXML private Label lblTotalItemsSold;
    @FXML private Label lblCreatedAt;
    @FXML private Label lblLastLogin;
    @FXML private Label lblAvatarInitials;
    @FXML private ImageView imgAvatar;
    private String newAvatarBase64;

    // Editable TextFields (Chế độ sửa)
    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;

    // Action Buttons
    @FXML private Button btnEditProfile;
    @FXML private Button btnBack;
    @FXML private Button btnSaveProfile;

    // Listeners để tránh memory leak
    private ChangeListener<LoginResponse> userListener;
    private ChangeListener<String> avatarListener;
    private ChangeListener<BigDecimal> balanceListener;

    @FXML
    public void initialize() {
        ClientModel model = ClientModel.getInstance();
        
        if (imgAvatar != null) {
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(25, 25, 25);
            imgAvatar.setClip(clip);
        }
        
        // 1. Ràng buộc Header + Sidebar
        userListener = (obs, oldUser, newUser) -> {
            if (newUser != null) {
                if (lblAvatarLetter != null) lblAvatarLetter.setText(SessionManager.getAvatarText());
                if (lblAvatarInitials != null) lblAvatarInitials.setText(SessionManager.getAvatarText());
            }
        };
        model.currentUserProperty().addListener(new WeakChangeListener<>(userListener));

        avatarListener = (obs, oldUrl, newUrl) -> {
            updateAvatarView(newUrl);
        };
        model.avatarUrlProperty().addListener(new WeakChangeListener<>(avatarListener));

        // Bind số dư để tự động nảy số khi nạp/rút tiền thành công
        if (lblBalance != null) lblBalance.setText(UIFormatter.formatPrice(model.getBalance()));
        balanceListener = (obs, oldVal, newVal) -> {
            if (lblBalance != null) lblBalance.setText(UIFormatter.formatPrice(newVal));
        };
        model.balanceProperty().addListener(new WeakChangeListener<>(balanceListener));

        if (btnHome != null) btnHome.setOnAction(e -> SceneManager.switchTo(SceneManager.VIEW_MAIN, "Auctiva - Live Online Auction"));
        if (btnLogout != null) btnLogout.setOnAction(e -> SessionManager.logout());
        
        // Sidebar actions
        if (btnBidNow != null) btnBidNow.setOnAction(e -> AlertUtil.showInfo("Nút này sẽ chuyển sang màn hình tham gia Đấu giá trực tiếp."));
        if (btnMyBids != null) btnMyBids.setOnAction(e -> ModalUtil.showModal("LỊCH SỬ ĐẤU GIÁ", "AuctionHistoryContent.fxml"));
        if (btnMyProducts != null) btnMyProducts.setOnAction(e -> ModalUtil.showModal("QUẢN LÝ KHO HÀNG", "SalesManagementContent.fxml"));

        // Che đi các chữ mặc định của FXML lúc vừa bật view lên (tránh chớp "Hà Nội, Việt Nam")
        if (lblFullName != null) lblFullName.setText("Đang tải...");
        if (lblPhone != null) lblPhone.setText("Đang tải...");
        if (lblAddress != null) lblAddress.setText("Đang tải...");

        // 2. Tải Profile chi tiết từ server
        loadFreshData();
    }

    private void loadFreshData() {
        UserService.loadProfile(profile -> Platform.runLater(() -> {
            if (profile != null) {
                // Đổ dữ liệu Readonly tĩnh
                lblUsername.setText(profile.getUsername());
                lblEmail.setText(profile.getEmail());
                lblUserId.setText("#USR-" + profile.getUserId());
                lblSellerRating.setText(profile.getSellerRating() != null ? profile.getSellerRating() + " ★" : "0.0 ★");
                lblTotalBidsPlaced.setText(profile.getTotalBidsPlaced() + " lượt");
                lblTotalItemsSold.setText(profile.getTotalItemsSold() + " sản phẩm");
                lblCreatedAt.setText(UIFormatter.formatInstant(profile.getCreatedAt()));
                lblLastLogin.setText(UIFormatter.formatInstant(profile.getLastLogin()));

                // Đổ dữ liệu có thể sửa (vào label)
                lblFullName.setText(profile.getFullName() != null && !profile.getFullName().isEmpty() ? profile.getFullName() : "Chưa cập nhật");
                lblPhone.setText(profile.getPhone() != null && !profile.getPhone().isEmpty() ? profile.getPhone() : "Chưa cập nhật");
                lblAddress.setText(profile.getAddress() != null && !profile.getAddress().trim().isEmpty() ? profile.getAddress() : "Chưa cập nhật");

                // Cập nhật hiển thị ảnh đại diện
                updateAvatarView(profile.getAvatarUrl());
            }
        }));
    }

    private void updateAvatarView(String avatarUrl) {
        UIFormatter.setCircularAvatar(imgAvatar, lblAvatarInitials, avatarUrl, 50.0);
    }

    @FXML
    public void onEditProfileClick(ActionEvent event) {
        // 1. Tắt chế độ Label hiển thị
        lblFullName.setVisible(false); lblFullName.setManaged(false);
        lblPhone.setVisible(false); lblPhone.setManaged(false);
        lblAddress.setVisible(false); lblAddress.setManaged(false);

        // 2. Bật TextField nhập liệu và chép giá trị cũ sang
        txtFullName.setVisible(true); txtFullName.setManaged(true);
        String name = lblFullName.getText();
        txtFullName.setText(name.equals("Chưa cập nhật") || name.equals("Đang tải...") ? "" : name);
        
        txtPhone.setVisible(true); txtPhone.setManaged(true);
        String phone = lblPhone.getText();
        txtPhone.setText(phone.equals("Chưa cập nhật") || phone.equals("Đang tải...") ? "" : phone);
        
        txtAddress.setVisible(true); txtAddress.setManaged(true);
        String addr = lblAddress.getText();
        txtAddress.setText(addr.equals("Chưa cập nhật") || addr.equals("Đang tải...") ? "" : addr);

        // 3. Chuyển thao tác nút
        btnSaveProfile.setDisable(false);
        btnEditProfile.setDisable(true);
    }

    @FXML
    public void onSaveProfileClick(ActionEvent event) {
        String newFullName = txtFullName.getText().trim();
        String newPhone = txtPhone.getText().trim();
        String newAddress = txtAddress.getText().trim();

        if (newFullName.isEmpty()) {
            AlertUtil.showError("Họ và tên không được bỏ trống!");
            return;
        }

        btnSaveProfile.setDisable(true);
        btnSaveProfile.setText("Đang lưu...");

        UserService.updateProfile(newFullName, newPhone, newAddress, newAvatarBase64, success -> Platform.runLater(() -> {
            btnSaveProfile.setText("Xác nhận hoàn tất");
            if (success) {
                newAvatarBase64 = null;
                // Cập nhật nhãn ngay lập tức để đồng bộ
                lblFullName.setText(newFullName.isEmpty() ? "Chưa cập nhật" : newFullName);
                lblPhone.setText(newPhone.isEmpty() ? "Chưa cập nhật" : newPhone);
                lblAddress.setText(newAddress.isEmpty() ? "Chưa cập nhật" : newAddress);

                // Tắt chế độ sửa
                txtFullName.setVisible(false); txtFullName.setManaged(false);
                txtPhone.setVisible(false); txtPhone.setManaged(false);
                txtAddress.setVisible(false); txtAddress.setManaged(false);
                lblFullName.setVisible(true); lblFullName.setManaged(true);
                lblPhone.setVisible(true); lblPhone.setManaged(true);
                lblAddress.setVisible(true); lblAddress.setManaged(true);
                btnSaveProfile.setDisable(true);
                btnEditProfile.setDisable(false);

                ModalUtil.showModal("CẬP NHẬT THÀNH CÔNG", "SuccessContent.fxml");
            } else {
                AlertUtil.showError("Cập nhật thông tin thất bại!");
                btnSaveProfile.setDisable(false);
            }
        }));
    }

    @FXML 
    public void onBackClick(ActionEvent event) {
        if (MainController.getInstance() != null) {
            MainController.getInstance().switchView("UserDashboard.fxml", "PROFILE");
        } else {
            SceneManager.switchTo("UserDashboard.fxml", "Auctiva - User Dashboard");
        }
    }
    @FXML
    public void onAvatarClick(MouseEvent event) {
        // Tự động bật chế độ Edit nếu chưa bật
        if (btnSaveProfile != null && btnSaveProfile.isDisable()) {
            onEditProfileClick(null);
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Hình ảnh (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
        );
        
        File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Đọc dữ liệu file và chuyển thành Base64 thô
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                newAvatarBase64 = Base64.getEncoder().encodeToString(fileContent);

                // Preview ảnh cục bộ lập tức
                Image localImg = new Image(selectedFile.toURI().toString());
                UIFormatter.applyImageToImageView(imgAvatar, lblAvatarInitials, localImg, 50.0);
            } catch (Exception e) {
                AlertUtil.showError("Không thể đọc tệp ảnh: " + e.getMessage());
            }
        }
    }
    
    @FXML public void onNavFavoritesClick(MouseEvent event) {
        PopupUtil.showPopup((javafx.scene.Node) event.getSource(), "FavoriteContent.fxml");
    }
    
    @FXML public void onNavNotificationsClick(MouseEvent event) {
        PopupUtil.showPopup((javafx.scene.Node) event.getSource(), "NotificationContent.fxml");
    }

    // Bổ sung điều hướng cho các khối hộp thống kê dưới góc màn hình
    @FXML
    public void onDepositClick() {
        ModalUtil.showModal("NẠP TIỀN VÀO VÍ ĐIỆN TỬ", "DepositContent.fxml");
    }
    @FXML
    public void onWithdrawClick() {
        ModalUtil.showModal("YÊU CẦU RÚT TIỀN", "WithdrawContent.fxml");
    }
    @FXML
    public void onViewTransactionHistoryClick() {
        ModalUtil.showModal("LỊCH SỬ BIẾN ĐỘNG SỐ DƯ", "ListContainer.fxml");
    }
    @FXML
    public void onChangePasswordClick() {
        ModalUtil.showModal("ĐỔI MẬT KHẨU", "ChangePasswordContent.fxml");
    }
    @FXML
    public void onViewReviewHistoryClick() {
        ModalUtil.showModal("ĐÁNH GIÁ TỪ NGƯỜI MUA", "BuyerReviewsContent.fxml");
    }
    @FXML
    public void onViewPurchaseHistoryClick() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().switchView("AuctionHistoryContent.fxml", "MY_AUCTIONS");
        } else {
            ModalUtil.showModal("NHẬT KÝ MUA HÀNG", "PurchaseHistoryContent.fxml");
        }
    }
    @FXML
    public void onViewSalesHistoryClick() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().switchView("SalesManagementContent.fxml", "SELLER");
        } else {
            ModalUtil.showModal("QUẢN LÝ BÁN HÀNG", "SalesManagementContent.fxml");
        }
    }
}
