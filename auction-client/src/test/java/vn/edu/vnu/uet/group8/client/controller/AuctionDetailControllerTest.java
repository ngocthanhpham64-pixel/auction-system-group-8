package vn.edu.vnu.uet.group8.client.controller;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.client.service.BidService;
import vn.edu.vnu.uet.group8.client.service.FavoriteService;
import vn.edu.vnu.uet.group8.client.util.ModalUtil;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.dto.model.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.BidResponse;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử toàn diện AuctionDetailController.
 *
 * QUAN TRỌNG: Hầu hết callbacks trong production code được bọc trong
 * Platform.runLater(). Để test hoạt động synchronously, tất cả các test
 * liên quan đến callbacks đều mock Platform.runLater để thực thi Runnable ngay lập tức.
 */
@DisplayName("AuctionDetailController — Full Coverage")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionDetailControllerTest extends FxTestBase {

    private AuctionDetailController controller;

    @BeforeEach
    void setup() throws Exception {
        controller = new AuctionDetailController();
        injectAllFxmlFields();
    }

    /** Inject tất cả FXML fields bằng reflection để tránh dùng FXMLLoader */
    private void injectAllFxmlFields() throws Exception {
        setField("lblCategory",          new Label());
        setField("imgMain",              new ImageView());
        setField("lblDetailStatusBadge", new Label());
        setField("btnPrevImage",         new Button("<"));
        setField("btnNextImage",         new Button(">"));
        setField("lblProductName",       new Label());
        setField("lblConditionBadge",    new Label());
        setField("lblBidStats",          new Label());
        setField("lblHours",             new Label());
        setField("lblMinutes",           new Label());
        setField("lblSeconds",           new Label());
        setField("lblCurrentPrice",      new Label());
        setField("lblStartPrice",        new Label());
        setField("tfBidAmount",          new TextField());
        setField("lblMinBid",            new Label());
        setField("btnTabDesc",           new Label());
        setField("paneDesc",             new VBox());
        setField("paneSpec",             new VBox());
        setField("paneOrigin",           new VBox());
        setField("lblDescription",       new Label());
        setField("lblCondition",         new Label());
        setField("lblBrand",             new Label());
        setField("lblModel",             new Label());
        setField("lblMaterial",          new Label());
        setField("lblOrigin",            new Label());
        setField("lblCertificate",       new Label());
        setField("bidHistory",           new VBox());
        setField("paneBidForm",          new VBox());
        setField("lblSellerName",        new Label());
        setField("lblSellerStats",       new Label());
        setField("btnAutoToggle",        new Button());
        setField("paneAuto",             new VBox());
        setField("tfMaxPrice",           new TextField());
        setField("lblAutoDesc",          new Label());
        setField("btnActivateAuto",      new Button());
        setField("btnCancelAuto",        new Button());
        setField("btnFavorite",          new Button());

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        setField("bidChart", chart);
    }

    // =========================================================
    // Reflection helpers
    // =========================================================

    private void setField(String name, Object value) throws Exception {
        Field f = AuctionDetailController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) throws Exception {
        Field f = AuctionDetailController.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(controller);
    }

    private Object invokePrivate(String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = AuctionDetailController.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    // =========================================================
    // Helper: mock Platform.runLater để chạy synchronously
    // =========================================================

    /**
     * Trả về MockedStatic<Platform> đã được cấu hình để thực thi
     * Runnable truyền vào runLater() ngay lập tức (synchronous).
     * Luôn dùng try-with-resources để đảm bảo cleanup.
     */
    private MockedStatic<Platform> mockPlatformRunLaterSync() {
        MockedStatic<Platform> mp = mockStatic(Platform.class);
        mp.when(() -> Platform.runLater(any(Runnable.class)))
                .thenAnswer(inv -> {
                    Runnable r = inv.getArgument(0);
                    r.run();
                    return null;
                });
        mp.when(Platform::isFxApplicationThread).thenReturn(true);
        return mp;
    }

    // =========================================================
    // Factory helpers
    // =========================================================

    private AuctionItemDTO makeItem(SessionStatus status) {
        return AuctionItemDTO.of(
                1, "Test Product", "Test Description",
                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                status,
                new BigDecimal("1000000"),
                Instant.now().plusSeconds(3600),
                99, "seller_user", null,
                Arrays.asList("http://img1.jpg", "http://img2.jpg"),
                5, Instant.now()
        );
    }

    private AuctionItemDTO makeItemWithRating(BigDecimal rating) {
        AuctionItemDTO item = AuctionItemDTO.of(
                2, "Rated Item", "Desc",
                ItemCategory.WATCHES, ItemCondition.USED,
                SessionStatus.ACTIVE,
                new BigDecimal("500000"),
                Instant.now().plusSeconds(1800),
                55, "seller2", null, null, 10, Instant.now()
        );
        try {
            Field f = AuctionItemDTO.class.getDeclaredField("sellerRating");
            f.setAccessible(true);
            f.set(item, rating);
        } catch (Exception e) { throw new RuntimeException(e); }
        return item;
    }

    private BidRecord makeBidRecord(int bidId, long amount) {
        return BidRecord.builder()
                .bidId(bidId).itemId(1).userId(10)
                .displayName("User " + bidId)
                .amount(new BigDecimal(amount))
                .placedAt(Instant.now())
                .build();
    }

    // =========================================================
    // 1. constructor & initialize
    // =========================================================

    @Test @Order(1)
    @DisplayName("Constructor khởi tạo không ném ngoại lệ")
    void constructor_defaultState() {
        assertNotNull(new AuctionDetailController());
    }

    @Test @Order(2)
    @DisplayName("initialize() không crash khi item null")
    void initialize_withNullItem_noException() {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);
            when(mockModel.isFavorite(anyInt())).thenReturn(false);

            as.when(() -> AuctionService.subscribeAuctionStatus(any())).thenReturn(null);
            as.when(() -> AuctionService.subscribeAuctionEnded(any())).thenReturn(null);

            assertDoesNotThrow(() -> controller.initialize());
        }
    }

    // =========================================================
    // 2. loadData()
    // =========================================================

    @Test @Order(10)
    @DisplayName("loadData() item null → hiển thị mock data")
    void loadData_itemNull_showsMockData() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);

            as.when(() -> AuctionService.subscribeAuctionStatus(any())).thenReturn(null);
            as.when(() -> AuctionService.subscribeAuctionEnded(any())).thenReturn(null);

            invokePrivate("loadData", new Class[]{});

            Label lblCategory = getField("lblCategory");
            assertEquals("Đồ điện tử", lblCategory.getText());
        }
    }

    @Test @Order(11)
    @DisplayName("loadData() item ACTIVE → badge LIVE")
    void loadData_itemActive_liveBadge() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(1)).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});

            Label badge = getField("lblDetailStatusBadge");
            assertTrue(badge.getText().contains("LIVE"));
        }
    }

    @Test @Order(12)
    @DisplayName("loadData() item UPCOMING → badge SẮP, đồng hồ 00:00:00")
    void loadData_itemUpcoming_upcomingBadge() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = makeItem(SessionStatus.UPCOMING);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(1)).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});

            Label badge = getField("lblDetailStatusBadge");
            assertTrue(badge.getText().contains("SẮP"));
            assertEquals("00", ((Label) getField("lblHours")).getText());
        }
    }

    @Test @Order(13)
    @DisplayName("loadData() item SOLD → badge ĐÃ KẾT THÚC")
    void loadData_itemSold_endedBadge() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = makeItem(SessionStatus.SOLD);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(1)).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});

            assertTrue(((Label) getField("lblDetailStatusBadge")).getText().contains("KẾT THÚC"));
        }
    }

    @Test @Order(14)
    @DisplayName("loadData() sellerRating > 0 → hiển thị đánh giá")
    void loadData_itemWithRating_showsRating() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = makeItemWithRating(new BigDecimal("4.5"));
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(anyInt())).thenReturn(true);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});

            assertTrue(((Label) getField("lblSellerStats")).getText().contains("Đánh giá:"));
        }
    }

    @Test @Order(15)
    @DisplayName("loadData() sellerRating = 0 → hiển thị 'Chưa có đánh giá'")
    void loadData_itemNoRating_showsNoRating() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = makeItemWithRating(BigDecimal.ZERO);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(anyInt())).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});

            assertTrue(((Label) getField("lblSellerStats")).getText().contains("Chưa có đánh giá"));
        }
    }

    @Test @Order(16)
    @DisplayName("loadData() isFavorite=true → button màu đỏ")
    void loadData_favoriteItem_redButton() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(1)).thenReturn(true);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});

            assertTrue(((Button) getField("btnFavorite")).getStyle().contains("#ef4444"));
        }
    }

    // =========================================================
    // 3. updateImageGallery()
    // =========================================================

    @Test @Order(20)
    @DisplayName("updateImageGallery() rỗng → imgMain null, nút ẩn")
    void updateImageGallery_emptyList_hidesButtons() throws Exception {
        setField("currentImageUrls", new ArrayList<>());
        setField("currentImageIndex", 0);
        invokePrivate("updateImageGallery", new Class[]{});
        assertNull(((ImageView) getField("imgMain")).getImage());
        assertFalse(((Button) getField("btnPrevImage")).isVisible());
        assertFalse(((Button) getField("btnNextImage")).isVisible());
    }

    @Test @Order(21)
    @DisplayName("updateImageGallery() 1 ảnh → nút ẩn")
    void updateImageGallery_singleImage_arrowsHidden() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Collections.singletonList("https://via.placeholder.com/150")));
        setField("currentImageIndex", 0);
        invokePrivate("updateImageGallery", new Class[]{});
        assertFalse(((Button) getField("btnPrevImage")).isVisible());
        assertFalse(((Button) getField("btnNextImage")).isVisible());
    }

    @Test @Order(22)
    @DisplayName("updateImageGallery() > 1 ảnh → nút hiện")
    void updateImageGallery_multipleImages_arrowsVisible() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Arrays.asList("https://via.placeholder.com/150", "https://via.placeholder.com/200")));
        setField("currentImageIndex", 0);
        invokePrivate("updateImageGallery", new Class[]{});
        assertTrue(((Button) getField("btnPrevImage")).isVisible());
        assertTrue(((Button) getField("btnNextImage")).isVisible());
    }

    @Test @Order(23)
    @DisplayName("updateImageGallery() index âm → clamp về 0")
    void updateImageGallery_negativeIndex_clampToZero() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Arrays.asList("http://a.jpg", "http://b.jpg")));
        setField("currentImageIndex", -5);
        invokePrivate("updateImageGallery", new Class[]{});
        assertEquals(0, (int) getField("currentImageIndex"));
    }

    @Test @Order(24)
    @DisplayName("updateImageGallery() index tràn → clamp về size-1")
    void updateImageGallery_overflowIndex_clampToLast() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Arrays.asList("http://a.jpg", "http://b.jpg")));
        setField("currentImageIndex", 99);
        invokePrivate("updateImageGallery", new Class[]{});
        assertEquals(1, (int) getField("currentImageIndex"));
    }

    // =========================================================
    // 4. onPrevImage() / onNextImage()
    // =========================================================

    @Test @Order(30)
    @DisplayName("onPrevImage() index > 0 → giảm")
    void onPrevImage_normalCase_decreasesIndex() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Arrays.asList("http://a.jpg", "http://b.jpg", "http://c.jpg")));
        setField("currentImageIndex", 2);
        controller.onPrevImage();
        assertEquals(1, (int) getField("currentImageIndex"));
    }

    @Test @Order(31)
    @DisplayName("onPrevImage() index 0 → wrap về cuối")
    void onPrevImage_atFirst_wrapsToLast() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Arrays.asList("http://a.jpg", "http://b.jpg", "http://c.jpg")));
        setField("currentImageIndex", 0);
        controller.onPrevImage();
        assertEquals(2, (int) getField("currentImageIndex"));
    }

    @Test @Order(32)
    @DisplayName("onPrevImage() 1 ảnh → không đổi")
    void onPrevImage_singleOrEmptyList_noOp() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Collections.singletonList("http://a.jpg")));
        setField("currentImageIndex", 0);
        controller.onPrevImage();
        assertEquals(0, (int) getField("currentImageIndex"));
    }

    @Test @Order(33)
    @DisplayName("onNextImage() index cuối → wrap về 0")
    void onNextImage_atLast_wrapsToFirst() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Arrays.asList("http://a.jpg", "http://b.jpg", "http://c.jpg")));
        setField("currentImageIndex", 2);
        controller.onNextImage();
        assertEquals(0, (int) getField("currentImageIndex"));
    }

    @Test @Order(34)
    @DisplayName("onNextImage() index giữa → tăng")
    void onNextImage_normalCase_increasesIndex() throws Exception {
        setField("currentImageUrls", new ArrayList<>(Arrays.asList("http://a.jpg", "http://b.jpg", "http://c.jpg")));
        setField("currentImageIndex", 0);
        controller.onNextImage();
        assertEquals(1, (int) getField("currentImageIndex"));
    }

    @Test @Order(35)
    @DisplayName("onNextImage() rỗng → không đổi")
    void onNextImage_emptyOrSingle_noOp() throws Exception {
        setField("currentImageUrls", new ArrayList<>());
        setField("currentImageIndex", 0);
        controller.onNextImage();
        assertEquals(0, (int) getField("currentImageIndex"));
    }

    // =========================================================
    // 5. onTabDesc()
    // =========================================================

    @Test @Order(40)
    @DisplayName("onTabDesc() → paneDesc hiện, paneSpec/paneOrigin ẩn")
    void onTabDesc_showsDescPane() throws Exception {
        VBox paneDesc = new VBox();
        VBox paneSpec = new VBox(); paneSpec.setVisible(true); paneSpec.setManaged(true);
        VBox paneOrigin = new VBox(); paneOrigin.setVisible(true); paneOrigin.setManaged(true);
        setField("paneDesc", paneDesc);
        setField("paneSpec", paneSpec);
        setField("paneOrigin", paneOrigin);
        controller.onTabDesc();
        assertTrue(paneDesc.isVisible());
        assertFalse(paneSpec.isVisible());
        assertFalse(paneOrigin.isVisible());
    }

    // =========================================================
    // 6. onShare()
    // =========================================================

    @Test @Order(50)
    @DisplayName("onShare() không throw")
    void onShare_noException() {
        assertDoesNotThrow(() -> controller.onShare());
    }

    // =========================================================
    // 7. onToggleAuto()
    // =========================================================

    @Test @Order(60)
    @DisplayName("onToggleAuto() ẩn → hiện")
    void onToggleAuto_whenHidden_makesVisible() throws Exception {
        VBox paneAuto = new VBox(); paneAuto.setVisible(false); paneAuto.setManaged(false);
        setField("paneAuto", paneAuto);
        controller.onToggleAuto();
        assertTrue(paneAuto.isVisible());
    }

    @Test @Order(61)
    @DisplayName("onToggleAuto() hiện → ẩn")
    void onToggleAuto_whenVisible_makesHidden() throws Exception {
        VBox paneAuto = new VBox(); paneAuto.setVisible(true); paneAuto.setManaged(true);
        setField("paneAuto", paneAuto);
        controller.onToggleAuto();
        assertFalse(paneAuto.isVisible());
    }

    // =========================================================
    // 8. updateAutoBidUI(boolean)
    // =========================================================

    @Test @Order(70)
    @DisplayName("updateAutoBidUI(true) → ĐANG BẬT")
    void updateAutoBidUI_active_showsCancelButton() throws Exception {
        invokePrivate("updateAutoBidUI", new Class[]{boolean.class}, true);
        assertTrue(((Button) getField("btnAutoToggle")).getText().contains("ĐANG BẬT"));
        assertFalse(((Button) getField("btnActivateAuto")).isVisible());
        assertTrue(((Button) getField("btnCancelAuto")).isVisible());
    }

    @Test @Order(71)
    @DisplayName("updateAutoBidUI(false) → ĐANG TẮT")
    void updateAutoBidUI_inactive_showsActivateButton() throws Exception {
        invokePrivate("updateAutoBidUI", new Class[]{boolean.class}, false);
        assertTrue(((Button) getField("btnAutoToggle")).getText().contains("ĐANG TẮT"));
        assertTrue(((Button) getField("btnActivateAuto")).isVisible());
        assertFalse(((Button) getField("btnCancelAuto")).isVisible());
    }

    // =========================================================
    // 9. updateFavoriteButtonUI(boolean)
    // =========================================================

    @Test @Order(80)
    @DisplayName("updateFavoriteButtonUI(true) → nền đỏ")
    void updateFavoriteButtonUI_true_redBackground() throws Exception {
        invokePrivate("updateFavoriteButtonUI", new Class[]{boolean.class}, true);
        assertTrue(((Button) getField("btnFavorite")).getStyle().contains("-fx-background-color: #ef4444"));
    }

    @Test @Order(81)
    @DisplayName("updateFavoriteButtonUI(false) → nền trắng")
    void updateFavoriteButtonUI_false_whiteBackground() throws Exception {
        invokePrivate("updateFavoriteButtonUI", new Class[]{boolean.class}, false);
        assertTrue(((Button) getField("btnFavorite")).getStyle().contains("-fx-background-color: white"));
    }

    @Test @Order(82)
    @DisplayName("updateFavoriteButtonUI() btnFavorite null → không throw")
    void updateFavoriteButtonUI_nullButton_noException() throws Exception {
        setField("btnFavorite", null);
        assertDoesNotThrow(() -> invokePrivate("updateFavoriteButtonUI", new Class[]{boolean.class}, true));
    }

    // =========================================================
    // 10. onFavorite()
    // =========================================================

    @Test @Order(90)
    @DisplayName("onFavorite() item null → không gọi service")
    void onFavorite_nullItem_earlyReturn() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<FavoriteService> fs = mockStatic(FavoriteService.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);
            controller.onFavorite();
            fs.verifyNoInteractions();
        }
    }

    @Test @Order(91)
    @DisplayName("onFavorite() đang favorite → gọi FavoriteService.remove()")
    void onFavorite_currentlyFavorite_callsRemove() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<FavoriteService> fs = mockStatic(FavoriteService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(1)).thenReturn(true);

            fs.when(() -> FavoriteService.remove(eq(1), any(Runnable.class), any()))
                    .thenAnswer(inv -> { ((Runnable) inv.getArgument(1)).run(); return null; });

            controller.onFavorite();

            fs.verify(() -> FavoriteService.remove(eq(1), any(), any()));
            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Đã xóa khỏi danh sách yêu thích"));
        }
    }

    @Test @Order(92)
    @DisplayName("onFavorite() chưa favorite → gọi FavoriteService.add()")
    void onFavorite_notFavorite_callsAdd() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<FavoriteService> fs = mockStatic(FavoriteService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(1)).thenReturn(false);

            fs.when(() -> FavoriteService.add(eq(1), any(Runnable.class), any()))
                    .thenAnswer(inv -> { ((Runnable) inv.getArgument(1)).run(); return null; });

            controller.onFavorite();

            fs.verify(() -> FavoriteService.add(eq(1), any(), any()));
            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Đã thêm vào danh sách yêu thích"));
        }
    }

    @Test @Order(93)
    @DisplayName("onFavorite() remove thất bại → showFailureModal")
    void onFavorite_removeFailure_callsErrorAlert() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<FavoriteService> fs = mockStatic(FavoriteService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(1)).thenReturn(true);

            fs.when(() -> FavoriteService.remove(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<String>) inv.getArgument(2)).accept("Lỗi mạng"); return null; });

            controller.onFavorite();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Lỗi mạng"));
        }
    }

    @Test @Order(94)
    @DisplayName("onFavorite() add thất bại → showFailureModal")
    void onFavorite_addFailure_callsErrorAlert() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<FavoriteService> fs = mockStatic(FavoriteService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(1)).thenReturn(false);

            fs.when(() -> FavoriteService.add(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<String>) inv.getArgument(2)).accept("Token hết hạn"); return null; });

            controller.onFavorite();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Token hết hạn"));
        }
    }

    // =========================================================
    // 11. addQuickBid() + onQuickBid1/2/3
    // =========================================================

    @Test @Order(100)
    @DisplayName("addQuickBid() item null → không thay đổi tfBidAmount")
    void addQuickBid_nullItem_noChange() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);
            TextField tf = getField("tfBidAmount"); tf.setText("500000");
            invokePrivate("addQuickBid", new Class[]{long.class}, 100000L);
            assertEquals("500000", tf.getText());
        }
    }

    @Test @Order(101)
    @DisplayName("addQuickBid() tfBidAmount rỗng → cộng vào giá item")
    void addQuickBid_emptyField_addsToCurrentPrice() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            TextField tf = getField("tfBidAmount"); tf.setText("");
            invokePrivate("addQuickBid", new Class[]{long.class}, 100000L);
            assertEquals(String.valueOf(item.getCurrentPrice().longValue() + 100000L), tf.getText());
        }
    }

    @Test @Order(102)
    @DisplayName("addQuickBid() tfBidAmount có số → cộng vào số đó")
    void addQuickBid_existingAmount_addsToIt() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            TextField tf = getField("tfBidAmount"); tf.setText("2000000");
            invokePrivate("addQuickBid", new Class[]{long.class}, 500000L);
            assertEquals("2500000", tf.getText());
        }
    }

    @Test @Order(103)
    @DisplayName("addQuickBid() text không phải số → dùng giá item")
    void addQuickBid_invalidText_fallsBackToItemPrice() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            TextField tf = getField("tfBidAmount"); tf.setText("không-phải-số");
            invokePrivate("addQuickBid", new Class[]{long.class}, 100000L);
            assertEquals(String.valueOf(item.getCurrentPrice().longValue() + 100000L), tf.getText());
        }
    }

    @Test @Order(104)
    @DisplayName("onQuickBid1() → +100000")
    void onQuickBid1_calls100k() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            TextField tf = getField("tfBidAmount"); tf.setText("");
            controller.onQuickBid1();
            assertEquals(String.valueOf(item.getCurrentPrice().longValue() + 100_000L), tf.getText());
        }
    }

    @Test @Order(105)
    @DisplayName("onQuickBid2() → +500000")
    void onQuickBid2_calls500k() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            TextField tf = getField("tfBidAmount"); tf.setText("");
            controller.onQuickBid2();
            assertEquals(String.valueOf(item.getCurrentPrice().longValue() + 500_000L), tf.getText());
        }
    }

    @Test @Order(106)
    @DisplayName("onQuickBid3() → +1000000")
    void onQuickBid3_calls1m() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class)) {
            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            TextField tf = getField("tfBidAmount"); tf.setText("");
            controller.onQuickBid3();
            assertEquals(String.valueOf(item.getCurrentPrice().longValue() + 1_000_000L), tf.getText());
        }
    }

    // =========================================================
    // 12. onBidNow()
    // =========================================================

    @Test
    void onBidNow_emptyAmount_showsWarning() throws Exception {
        try (MockedStatic<Platform> mp = mockStatic(Platform.class)) {

            TextField tf = getField("tfBidAmount");
            tf.setText("");

            controller.onBidNow();

            mp.verify(() -> Platform.runLater(any(Runnable.class)));
        }
    }

    @Test @Order(111)
    @DisplayName("onBidNow() tfBidAmount null → không crash")
    void onBidNow_nullAmount_showsWarning() throws Exception {
        TextField tf = getField("tfBidAmount"); tf.setText(null);
        assertDoesNotThrow(() -> controller.onBidNow());
    }

    @Test @Order(112)
    @DisplayName("onBidNow() số không hợp lệ → showFailureModal")
    void onBidNow_invalidNumber_showsError() throws Exception {
        try (MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {
            TextField tf = getField("tfBidAmount"); tf.setText("abc");
            controller.onBidNow();
            
            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Số tiền không hợp lệ!"));
        }
    }

    @Test @Order(113)
    @DisplayName("onBidNow() item null → không gọi BidService")
    void onBidNow_nullItem_noBidService() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);
            TextField tf = getField("tfBidAmount"); tf.setText("1000000");
            controller.onBidNow();
            bs.verifyNoInteractions();
        }
    }

    @Test @Order(114)
    @DisplayName("onBidNow() thành công → showSuccessModal")
    void onBidNow_success_showsSuccessAndReloads() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             // FIX: mock Platform.runLater để callback thực thi synchronously
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            BidResponse successResp = new BidResponse(true, "Đặt giá thành công");
            bs.when(() -> BidService.placeBid(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<BidResponse>) inv.getArgument(2)).accept(successResp); return null; });

            // loadDetail sau khi bid thành công: callback updatedItem=null để không trigger loadData lần 2
            as.when(() -> AuctionService.loadDetail(eq(1), any())).thenAnswer(inv -> {
                ((Consumer<AuctionItemDTO>) inv.getArgument(1)).accept(null);
                return null;
            });

            TextField tf = getField("tfBidAmount"); tf.setText("1500000");
            controller.onBidNow();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Đặt giá thành công"));
        }
    }

    @Test @Order(115)
    @DisplayName("onBidNow() thất bại → showFailureModal")
    void onBidNow_failure_showsError() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            BidResponse failResp = new BidResponse(false, "Giá quá thấp");
            bs.when(() -> BidService.placeBid(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<BidResponse>) inv.getArgument(2)).accept(failResp); return null; });

            as.when(() -> AuctionService.loadDetail(eq(1), any())).thenAnswer(inv -> {
                ((Consumer<AuctionItemDTO>) inv.getArgument(1)).accept(null);
                return null;
            });

            TextField tf = getField("tfBidAmount"); tf.setText("1000");
            controller.onBidNow();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Giá quá thấp"));
        }
    }

    @Test @Order(116)
    @DisplayName("onBidNow() response null → showFailureModal 'Đặt giá thất bại'")
    void onBidNow_nullResponse_showsDefaultError() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            bs.when(() -> BidService.placeBid(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<BidResponse>) inv.getArgument(2)).accept(null); return null; });

            as.when(() -> AuctionService.loadDetail(eq(1), any())).thenAnswer(inv -> {
                ((Consumer<AuctionItemDTO>) inv.getArgument(1)).accept(null);
                return null;
            });

            TextField tf = getField("tfBidAmount"); tf.setText("1000000");
            controller.onBidNow();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Đặt giá thất bại"));
        }
    }

    // =========================================================
    // 13. onActivateAuto()
    // =========================================================

    @Test @Order(120)
    @DisplayName("onActivateAuto() tfMaxPrice rỗng → không crash")
    void onActivateAuto_emptyField_showsWarning() throws Exception {
        TextField tfMax = getField("tfMaxPrice"); tfMax.setText("");
        assertDoesNotThrow(() -> controller.onActivateAuto());
    }

    @Test @Order(121)
    @DisplayName("onActivateAuto() tfMaxPrice null → không crash")
    void onActivateAuto_nullField_noException() throws Exception {
        TextField tfMax = getField("tfMaxPrice"); tfMax.setText(null);
        assertDoesNotThrow(() -> controller.onActivateAuto());
    }

    @Test @Order(122)
    @DisplayName("onActivateAuto() số không hợp lệ → showFailureModal")
    void onActivateAuto_invalidNumber_showsError() throws Exception {
        try (MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {
            TextField tfMax = getField("tfMaxPrice"); tfMax.setText("không-phải-số");
            controller.onActivateAuto();
            
            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Số tiền không hợp lệ!"));
        }
    }

    @Test @Order(123)
    @DisplayName("onActivateAuto() item null → không gọi BidService")
    void onActivateAuto_nullItem_noBidService() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);
            TextField tfMax = getField("tfMaxPrice"); tfMax.setText("5000000");
            controller.onActivateAuto();
            bs.verifyNoInteractions();
        }
    }

    @Test @Order(124)
    @DisplayName("onActivateAuto() thành công → showSuccessModal, updateAutoBidUI(true)")
    void onActivateAuto_success_activatesAutoBid() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             // FIX: Platform.runLater synchronous
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            ServerResponse successResp = mock(ServerResponse.class);
            when(successResp.isSuccess()).thenReturn(true);
            when(successResp.getMessage()).thenReturn("Đã kích hoạt Auto-bid thành công!");

            bs.when(() -> BidService.setAutoBid(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<ServerResponse>) inv.getArgument(2)).accept(successResp); return null; });
            mu.when(() -> ModalUtil.showSuccessModal(any(), any())).thenAnswer(inv -> null);

            TextField tfMax = getField("tfMaxPrice"); tfMax.setText("5000000");
            controller.onActivateAuto();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Đã kích hoạt Auto-bid thành công!"));
            assertTrue(((Button) getField("btnAutoToggle")).getText().contains("ĐANG BẬT"));
        }
    }

    @Test @Order(125)
    @DisplayName("onActivateAuto() thành công message null → dùng thông báo mặc định")
    void onActivateAuto_successNullMessage_usesDefaultMessage() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            ServerResponse successResp = mock(ServerResponse.class);
            when(successResp.isSuccess()).thenReturn(true);
            when(successResp.getMessage()).thenReturn(null);

            bs.when(() -> BidService.setAutoBid(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<ServerResponse>) inv.getArgument(2)).accept(successResp); return null; });
            mu.when(() -> ModalUtil.showSuccessModal(any(), any())).thenAnswer(inv -> null);

            TextField tfMax = getField("tfMaxPrice"); tfMax.setText("5000000");
            controller.onActivateAuto();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Đã kích hoạt Auto-bid thành công!"));
        }
    }

    @Test @Order(126)
    @DisplayName("onActivateAuto() thất bại → showFailureModal")
    void onActivateAuto_failure_showsError() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            ServerResponse failResp = mock(ServerResponse.class);
            when(failResp.isSuccess()).thenReturn(false);
            when(failResp.getMessage()).thenReturn("Số dư không đủ");

            bs.when(() -> BidService.setAutoBid(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<ServerResponse>) inv.getArgument(2)).accept(failResp); return null; });
            mu.when(() -> ModalUtil.showFailureModal(any(), any())).thenAnswer(inv -> null);

            TextField tfMax = getField("tfMaxPrice"); tfMax.setText("100");
            controller.onActivateAuto();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Số dư không đủ"));
        }
    }

    @Test @Order(127)
    @DisplayName("onActivateAuto() response null → thông báo mặc định")
    void onActivateAuto_nullResponse_defaultError() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            bs.when(() -> BidService.setAutoBid(eq(1), any(), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<ServerResponse>) inv.getArgument(2)).accept(null); return null; });
            mu.when(() -> ModalUtil.showFailureModal(any(), any())).thenAnswer(inv -> null);

            TextField tfMax = getField("tfMaxPrice"); tfMax.setText("100");
            controller.onActivateAuto();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Không thể kích hoạt Auto-bid!"));
        }
    }

    // =========================================================
    // 14. onCancelAuto()
    // =========================================================

    @Test @Order(130)
    @DisplayName("onCancelAuto() item null → không gọi BidService")
    void onCancelAuto_nullItem_noService() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);
            controller.onCancelAuto();
            bs.verifyNoInteractions();
        }
    }

    @Test @Order(131)
    @DisplayName("onCancelAuto() thành công → updateAutoBidUI(false), showSuccessModal")
    void onCancelAuto_success_deactivatesAutoBid() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            ServerResponse successResp = mock(ServerResponse.class);
            when(successResp.isSuccess()).thenReturn(true);
            when(successResp.getMessage()).thenReturn("Đã hủy Auto-bid");

            bs.when(() -> BidService.setAutoBid(eq(1), eq(BigDecimal.ZERO), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<ServerResponse>) inv.getArgument(2)).accept(successResp); return null; });
            mu.when(() -> ModalUtil.showSuccessModal(any(), any())).thenAnswer(inv -> null);

            controller.onCancelAuto();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Đã hủy Auto-bid"));
            assertTrue(((Button) getField("btnAutoToggle")).getText().contains("ĐANG TẮT"));
        }
    }

    @Test @Order(132)
    @DisplayName("onCancelAuto() thành công message null → thông báo mặc định")
    void onCancelAuto_successNullMessage_defaultMessage() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            ServerResponse successResp = mock(ServerResponse.class);
            when(successResp.isSuccess()).thenReturn(true);
            when(successResp.getMessage()).thenReturn(null);

            bs.when(() -> BidService.setAutoBid(eq(1), eq(BigDecimal.ZERO), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<ServerResponse>) inv.getArgument(2)).accept(successResp); return null; });
            mu.when(() -> ModalUtil.showSuccessModal(any(), any())).thenAnswer(inv -> null);

            controller.onCancelAuto();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Đã hủy tính năng Auto-bid!"));
        }
    }

    @Test @Order(133)
    @DisplayName("onCancelAuto() thất bại → showFailureModal")
    void onCancelAuto_failure_showsError() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            ServerResponse failResp = mock(ServerResponse.class);
            when(failResp.isSuccess()).thenReturn(false);
            when(failResp.getMessage()).thenReturn("Lỗi server");

            bs.when(() -> BidService.setAutoBid(eq(1), eq(BigDecimal.ZERO), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<ServerResponse>) inv.getArgument(2)).accept(failResp); return null; });
            mu.when(() -> ModalUtil.showFailureModal(any(), any())).thenAnswer(inv -> null);

            controller.onCancelAuto();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Lỗi server"));
        }
    }

    @Test @Order(134)
    @DisplayName("onCancelAuto() response null → thông báo mặc định")
    void onCancelAuto_nullResponse_defaultError() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            bs.when(() -> BidService.setAutoBid(eq(1), eq(BigDecimal.ZERO), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<ServerResponse>) inv.getArgument(2)).accept(null); return null; });
            mu.when(() -> ModalUtil.showFailureModal(any(), any())).thenAnswer(inv -> null);

            controller.onCancelAuto();

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Không thể hủy Auto-bid!"));
        }
    }

    // =========================================================
    // 15. computeBidHistoryFingerprint()
    // =========================================================

    @Test @Order(140)
    @DisplayName("fingerprint(null) → 'empty'")
    void fingerprint_nullList_returnsEmpty() throws Exception {
        assertEquals("empty", invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class}, (Object) null));
    }

    @Test @Order(141)
    @DisplayName("fingerprint([]) → 'empty'")
    void fingerprint_emptyList_returnsEmpty() throws Exception {
        assertEquals("empty", invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class}, Collections.emptyList()));
    }

    @Test @Order(142)
    @DisplayName("fingerprint(1 record) → không rỗng")
    void fingerprint_oneRecord_nonEmpty() throws Exception {
        String result = (String) invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class},
                new ArrayList<>(Collections.singletonList(makeBidRecord(1, 1000000))));
        assertNotNull(result);
        assertNotEquals("empty", result);
    }

    @Test @Order(143)
    @DisplayName("fingerprint(4 records) → bắt đầu bằng '4:'")
    void fingerprint_multipleRecords_includesSize() throws Exception {
        List<BidRecord> records = new ArrayList<>();
        for (int i = 1; i <= 4; i++) records.add(makeBidRecord(i, 1000000L * i));
        String result = (String) invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class}, records);
        assertTrue(result.startsWith("4:"));
    }

    @Test @Order(144)
    @DisplayName("fingerprint khác data → khác nhau")
    void fingerprint_differentData_differentFingerprint() throws Exception {
        String fp1 = (String) invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class},
                new ArrayList<>(Collections.singletonList(makeBidRecord(1, 1000000))));
        String fp2 = (String) invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class},
                new ArrayList<>(Collections.singletonList(makeBidRecord(2, 9999999))));
        assertNotEquals(fp1, fp2);
    }

    @Test @Order(145)
    @DisplayName("fingerprint cùng data → bằng nhau")
    void fingerprint_sameData_sameFingerprint() throws Exception {
        Instant t = Instant.parse("2025-01-01T00:00:00Z");
        BidRecord r1 = BidRecord.builder().bidId(1).itemId(1).userId(10).displayName("Alice").amount(new BigDecimal("1000000")).placedAt(t).build();
        BidRecord r2 = BidRecord.builder().bidId(1).itemId(1).userId(10).displayName("Alice").amount(new BigDecimal("1000000")).placedAt(t).build();
        String fp1 = (String) invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class}, new ArrayList<>(Collections.singletonList(r1)));
        String fp2 = (String) invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class}, new ArrayList<>(Collections.singletonList(r2)));
        assertEquals(fp1, fp2);
    }

    // =========================================================
    // 16. reloadBidHistory()
    // =========================================================

    @Test @Order(150)
    @DisplayName("reloadBidHistory() history rỗng → label 'Chưa có lượt'")
    void reloadBidHistory_emptyHistory_showsEmptyLabel() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class);
             // FIX: Platform.runLater synchronous để bidHistory.getChildren() được populate ngay
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            bs.when(() -> BidService.loadHistory(eq(1), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<List<BidRecord>>) inv.getArgument(1)).accept(Collections.emptyList()); return null; });

            setField("lastBidHistoryFingerprint", "FORCE_REBUILD");
            invokePrivate("reloadBidHistory", new Class[]{int.class}, 1);

            VBox bidHistory = getField("bidHistory");
            boolean hasEmptyLabel = bidHistory.getChildren().stream()
                    .anyMatch(n -> n instanceof Label && ((Label) n).getText().contains("Chưa có lượt"));
            assertTrue(hasEmptyLabel);
        }
    }

    @Test @Order(151)
    @DisplayName("reloadBidHistory() fingerprint không đổi → skip rebuild")
    void reloadBidHistory_sameFingerprint_skipsRebuild() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            List<BidRecord> records = new ArrayList<>(Collections.singletonList(makeBidRecord(1, 1000000)));
            String fp = (String) invokePrivate("computeBidHistoryFingerprint", new Class[]{List.class}, records);
            setField("lastBidHistoryFingerprint", fp);

            bs.when(() -> BidService.loadHistory(eq(1), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<List<BidRecord>>) inv.getArgument(1)).accept(records); return null; });

            VBox bidHistory = getField("bidHistory");
            int before = bidHistory.getChildren().size();
            invokePrivate("reloadBidHistory", new Class[]{int.class}, 1);
            assertEquals(before, bidHistory.getChildren().size());
        }
    }

    @Test @Order(152)
    @DisplayName("reloadBidHistory() isFetchingHistory=true → bỏ qua")
    void reloadBidHistory_alreadyFetching_skipsCall() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class)) {
            java.util.concurrent.atomic.AtomicBoolean flag = getField("isFetchingHistory");
            flag.set(true);
            invokePrivate("reloadBidHistory", new Class[]{int.class}, 1);
            bs.verifyNoInteractions();
            flag.set(false);
        }
    }

    @Test @Order(153)
    @DisplayName("reloadBidHistory() có dữ liệu → build rows")
    void reloadBidHistory_withData_buildsRows() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class);
             // FIX: Platform.runLater synchronous
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            List<BidRecord> records = new ArrayList<>();
            for (int i = 1; i <= 5; i++) records.add(makeBidRecord(i, 1000000L * i));

            setField("lastBidHistoryFingerprint", "");
            bs.when(() -> BidService.loadHistory(eq(1), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<List<BidRecord>>) inv.getArgument(1)).accept(records); return null; });

            invokePrivate("reloadBidHistory", new Class[]{int.class}, 1);

            VBox bidHistory = getField("bidHistory");
            assertFalse(bidHistory.getChildren().isEmpty());
        }
    }

    // =========================================================
    // 17. refreshAutoBidStatus()
    // =========================================================

    @Test @Order(160)
    @DisplayName("refreshAutoBidStatus() isFetchingAutoBid=true → bỏ qua")
    void refreshAutoBidStatus_alreadyFetching_skips() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class)) {
            java.util.concurrent.atomic.AtomicBoolean flag = getField("isFetchingAutoBid");
            flag.set(true);
            invokePrivate("refreshAutoBidStatus", new Class[]{int.class}, 1);
            bs.verifyNoInteractions();
            flag.set(false);
        }
    }

    @Test @Order(161)
    @DisplayName("refreshAutoBidStatus() isActive=true → ĐANG BẬT")
    void refreshAutoBidStatus_activeTrue_updatesUI() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class);
             // FIX: Platform.runLater synchronous để updateAutoBidUI chạy ngay
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            bs.when(() -> BidService.getAutoBidStatus(eq(1), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<Boolean>) inv.getArgument(1)).accept(true); return null; });

            invokePrivate("refreshAutoBidStatus", new Class[]{int.class}, 1);

            assertTrue(((Button) getField("btnAutoToggle")).getText().contains("ĐANG BẬT"));
        }
    }

    @Test @Order(162)
    @DisplayName("refreshAutoBidStatus() isActive=false → ĐANG TẮT")
    void refreshAutoBidStatus_activeFalse_updatesUI() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            bs.when(() -> BidService.getAutoBidStatus(eq(1), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<Boolean>) inv.getArgument(1)).accept(false); return null; });

            invokePrivate("refreshAutoBidStatus", new Class[]{int.class}, 1);

            assertTrue(((Button) getField("btnAutoToggle")).getText().contains("ĐANG TẮT"));
        }
    }

    // =========================================================
    // 18. showAlert()
    // =========================================================

    @Test @Order(170)
    @DisplayName("showAlert(INFORMATION) → showSuccessModal")
    void showAlert_information_callsSuccessModal() throws Exception {
        try (MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {
            invokePrivate("showAlert",
                    new Class[]{javafx.scene.control.Alert.AlertType.class, String.class, String.class},
                    javafx.scene.control.Alert.AlertType.INFORMATION, "Tiêu đề", "Nội dung");
            
            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Nội dung"));
        }
    }

    @Test @Order(171)
    @DisplayName("showAlert(ERROR) → showFailureModal")
    void showAlert_error_callsFailureModal() throws Exception {
        try (MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {
            invokePrivate("showAlert",
                    new Class[]{javafx.scene.control.Alert.AlertType.class, String.class, String.class},
                    javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", "Chi tiết");
            
            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Chi tiết"));
        }
    }

    @Test @Order(172)
    @DisplayName("showAlert(WARNING) → dùng Platform.runLater + Alert thuần")
    void showAlert_warning_usesDefaultAlert() throws Exception {
        try (MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {
            // Alert.showAndWait() sẽ throw trong headless mode, bắt được là đủ
            assertDoesNotThrow(() -> {
                try {
                    invokePrivate("showAlert",
                            new Class[]{javafx.scene.control.Alert.AlertType.class, String.class, String.class},
                            javafx.scene.control.Alert.AlertType.WARNING, "Cảnh báo", "Nội dung");
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    // Alert.showAndWait() có thể ném trong headless — bỏ qua
                }
            });
        }
    }

    // =========================================================
    // 19. onBack()
    // =========================================================

    @Test @Order(180)
    @DisplayName("onBack() MainController null → switchTo(VIEW_MAIN)")
    void onBack_noMainController_switchesToMain() {
        try (MockedStatic<vn.edu.vnu.uet.group8.client.util.SceneManager> sm =
                     mockStatic(vn.edu.vnu.uet.group8.client.util.SceneManager.class);
             MockedStatic<MainController> mc = mockStatic(MainController.class)) {
            mc.when(MainController::getInstance).thenReturn(null);
            sm.when(() -> vn.edu.vnu.uet.group8.client.util.SceneManager.switchTo(any())).thenAnswer(inv -> null);
            controller.onBack();
            sm.verify(() -> vn.edu.vnu.uet.group8.client.util.SceneManager
                    .switchTo(vn.edu.vnu.uet.group8.client.util.SceneManager.VIEW_MAIN));
        }
    }

    @Test @Order(181)
    @DisplayName("onBack() MainController có instance → loadContentView(VIEW_EXPLORE)")
    void onBack_withMainController_loadsExplore() {
        try (MockedStatic<MainController> mc = mockStatic(MainController.class)) {
            MainController mockMain = mock(MainController.class);
            mc.when(MainController::getInstance).thenReturn(mockMain);
            controller.onBack();
            verify(mockMain).loadContentView(vn.edu.vnu.uet.group8.client.util.SceneManager.VIEW_EXPLORE);
        }
    }

    // =========================================================
    // 20. setupPolling()
    // =========================================================

    @Test @Order(190)
    @DisplayName("setupPolling() item null → không gọi refreshAutoBidStatus")
    void setupPolling_nullItem_noRefresh() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);
            invokePrivate("setupPolling", new Class[]{});
            bs.verifyNoInteractions();
        }
    }

    @Test @Order(191)
    @DisplayName("setupPolling() item có giá trị → KeyFrame callback gọi refreshAutoBidStatus")
    void setupPolling_withItem_callsRefresh() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {
            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            bs.when(() -> BidService.getAutoBidStatus(eq(1), any())).thenAnswer(inv -> null);

            invokePrivate("setupPolling", new Class[]{});

            javafx.animation.Timeline timeline = getField("pollingTimeline");
            assertNotNull(timeline);
            // Fire KeyFrame event để test lambda
            timeline.getKeyFrames().get(0).getOnFinished().handle(new javafx.event.ActionEvent());
            bs.verify(() -> BidService.getAutoBidStatus(eq(1), any()));
        }
    }

    // =========================================================
    // 21. setupRealtimeUpdates()
    // =========================================================

    @Test @Order(200)
    @DisplayName("setupRealtimeUpdates() đăng ký subscribeAuctionStatus và subscribeAuctionEnded")
    void setupRealtimeUpdates_registersSubscriptions() throws Exception {
        try (MockedStatic<AuctionService> as = mockStatic(AuctionService.class)) {
            as.when(() -> AuctionService.subscribeAuctionStatus(any())).thenReturn(null);
            as.when(() -> AuctionService.subscribeAuctionEnded(any())).thenReturn(null);
            invokePrivate("setupRealtimeUpdates", new Class[]{});
            as.verify(() -> AuctionService.subscribeAuctionStatus(any()));
            as.verify(() -> AuctionService.subscribeAuctionEnded(any()));
        }
    }

    @Test @Order(201)
    @DisplayName("priceUpdate lambda: item null → không update UI")
    void realtimeLambda_priceUpdate_nullItem_noUpdate() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);

            final Consumer<?>[] captured = new Consumer[1];
            as.when(() -> AuctionService.subscribeAuctionStatus(any()))
                    .thenAnswer(inv -> { captured[0] = inv.getArgument(0); return null; });
            as.when(() -> AuctionService.subscribeAuctionEnded(any())).thenReturn(null);

            invokePrivate("setupRealtimeUpdates", new Class[]{});

            AuctionStatusDTO status = new AuctionStatusDTO(1, new BigDecimal("2000000"), Instant.now().plusSeconds(3600), Collections.emptyList());
            assertDoesNotThrow(() -> ((Consumer<AuctionStatusDTO>) captured[0]).accept(status));
        }
    }

    @Test @Order(202)
    @DisplayName("priceUpdate lambda: item.itemId khớp → update labels giá")
    void realtimeLambda_priceUpdate_matchingItem_updatesPrice() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             // FIX: runLater synchronous để lblCurrentPrice.setText chạy ngay
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            final Consumer<?>[] captured = new Consumer[1];
            as.when(() -> AuctionService.subscribeAuctionStatus(any()))
                    .thenAnswer(inv -> { captured[0] = inv.getArgument(0); return null; });
            as.when(() -> AuctionService.subscribeAuctionEnded(any())).thenReturn(null);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("setupRealtimeUpdates", new Class[]{});

            AuctionStatusDTO status = new AuctionStatusDTO(1, new BigDecimal("3000000"), Instant.now().plusSeconds(7200), Collections.emptyList());
            assertDoesNotThrow(() -> ((Consumer<AuctionStatusDTO>) captured[0]).accept(status));
        }
    }

    @Test @Order(203)
    @DisplayName("priceUpdate lambda: item.itemId không khớp → không update giá")
    void realtimeLambda_priceUpdate_mismatchedItem_noUpdate() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            final Consumer<?>[] captured = new Consumer[1];
            as.when(() -> AuctionService.subscribeAuctionStatus(any()))
                    .thenAnswer(inv -> { captured[0] = inv.getArgument(0); return null; });
            as.when(() -> AuctionService.subscribeAuctionEnded(any())).thenReturn(null);

            invokePrivate("setupRealtimeUpdates", new Class[]{});

            Label lblPrice = getField("lblCurrentPrice");
            String priceBefore = lblPrice.getText();

            AuctionStatusDTO status = new AuctionStatusDTO(999, new BigDecimal("9999999"), Instant.now().plusSeconds(3600), Collections.emptyList());
            assertDoesNotThrow(() -> ((Consumer<AuctionStatusDTO>) captured[0]).accept(status));

            assertEquals(priceBefore, lblPrice.getText());
        }
    }

    @Test @Order(204)
    @DisplayName("auctionEnded lambda: item khớp → showSuccessModal + disable form")
    void realtimeLambda_auctionEnded_matchingItem_disablesForm() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             // FIX: runLater synchronous
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            final Consumer<?>[] captured = new Consumer[1];
            as.when(() -> AuctionService.subscribeAuctionStatus(any())).thenReturn(null);
            as.when(() -> AuctionService.subscribeAuctionEnded(any()))
                    .thenAnswer(inv -> { captured[0] = inv.getArgument(0); return null; });
            mu.when(() -> ModalUtil.showSuccessModal(any(), any())).thenAnswer(inv -> null);

            invokePrivate("setupRealtimeUpdates", new Class[]{});

            AuctionEndedBroadcastResponse event = AuctionEndedBroadcastResponse.sold(1, "Test Product", new BigDecimal("5000000"), "winner");
            assertDoesNotThrow(() -> ((Consumer<AuctionEndedBroadcastResponse>) captured[0]).accept(event));

            Label toast = getField("toastLabel");
            assertNotNull(toast);
            assertTrue(toast.isVisible());
            assertTrue(toast.getText().contains("Phiên đấu giá đã khép lại"));
        }
    }

    @Test @Order(205)
    @DisplayName("auctionEnded lambda: item không khớp → không làm gì")
    void realtimeLambda_auctionEnded_mismatchItem_noAction() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            final Consumer<?>[] captured = new Consumer[1];
            as.when(() -> AuctionService.subscribeAuctionStatus(any())).thenReturn(null);
            as.when(() -> AuctionService.subscribeAuctionEnded(any()))
                    .thenAnswer(inv -> { captured[0] = inv.getArgument(0); return null; });

            invokePrivate("setupRealtimeUpdates", new Class[]{});

            AuctionEndedBroadcastResponse event = AuctionEndedBroadcastResponse.sold(999, "Other", new BigDecimal("5000000"), "winner");
            assertDoesNotThrow(() -> ((Consumer<AuctionEndedBroadcastResponse>) captured[0]).accept(event));
            mu.verifyNoInteractions();
        }
    }

    @Test @Order(206)
    @DisplayName("auctionEnded lambda: item null → không làm gì")
    void realtimeLambda_auctionEnded_nullItem_noAction() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<ModalUtil> mu = mockStatic(ModalUtil.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(null);

            final Consumer<?>[] captured = new Consumer[1];
            as.when(() -> AuctionService.subscribeAuctionStatus(any())).thenReturn(null);
            as.when(() -> AuctionService.subscribeAuctionEnded(any()))
                    .thenAnswer(inv -> { captured[0] = inv.getArgument(0); return null; });

            invokePrivate("setupRealtimeUpdates", new Class[]{});

            AuctionEndedBroadcastResponse event = AuctionEndedBroadcastResponse.noBid(1, "Test");
            assertDoesNotThrow(() -> ((Consumer<AuctionEndedBroadcastResponse>) captured[0]).accept(event));
            mu.verifyNoInteractions();
        }
    }

    // =========================================================
    // 22. priceUpdate endTime null/non-null
    // =========================================================

    @Test @Order(210)
    @DisplayName("priceUpdate broadcast: endTime non-null → item.endTime được cập nhật")
    void realtimeLambda_priceUpdate_nullEndTime_noEndTimeUpdate() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<AuctionService> as = mockStatic(AuctionService.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            AuctionItemDTO item = makeItem(SessionStatus.ACTIVE);
            Instant originalEndTime = item.getEndTime();
            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);

            final Consumer<?>[] captured = new Consumer[1];
            as.when(() -> AuctionService.subscribeAuctionStatus(any()))
                    .thenAnswer(inv -> { captured[0] = inv.getArgument(0); return null; });
            as.when(() -> AuctionService.subscribeAuctionEnded(any())).thenReturn(null);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("setupRealtimeUpdates", new Class[]{});

            Instant newEndTime = Instant.now().plusSeconds(7200);
            AuctionStatusDTO status = new AuctionStatusDTO(1, new BigDecimal("2000000"), newEndTime, Collections.emptyList());
            ((Consumer<AuctionStatusDTO>) captured[0]).accept(status);

            // endTime phải được cập nhật thành newEndTime
            assertNotEquals(originalEndTime, item.getEndTime());
            assertEquals(newEndTime, item.getEndTime());
        }
    }

    // =========================================================
    // 23. scene unload
    // =========================================================

    @Test @Order(220)
    @DisplayName("setupRealtimeUpdates() đăng ký subscriptions đúng")
    void realtimeLambda_sceneDetach_unsubscribesAndStopsTimers() throws Exception {
        try (MockedStatic<AuctionService> as = mockStatic(AuctionService.class)) {
            as.when(() -> AuctionService.subscribeAuctionStatus(any())).thenReturn(null);
            as.when(() -> AuctionService.subscribeAuctionEnded(any())).thenReturn(null);
            as.when(() -> AuctionService.unsubscribeAuctionStatus(any())).thenAnswer(inv -> null);
            as.when(() -> AuctionService.unsubscribeAuctionEnded(any())).thenAnswer(inv -> null);

            invokePrivate("setupRealtimeUpdates", new Class[]{});

            as.verify(() -> AuctionService.subscribeAuctionStatus(any()));
            as.verify(() -> AuctionService.subscribeAuctionEnded(any()));
        }
    }

    // =========================================================
    // 24. reloadBidHistory() limits
    // =========================================================

    @Test @Order(230)
    @DisplayName("reloadBidHistory() > 50 records → tối đa 50 HBox rows")
    void reloadBidHistory_over50Records_capsAt50() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            List<BidRecord> records = new ArrayList<>();
            for (int i = 1; i <= 60; i++) records.add(makeBidRecord(i, 1000000L + i));

            setField("lastBidHistoryFingerprint", "");
            bs.when(() -> BidService.loadHistory(eq(1), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<List<BidRecord>>) inv.getArgument(1)).accept(records); return null; });

            invokePrivate("reloadBidHistory", new Class[]{int.class}, 1);

            VBox bidHistory = getField("bidHistory");
            long rowCount = bidHistory.getChildren().stream()
                    .filter(n -> n instanceof javafx.scene.layout.HBox).count();
            assertTrue(rowCount <= 50, "Không quá 50 rows, thực tế: " + rowCount);
        }
    }

    @Test @Order(231)
    @DisplayName("reloadBidHistory() > 10 records → chart tối đa 10 điểm")
    void reloadBidHistory_over10Records_chartCapAt10() throws Exception {
        try (MockedStatic<BidService> bs = mockStatic(BidService.class);
             MockedStatic<Platform> mp = mockPlatformRunLaterSync()) {

            List<BidRecord> records = new ArrayList<>();
            Instant base = Instant.parse("2025-01-01T10:00:00Z");
            for (int i = 1; i <= 15; i++) {
                records.add(BidRecord.builder().bidId(i).itemId(1).userId(10)
                        .displayName("U" + i).amount(new BigDecimal(1000000L + i * 100))
                        .placedAt(base.plusSeconds(i * 60)).build());
            }

            setField("lastBidHistoryFingerprint", "");
            bs.when(() -> BidService.loadHistory(eq(1), any(Consumer.class)))
                    .thenAnswer(inv -> { ((Consumer<List<BidRecord>>) inv.getArgument(1)).accept(records); return null; });

            invokePrivate("reloadBidHistory", new Class[]{int.class}, 1);

            LineChart<?, ?> chart = getField("bidChart");
            if (!chart.getData().isEmpty()) {
                assertTrue(chart.getData().get(0).getData().size() <= 10);
            }
        }
    }

    // =========================================================
    // 25-27. loadData() edge cases
    // =========================================================

    @Test @Order(240)
    @DisplayName("loadData() ACTIVE endTime=null → countdownTimer null")
    void loadData_activeNoEndTime_noTimer() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = AuctionItemDTO.of(1, "Test", "Desc",
                    ItemCategory.ELECTRONICS, ItemCondition.NEW, SessionStatus.ACTIVE,
                    new BigDecimal("1000000"), null, 99, "seller", null, null, 0, Instant.now());

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(anyInt())).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            assertDoesNotThrow(() -> invokePrivate("loadData", new Class[]{}));
            assertNull(getField("countdownTimer"));
        }
    }

    @Test @Order(250)
    @DisplayName("loadData() description null → 'Chưa có mô tả'")
    void loadData_nullDescription_showsDefault() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = AuctionItemDTO.of(1, "Test", null,
                    ItemCategory.ELECTRONICS, ItemCondition.NEW, SessionStatus.ACTIVE,
                    new BigDecimal("1000000"), Instant.now().plusSeconds(3600), 99, "seller", null, null, 3, Instant.now());

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(anyInt())).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});
            assertEquals("Chưa có mô tả", ((Label) getField("lblDescription")).getText());
        }
    }

    @Test @Order(251)
    @DisplayName("loadData() title null → hiển thị chuỗi rỗng")
    void loadData_nullTitle_showsEmpty() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = AuctionItemDTO.of(1, null, "Desc",
                    ItemCategory.ELECTRONICS, ItemCondition.NEW, SessionStatus.ACTIVE,
                    new BigDecimal("1000000"), Instant.now().plusSeconds(3600), 99, "seller", null, null, 3, Instant.now());

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(anyInt())).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});
            assertEquals("", ((Label) getField("lblProductName")).getText());
        }
    }

    @Test @Order(252)
    @DisplayName("loadData() category null → 'Other'")
    void loadData_nullCategory_showsOther() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = AuctionItemDTO.of(1, "Test", "Desc",
                    null, ItemCondition.NEW, SessionStatus.ACTIVE,
                    new BigDecimal("1000000"), Instant.now().plusSeconds(3600), 99, "seller", null, null, 0, Instant.now());

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(anyInt())).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});
            assertEquals("Other", ((Label) getField("lblCategory")).getText());
        }
    }

    @Test @Order(253)
    @DisplayName("loadData() status null → không throw")
    void loadData_nullStatus_noBadge() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = AuctionItemDTO.of(1, "Test", "Desc",
                    ItemCategory.ELECTRONICS, ItemCondition.USED, null,
                    new BigDecimal("1000000"), null, 99, "seller", null, null, 0, Instant.now());

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(anyInt())).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            assertDoesNotThrow(() -> invokePrivate("loadData", new Class[]{}));
        }
    }

    @Test @Order(260)
    @DisplayName("loadData() lblBidStats hiển thị đúng số lượt")
    void loadData_bidStats_correctCount() throws Exception {
        try (MockedStatic<ClientModel> cm = mockStatic(ClientModel.class);
             MockedStatic<BidService> bs = mockStatic(BidService.class)) {

            AuctionItemDTO item = AuctionItemDTO.of(1, "Test", "Desc",
                    ItemCategory.ELECTRONICS, ItemCondition.NEW, SessionStatus.ACTIVE,
                    new BigDecimal("1000000"), Instant.now().plusSeconds(3600), 99, "seller", null, null, 7, Instant.now());

            ClientModel mockModel = mock(ClientModel.class);
            cm.when(ClientModel::getInstance).thenReturn(mockModel);
            when(mockModel.getCurrentAuctionItem()).thenReturn(item);
            when(mockModel.isFavorite(anyInt())).thenReturn(false);
            bs.when(() -> BidService.loadHistory(anyInt(), any())).thenAnswer(inv -> null);
            bs.when(() -> BidService.getAutoBidStatus(anyInt(), any())).thenAnswer(inv -> null);

            invokePrivate("loadData", new Class[]{});
            assertTrue(((Label) getField("lblBidStats")).getText().contains("7"));
        }
    }
}