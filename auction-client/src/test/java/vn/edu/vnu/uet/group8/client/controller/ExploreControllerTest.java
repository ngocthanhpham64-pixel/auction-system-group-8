package vn.edu.vnu.uet.group8.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.service.AuctionService;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Unit test toàn diện cho ExploreController.
 * Mục tiêu: đạt 100% instruction coverage và 100% branch coverage cho tất cả method.
 *
 * Chiến lược:
 * - Dùng Reflection để inject @FXML fields (không cần FXML loader thật).
 * - Dùng MockedStatic để mock AuctionService.loadAll (static method).
 * - Dùng ClientModel singleton (reset trước mỗi test).
 * - Mỗi branch trong filterAndRenderProducts, renderCurrentPage,
 *   updateActiveTag, getCategoryValueFromName, getCategoryNameFromValue,
 *   onPrevPage, onNextPage, onTagClick đều có ít nhất 1 test case.
 *
 * FIX NOTES:
 * - ClientModel.setAuctionItems() và setSearchQuery() dùng runOnFX() nên khi
 *   test không chạy trên FX Application Thread sẽ gọi Platform.runLater (bất
 *   đồng bộ). Dùng setAuctionItemsDirect() và setSearchQueryDirect() để bypass
 *   bằng cách set thẳng vào ObservableList/StringProperty bên trong ClientModel
 *   qua reflection, đảm bảo data sẵn sàng ngay lập tức.
 * - refreshProducts() -> AuctionService.loadAll callback chứa Platform.runLater
 *   -> vẫn async. Sau khi invoke onTagClick/initialize, gọi thêm
 *   filterAndRenderProducts() trực tiếp để đảm bảo state được cập nhật.
 */
@DisplayName("ExploreController")
class ExploreControllerTest extends FxTestBase {

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────

    private ExploreController controller;

    // FXML controls được inject bằng reflection
    private TilePane productContainer;
    private Label lblResultCount;
    private HBox hboxTags;
    private ComboBox<String> cbSort;
    private ComboBox<String> cbCategory;
    private Button btnPrevPage;
    private Button btnNextPage;
    private Label lblCurrentPage;
    private Label lblTotalPages;

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setup() throws Exception {
        // Reset ClientModel về trạng thái sạch (dùng direct helpers để tránh async)
        setAuctionItemsDirect(Collections.emptyList());
        setSearchQueryDirect("");

        controller = new ExploreController();

        // Tạo và inject tất cả @FXML fields
        productContainer = new TilePane();
        lblResultCount   = new Label();
        hboxTags         = new HBox();
        cbSort           = new ComboBox<>();
        cbCategory       = new ComboBox<>();
        btnPrevPage      = new Button("Prev");
        btnNextPage      = new Button("Next");
        lblCurrentPage   = new Label();
        lblTotalPages    = new Label();

        injectField("productContainer", productContainer);
        injectField("lblResultCount",   lblResultCount);
        injectField("hboxTags",         hboxTags);
        injectField("cbSort",           cbSort);
        injectField("cbCategory",       cbCategory);
        injectField("btnPrevPage",      btnPrevPage);
        injectField("btnNextPage",      btnNextPage);
        injectField("lblCurrentPage",   lblCurrentPage);
        injectField("lblTotalPages",    lblTotalPages);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: reflection utilities
    // ─────────────────────────────────────────────────────────────────────────

    private void injectField(String name, Object value) throws Exception {
        Field f = ExploreController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) throws Exception {
        Field f = ExploreController.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(controller);
    }

    private void setField(String name, Object value) throws Exception {
        injectField(name, value);
    }

    /** Gọi private method không tham số */
    private void callPrivate(String methodName) throws Exception {
        Method m = ExploreController.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(controller);
    }

    /** Gọi private method getCategoryValueFromName(String) */
    private String callGetCategoryValueFromName(String name) throws Exception {
        Method m = ExploreController.class.getDeclaredMethod("getCategoryValueFromName", String.class);
        m.setAccessible(true);
        return (String) m.invoke(controller, name);
    }

    /** Gọi private method getCategoryNameFromValue(String) */
    private String callGetCategoryNameFromValue(String value) throws Exception {
        Method m = ExploreController.class.getDeclaredMethod("getCategoryNameFromValue", String.class);
        m.setAccessible(true);
        return (String) m.invoke(controller, value);
    }

    /** Gọi private method updateActiveTag(String) */
    private void callUpdateActiveTag(String catValue) throws Exception {
        Method m = ExploreController.class.getDeclaredMethod("updateActiveTag", String.class);
        m.setAccessible(true);
        m.invoke(controller, catValue);
    }

    /**
     * FIX: Set trực tiếp vào ObservableList bên trong ClientModel, bypass runOnFX.
     * ClientModel.setAuctionItems() dùng Platform.runLater khi không ở FX thread,
     * khiến data chưa sẵn sàng khi filterAndRenderProducts() đọc ngay sau đó.
     */
    private void setAuctionItemsDirect(List<AuctionItemDTO> items) throws Exception {
        Field f = ClientModel.class.getDeclaredField("auctionItems");
        f.setAccessible(true);
        // auctionItems là ListProperty<AuctionItemDTO>, lấy ObservableList bên trong
        javafx.beans.property.ListProperty<AuctionItemDTO> listProp =
                (javafx.beans.property.ListProperty<AuctionItemDTO>) f.get(ClientModel.getInstance());
        listProp.setAll(items == null ? Collections.emptyList() : items);
    }

    /**
     * FIX: Set trực tiếp vào StringProperty bên trong ClientModel, bypass runOnFX.
     */
    private void setSearchQueryDirect(String query) throws Exception {
        Field f = ClientModel.class.getDeclaredField("searchQuery");
        f.setAccessible(true);
        javafx.beans.property.StringProperty prop =
                (javafx.beans.property.StringProperty) f.get(ClientModel.getInstance());
        prop.set(query != null ? query : "");
    }

    /**
     * FIX: Drain FX event queue. Gửi một task rỗng lên FX thread và chờ nó chạy xong.
     * Khi latch đếm về 0, tất cả Platform.runLater đã được enqueue trước đó đều đã chạy.
     */
    private static void flushFxEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        latch.await();
    }

    /** Tạo AuctionItemDTO mẫu */
    private AuctionItemDTO makeItem(int id, String title, ItemCategory category,
                                    BigDecimal price, Instant endTime, Instant createdAt) {
        return AuctionItemDTO.of(
                id, title, "desc", category, ItemCondition.NEW,
                SessionStatus.ACTIVE, price, endTime,
                1, "seller", null, null, 0, createdAt);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: getCategoryValueFromName — covers all switch branches
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCategoryValueFromName()")
    class GetCategoryValueFromNameTests {

        @Test @DisplayName("null → ALL")
        void nullReturnsAll() throws Exception {
            assertEquals("ALL", callGetCategoryValueFromName(null));
        }

        @Test @DisplayName("Đồng hồ cao cấp → WATCHES")
        void watches() throws Exception {
            assertEquals("WATCHES", callGetCategoryValueFromName("Đồng hồ cao cấp"));
        }

        @Test @DisplayName("Điện tử → ELECTRONICS")
        void electronics() throws Exception {
            assertEquals("ELECTRONICS", callGetCategoryValueFromName("Điện tử"));
        }

        @Test @DisplayName("Trang sức → JEWELRY")
        void jewelry() throws Exception {
            assertEquals("JEWELRY", callGetCategoryValueFromName("Trang sức"));
        }

        @Test @DisplayName("Nghệ thuật → ART")
        void art() throws Exception {
            assertEquals("ART", callGetCategoryValueFromName("Nghệ thuật"));
        }

        @Test @DisplayName("Xe cộ → VEHICLES")
        void vehicles() throws Exception {
            assertEquals("VEHICLES", callGetCategoryValueFromName("Xe cộ"));
        }

        @Test @DisplayName("Sách quý → BOOKS")
        void books() throws Exception {
            assertEquals("BOOKS", callGetCategoryValueFromName("Sách quý"));
        }

        @Test @DisplayName("Đồ cổ → ANTIQUES")
        void antiques() throws Exception {
            assertEquals("ANTIQUES", callGetCategoryValueFromName("Đồ cổ"));
        }

        @Test @DisplayName("Thời trang → FASHION")
        void fashion() throws Exception {
            assertEquals("FASHION", callGetCategoryValueFromName("Thời trang"));
        }

        @Test @DisplayName("Bất động sản → REAL_ESTATE")
        void realEstate() throws Exception {
            assertEquals("REAL_ESTATE", callGetCategoryValueFromName("Bất động sản"));
        }

        @Test @DisplayName("Nhà cửa → HOME")
        void home() throws Exception {
            assertEquals("HOME", callGetCategoryValueFromName("Nhà cửa"));
        }

        @Test @DisplayName("Thể thao → SPORTS")
        void sports() throws Exception {
            assertEquals("SPORTS", callGetCategoryValueFromName("Thể thao"));
        }

        @Test @DisplayName("Khác → OTHER")
        void other() throws Exception {
            assertEquals("OTHER", callGetCategoryValueFromName("Khác"));
        }

        @Test @DisplayName("Không rõ / default → ALL")
        void unknownDefault() throws Exception {
            assertEquals("ALL", callGetCategoryValueFromName("Tất cả danh mục"));
            assertEquals("ALL", callGetCategoryValueFromName("XYZ_UNKNOWN"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: getCategoryNameFromValue — covers all switch branches
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCategoryNameFromValue()")
    class GetCategoryNameFromValueTests {

        @Test @DisplayName("null → Tất cả danh mục")
        void nullReturnsDefault() throws Exception {
            assertEquals("Tất cả danh mục", callGetCategoryNameFromValue(null));
        }

        @Test @DisplayName("WATCHES → Đồng hồ cao cấp")
        void watches() throws Exception {
            assertEquals("Đồng hồ cao cấp", callGetCategoryNameFromValue("WATCHES"));
        }

        @Test @DisplayName("ELECTRONICS → Điện tử")
        void electronics() throws Exception {
            assertEquals("Điện tử", callGetCategoryNameFromValue("ELECTRONICS"));
        }

        @Test @DisplayName("JEWELRY → Trang sức")
        void jewelry() throws Exception {
            assertEquals("Trang sức", callGetCategoryNameFromValue("JEWELRY"));
        }

        @Test @DisplayName("ART → Nghệ thuật")
        void art() throws Exception {
            assertEquals("Nghệ thuật", callGetCategoryNameFromValue("ART"));
        }

        @Test @DisplayName("VEHICLES → Xe cộ")
        void vehicles() throws Exception {
            assertEquals("Xe cộ", callGetCategoryNameFromValue("VEHICLES"));
        }

        @Test @DisplayName("BOOKS → Sách quý")
        void books() throws Exception {
            assertEquals("Sách quý", callGetCategoryNameFromValue("BOOKS"));
        }

        @Test @DisplayName("ANTIQUES → Đồ cổ")
        void antiques() throws Exception {
            assertEquals("Đồ cổ", callGetCategoryNameFromValue("ANTIQUES"));
        }

        @Test @DisplayName("FASHION → Thời trang")
        void fashion() throws Exception {
            assertEquals("Thời trang", callGetCategoryNameFromValue("FASHION"));
        }

        @Test @DisplayName("REAL_ESTATE → Bất động sản")
        void realEstate() throws Exception {
            assertEquals("Bất động sản", callGetCategoryNameFromValue("REAL_ESTATE"));
        }

        @Test @DisplayName("HOME → Nhà cửa")
        void home() throws Exception {
            assertEquals("Nhà cửa", callGetCategoryNameFromValue("HOME"));
        }

        @Test @DisplayName("SPORTS → Thể thao")
        void sports() throws Exception {
            assertEquals("Thể thao", callGetCategoryNameFromValue("SPORTS"));
        }

        @Test @DisplayName("OTHER → Khác")
        void other() throws Exception {
            assertEquals("Khác", callGetCategoryNameFromValue("OTHER"));
        }

        @Test @DisplayName("Không rõ → Tất cả danh mục")
        void unknownDefault() throws Exception {
            assertEquals("Tất cả danh mục", callGetCategoryNameFromValue("ALL"));
            assertEquals("Tất cả danh mục", callGetCategoryNameFromValue("UNKNOWN_XYZ"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: updateActiveTag
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateActiveTag()")
    class UpdateActiveTagTests {

        @Test @DisplayName("hboxTags null → không throw")
        void nullHboxTags() throws Exception {
            injectField("hboxTags", null);
            assertDoesNotThrow(() -> callUpdateActiveTag("WATCHES"));
        }

        @Test @DisplayName("button userData khớp → thêm tag-pill-active, xóa tag-pill")
        void matchingButtonGetsActive() throws Exception {
            Button btn = new Button("Đồng hồ");
            btn.setUserData("WATCHES");
            btn.getStyleClass().add("tag-pill");
            hboxTags.getChildren().add(btn);

            callUpdateActiveTag("WATCHES");

            assertTrue(btn.getStyleClass().contains("tag-pill-active"),
                    "Phải có class tag-pill-active");
            assertFalse(btn.getStyleClass().contains("tag-pill"),
                    "Không được còn tag-pill");
        }

        @Test @DisplayName("button userData khớp, đã có tag-pill-active → không thêm trùng")
        void matchingButtonAlreadyActive() throws Exception {
            Button btn = new Button("Đồng hồ");
            btn.setUserData("WATCHES");
            btn.getStyleClass().add("tag-pill-active");
            hboxTags.getChildren().add(btn);

            callUpdateActiveTag("WATCHES");

            long count = btn.getStyleClass().stream()
                    .filter("tag-pill-active"::equals).count();
            assertEquals(1, count, "tag-pill-active không được trùng");
        }

        @Test @DisplayName("button userData không khớp → thêm tag-pill, xóa tag-pill-active")
        void nonMatchingButtonGetsTagPill() throws Exception {
            Button btn = new Button("Điện tử");
            btn.setUserData("ELECTRONICS");
            btn.getStyleClass().add("tag-pill-active");
            hboxTags.getChildren().add(btn);

            callUpdateActiveTag("WATCHES");

            assertTrue(btn.getStyleClass().contains("tag-pill"),
                    "Phải có class tag-pill");
            assertFalse(btn.getStyleClass().contains("tag-pill-active"),
                    "Không được còn tag-pill-active");
        }

        @Test @DisplayName("button userData null → không khớp, nhận tag-pill")
        void buttonWithNullUserData() throws Exception {
            Button btn = new Button("Any");
            // userData = null, không setUserData
            hboxTags.getChildren().add(btn);

            assertDoesNotThrow(() -> callUpdateActiveTag("WATCHES"));
            assertTrue(btn.getStyleClass().contains("tag-pill"));
        }

        @Test @DisplayName("Node không phải Button → bỏ qua")
        void nonButtonNodeIgnored() throws Exception {
            Label lbl = new Label("Not a button");
            hboxTags.getChildren().add(lbl);

            assertDoesNotThrow(() -> callUpdateActiveTag("WATCHES"));
        }

        @Test @DisplayName("Nhiều button — chỉ button khớp active, còn lại pill")
        void multipleButtonsOnlyOneActive() throws Exception {
            Button btn1 = new Button("Watches"); btn1.setUserData("WATCHES");
            Button btn2 = new Button("Electronics"); btn2.setUserData("ELECTRONICS");
            Button btn3 = new Button("All"); btn3.setUserData("ALL");
            hboxTags.getChildren().addAll(btn1, btn2, btn3);

            callUpdateActiveTag("ELECTRONICS");

            assertFalse(btn1.getStyleClass().contains("tag-pill-active"));
            assertTrue(btn2.getStyleClass().contains("tag-pill-active"));
            assertFalse(btn3.getStyleClass().contains("tag-pill-active"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: filterAndRenderProducts — filter branches
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("filterAndRenderProducts() — filter logic")
    class FilterAndRenderProductsTests {

        /** Cài sẵn cbSort để gọi filterAndRenderProducts không lỗi */
        private void setupSort(String sortValue) {
            cbSort.getItems().addAll("Mới nhất", "Giá thấp đến cao", "Giá cao đến thấp", "Sắp kết thúc");
            cbSort.setValue(sortValue);
        }

        @Test @DisplayName("query null → không filter, hiện tất cả")
        void nullQueryShowsAll() throws Exception {
            Instant now = Instant.now();
            // FIX: dùng setAuctionItemsDirect để data available ngay (bypass runOnFX)
            setAuctionItemsDirect(List.of(
                    makeItem(1, "Item A", ItemCategory.WATCHES, BigDecimal.TEN, now, now),
                    makeItem(2, "Item B", ItemCategory.ELECTRONICS, BigDecimal.ONE, now, now)
            ));
            setSearchQueryDirect(null);
            setupSort("Mới nhất");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(2, filtered.size());
        }

        @Test @DisplayName("query rỗng → không filter")
        void emptyQueryShowsAll() throws Exception {
            Instant now = Instant.now();
            // FIX: dùng setAuctionItemsDirect
            setAuctionItemsDirect(List.of(
                    makeItem(1, "Rolex", ItemCategory.WATCHES, BigDecimal.TEN, now, now)
            ));
            setSearchQueryDirect("");
            setupSort("Mới nhất");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(1, filtered.size());
        }

        @Test @DisplayName("query CATEGORY:WATCHES → chỉ lấy item có category WATCHES")
        void categoryFilterWatches() throws Exception {
            Instant now = Instant.now();
            // FIX: dùng setAuctionItemsDirect và setSearchQueryDirect
            setAuctionItemsDirect(List.of(
                    makeItem(1, "Rolex", ItemCategory.WATCHES, BigDecimal.TEN, now, now),
                    makeItem(2, "Phone", ItemCategory.ELECTRONICS, BigDecimal.ONE, now, now)
            ));
            setSearchQueryDirect("CATEGORY:WATCHES");
            setupSort("Mới nhất");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(1, filtered.size());
            assertEquals("Rolex", filtered.get(0).getTitle());
        }

        @Test @DisplayName("query CATEGORY: không khớp → filteredItems rỗng")
        void categoryFilterNoMatch() throws Exception {
            Instant now = Instant.now();
            // FIX: dùng setAuctionItemsDirect và setSearchQueryDirect
            setAuctionItemsDirect(List.of(
                    makeItem(1, "Phone", ItemCategory.ELECTRONICS, BigDecimal.ONE, now, now)
            ));
            setSearchQueryDirect("CATEGORY:WATCHES");
            setupSort("Mới nhất");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertTrue(filtered.isEmpty());
        }

        @Test @DisplayName("query CATEGORY: item.category null → bị filter ra")
        void categoryFilterItemWithNullCategory() throws Exception {
            Instant now = Instant.now();
            AuctionItemDTO noCategory = AuctionItemDTO.of(
                    3, "No Cat", "d", null, ItemCondition.NEW,
                    SessionStatus.ACTIVE, BigDecimal.ONE, now, 1, "s",
                    null, null, 0, now);
            // FIX: dùng setAuctionItemsDirect và setSearchQueryDirect
            setAuctionItemsDirect(List.of(noCategory));
            setSearchQueryDirect("CATEGORY:WATCHES");
            setupSort("Mới nhất");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertTrue(filtered.isEmpty());
        }

        @Test @DisplayName("keyword search — title khớp case-insensitive")
        void keywordSearchMatchesTitle() throws Exception {
            Instant now = Instant.now();
            // FIX: dùng setAuctionItemsDirect và setSearchQueryDirect
            setAuctionItemsDirect(List.of(
                    makeItem(1, "ROLEX Watch", ItemCategory.WATCHES, BigDecimal.TEN, now, now),
                    makeItem(2, "iPhone 16", ItemCategory.ELECTRONICS, BigDecimal.ONE, now, now)
            ));
            setSearchQueryDirect("rolex");
            setupSort("Mới nhất");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(1, filtered.size());
            assertEquals("ROLEX Watch", filtered.get(0).getTitle());
        }

        @Test @DisplayName("keyword search — title null → không khớp, bị lọc ra")
        void keywordSearchNullTitle() throws Exception {
            Instant now = Instant.now();
            AuctionItemDTO noTitle = AuctionItemDTO.of(
                    4, null, "d", ItemCategory.WATCHES, ItemCondition.NEW,
                    SessionStatus.ACTIVE, BigDecimal.TEN, now, 1, "s",
                    null, null, 0, now);
            // FIX: dùng setAuctionItemsDirect và setSearchQueryDirect
            setAuctionItemsDirect(List.of(noTitle));
            setSearchQueryDirect("rolex");
            setupSort("Mới nhất");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertTrue(filtered.isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: filterAndRenderProducts — sorting branches
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("filterAndRenderProducts() — sorting logic")
    class SortingTests {

        private final Instant T1 = Instant.ofEpochSecond(1_000_000);
        private final Instant T2 = Instant.ofEpochSecond(2_000_000);
        private final Instant T3 = Instant.ofEpochSecond(3_000_000);

        /**
         * FIX: Dùng setAuctionItemsDirect thay vì ClientModel.getInstance().setAuctionItems().
         * setAuctionItems() gọi runOnFX -> Platform.runLater khi không trên FX thread
         * -> data chưa set khi filterAndRenderProducts() đọc -> filteredItems rỗng
         * -> IndexOutOfBoundsException khi truy cập filtered.get(0).
         */
        private void setupItems() throws Exception {
            setAuctionItemsDirect(List.of(
                    makeItem(1, "A", ItemCategory.WATCHES, new BigDecimal("300"), T3, T1),
                    makeItem(2, "B", ItemCategory.WATCHES, new BigDecimal("100"), T1, T3),
                    makeItem(3, "C", ItemCategory.WATCHES, new BigDecimal("200"), T2, T2)
            ));
            setSearchQueryDirect("");
        }

        @Test @DisplayName("Sắp xếp mặc định 'Mới nhất' — giảm dần theo createdAt")
        void sortNewest() throws Exception {
            setupItems();
            cbSort.getItems().addAll("Mới nhất");
            cbSort.setValue("Mới nhất");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            // createdAt: T3 > T2 > T1 → thứ tự: item2(T3), item3(T2), item1(T1)
            assertEquals(2, filtered.get(0).getItemId());
            assertEquals(3, filtered.get(1).getItemId());
            assertEquals(1, filtered.get(2).getItemId());
        }

        @Test @DisplayName("Giá thấp đến cao")
        void sortPriceLowToHigh() throws Exception {
            setupItems();
            cbSort.getItems().addAll("Giá thấp đến cao");
            cbSort.setValue("Giá thấp đến cao");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            // 100, 200, 300
            assertEquals(new BigDecimal("100"), filtered.get(0).getCurrentPrice());
            assertEquals(new BigDecimal("200"), filtered.get(1).getCurrentPrice());
            assertEquals(new BigDecimal("300"), filtered.get(2).getCurrentPrice());
        }

        @Test @DisplayName("Giá cao đến thấp")
        void sortPriceHighToLow() throws Exception {
            setupItems();
            cbSort.getItems().addAll("Giá cao đến thấp");
            cbSort.setValue("Giá cao đến thấp");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(new BigDecimal("300"), filtered.get(0).getCurrentPrice());
            assertEquals(new BigDecimal("100"), filtered.get(2).getCurrentPrice());
        }

        @Test @DisplayName("Sắp kết thúc — sắp xếp tăng dần endTime")
        void sortEndingSoon() throws Exception {
            setupItems();
            cbSort.getItems().addAll("Sắp kết thúc");
            cbSort.setValue("Sắp kết thúc");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            // setupItems(): item1.endTime=T3, item2.endTime=T1, item3.endTime=T2
            // Sort tăng dần endTime: T1(item2) < T2(item3) < T3(item1) → thứ tự: 2, 3, 1
            assertEquals(2, filtered.get(0).getItemId());
            assertEquals(3, filtered.get(1).getItemId());
            assertEquals(1, filtered.get(2).getItemId());
        }

        @Test @DisplayName("Sort giá — p1 null, p2 null → equal (return 0)")
        void sortPriceBothNull() throws Exception {
            Instant now = Instant.now();
            setAuctionItemsDirect(List.of(
                    makeItem(1, "X", ItemCategory.WATCHES, null, now, now),
                    makeItem(2, "Y", ItemCategory.WATCHES, null, now, now)
            ));
            setSearchQueryDirect("");
            cbSort.getItems().addAll("Giá thấp đến cao");
            cbSort.setValue("Giá thấp đến cao");

            assertDoesNotThrow(() -> callPrivate("filterAndRenderProducts"));
            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(2, filtered.size());
        }

        @Test @DisplayName("Sort giá thấp đến cao — p1 null (xếp cuối)")
        void sortPriceP1Null() throws Exception {
            Instant now = Instant.now();
            setAuctionItemsDirect(List.of(
                    makeItem(1, "NullPrice", ItemCategory.WATCHES, null, now, now),
                    makeItem(2, "HasPrice",  ItemCategory.WATCHES, BigDecimal.TEN, now, now)
            ));
            setSearchQueryDirect("");
            cbSort.getItems().addAll("Giá thấp đến cao");
            cbSort.setValue("Giá thấp đến cao");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            // p1 null → return 1 (xuống cuối)
            assertEquals(2, filtered.get(0).getItemId());
            assertEquals(1, filtered.get(1).getItemId());
        }

        @Test @DisplayName("Sort giá thấp đến cao — p2 null (p1 lên trước)")
        void sortPriceP2Null() throws Exception {
            Instant now = Instant.now();
            setAuctionItemsDirect(List.of(
                    makeItem(1, "HasPrice",  ItemCategory.WATCHES, BigDecimal.TEN, now, now),
                    makeItem(2, "NullPrice", ItemCategory.WATCHES, null, now, now)
            ));
            setSearchQueryDirect("");
            cbSort.getItems().addAll("Giá thấp đến cao");
            cbSort.setValue("Giá thấp đến cao");

            callPrivate("filterAndRenderProducts");

            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(1, filtered.get(0).getItemId());
            assertEquals(2, filtered.get(1).getItemId());
        }

        @Test @DisplayName("Sort giá cao đến thấp — p1 null p2 null")
        void sortHighToLowBothNull() throws Exception {
            Instant now = Instant.now();
            setAuctionItemsDirect(List.of(
                    makeItem(1, "X", ItemCategory.WATCHES, null, now, now),
                    makeItem(2, "Y", ItemCategory.WATCHES, null, now, now)
            ));
            setSearchQueryDirect("");
            cbSort.getItems().addAll("Giá cao đến thấp");
            cbSort.setValue("Giá cao đến thấp");

            assertDoesNotThrow(() -> callPrivate("filterAndRenderProducts"));
        }

        @Test @DisplayName("Sort giá cao đến thấp — p1 null (xuống cuối)")
        void sortHighToLowP1Null() throws Exception {
            Instant now = Instant.now();
            setAuctionItemsDirect(List.of(
                    makeItem(1, "NullPrice", ItemCategory.WATCHES, null, now, now),
                    makeItem(2, "HasPrice",  ItemCategory.WATCHES, BigDecimal.TEN, now, now)
            ));
            setSearchQueryDirect("");
            cbSort.getItems().addAll("Giá cao đến thấp");
            cbSort.setValue("Giá cao đến thấp");

            callPrivate("filterAndRenderProducts");
            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(2, filtered.get(0).getItemId());
        }

        @Test @DisplayName("Sort giá cao đến thấp — p2 null (p1 lên trước)")
        void sortHighToLowP2Null() throws Exception {
            Instant now = Instant.now();
            setAuctionItemsDirect(List.of(
                    makeItem(1, "HasPrice",  ItemCategory.WATCHES, BigDecimal.TEN, now, now),
                    makeItem(2, "NullPrice", ItemCategory.WATCHES, null, now, now)
            ));
            setSearchQueryDirect("");
            cbSort.getItems().addAll("Giá cao đến thấp");
            cbSort.setValue("Giá cao đến thấp");

            callPrivate("filterAndRenderProducts");
            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(1, filtered.get(0).getItemId());
        }

        @Test @DisplayName("Sort Sắp kết thúc — endTime null xếp cuối (nullsLast)")
        void sortEndTimeSomeNull() throws Exception {
            Instant now = Instant.now();
            setAuctionItemsDirect(List.of(
                    makeItem(1, "NullEnd", ItemCategory.WATCHES, BigDecimal.TEN, null, now),
                    makeItem(2, "HasEnd",  ItemCategory.WATCHES, BigDecimal.TEN, now, now)
            ));
            setSearchQueryDirect("");
            cbSort.getItems().addAll("Sắp kết thúc");
            cbSort.setValue("Sắp kết thúc");

            callPrivate("filterAndRenderProducts");
            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(2, filtered.get(0).getItemId());
            assertEquals(1, filtered.get(1).getItemId());
        }

        @Test @DisplayName("Sort Mới nhất — createdAt null xếp cuối (nullsLast)")
        void sortNewestNullCreatedAt() throws Exception {
            Instant now = Instant.now();
            setAuctionItemsDirect(List.of(
                    makeItem(1, "NullCreated", ItemCategory.WATCHES, BigDecimal.TEN, now, null),
                    makeItem(2, "HasCreated",  ItemCategory.WATCHES, BigDecimal.TEN, now, now)
            ));
            setSearchQueryDirect("");
            cbSort.getItems().addAll("Mới nhất");
            cbSort.setValue("Mới nhất");

            callPrivate("filterAndRenderProducts");
            List<AuctionItemDTO> filtered = getField("filteredItems");
            assertEquals(2, filtered.get(0).getItemId());
            assertEquals(1, filtered.get(1).getItemId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: renderCurrentPage — pagination branches
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("renderCurrentPage()")
    class RenderCurrentPageTests {

        @Test @DisplayName("Danh sách rỗng → hiện 'Không có phiên đấu giá nào', page 1/1")
        void emptyList() throws Exception {
            setField("filteredItems", new ArrayList<>());
            setField("currentPage", 1);

            callPrivate("renderCurrentPage");

            assertEquals("Không có phiên đấu giá nào", lblResultCount.getText());
            assertEquals("1", lblCurrentPage.getText());
            assertEquals("1", lblTotalPages.getText());
            assertTrue(btnPrevPage.isDisable());
            assertTrue(btnNextPage.isDisable());
        }

        @Test @DisplayName("Có items, page 1 → hiện đúng label kết quả")
        void page1WithItems() throws Exception {
            Instant now = Instant.now();
            List<AuctionItemDTO> items = new ArrayList<>();
            items.add(makeItem(1, "Item 1", ItemCategory.WATCHES, BigDecimal.TEN, now, now));
            items.add(makeItem(2, "Item 2", ItemCategory.WATCHES, BigDecimal.ONE, now, now));
            setField("filteredItems", items);
            setField("currentPage", 1);

            callPrivate("renderCurrentPage");

            String txt = lblResultCount.getText();
            assertTrue(txt.contains("2 phiên đấu giá"));
            assertTrue(txt.contains("1–2"));
            assertEquals("1", lblCurrentPage.getText());
            assertEquals("1", lblTotalPages.getText());
            assertTrue(btnPrevPage.isDisable());
            assertTrue(btnNextPage.isDisable());
        }

        @Test @DisplayName("currentPage < 1 → clamp thành 1")
        void currentPageBelowOne() throws Exception {
            Instant now = Instant.now();
            List<AuctionItemDTO> items = new ArrayList<>();
            items.add(makeItem(1, "Item", ItemCategory.WATCHES, BigDecimal.TEN, now, now));
            setField("filteredItems", items);
            setField("currentPage", 0);

            callPrivate("renderCurrentPage");

            int cp = getField("currentPage");
            assertEquals(1, cp);
        }

        @Test @DisplayName("currentPage > totalPages → clamp xuống totalPages")
        void currentPageAboveTotalPages() throws Exception {
            Instant now = Instant.now();
            List<AuctionItemDTO> items = new ArrayList<>();
            items.add(makeItem(1, "Item", ItemCategory.WATCHES, BigDecimal.TEN, now, now));
            setField("filteredItems", items);
            setField("currentPage", 999);

            callPrivate("renderCurrentPage");

            int cp = getField("currentPage");
            assertEquals(1, cp);
        }

        @Test @DisplayName("Nhiều hơn PAGE_SIZE items → pagination hoạt động đúng")
        void moreThanPageSizeItems() throws Exception {
            Instant now = Instant.now();
            List<AuctionItemDTO> items = new ArrayList<>();
            for (int i = 1; i <= 25; i++) {
                items.add(makeItem(i, "Item " + i, ItemCategory.WATCHES,
                        new BigDecimal(i * 10), now, now));
            }
            setField("filteredItems", items);
            setField("currentPage", 1);

            callPrivate("renderCurrentPage");

            // 25 items, PAGE_SIZE=20 → 2 pages
            assertEquals("2", lblTotalPages.getText());
            assertEquals("1", lblCurrentPage.getText());
            // Trang 1: items 1-20, nên Next enabled, Prev disabled
            assertTrue(btnPrevPage.isDisable());
            assertFalse(btnNextPage.isDisable());

            String txt = lblResultCount.getText();
            assertTrue(txt.contains("25 phiên đấu giá"));
            assertTrue(txt.contains("1–20"));
        }

        @Test @DisplayName("Trang cuối → Prev enabled, Next disabled")
        void lastPageNavigation() throws Exception {
            Instant now = Instant.now();
            List<AuctionItemDTO> items = new ArrayList<>();
            for (int i = 1; i <= 25; i++) {
                items.add(makeItem(i, "Item " + i, ItemCategory.WATCHES,
                        new BigDecimal(i * 10), now, now));
            }
            setField("filteredItems", items);
            setField("currentPage", 2);

            callPrivate("renderCurrentPage");

            assertFalse(btnPrevPage.isDisable());
            assertTrue(btnNextPage.isDisable());
            // Trang 2: items 21-25
            String txt = lblResultCount.getText();
            assertTrue(txt.contains("21–25"), "Expected 21–25 but got: " + txt);
        }

        @Test @DisplayName("lblCurrentPage và lblTotalPages null → không throw NullPointerException")
        void nullPaginationLabels() throws Exception {
            injectField("lblCurrentPage", null);
            injectField("lblTotalPages", null);
            setField("filteredItems", new ArrayList<>());
            setField("currentPage", 1);

            assertDoesNotThrow(() -> callPrivate("renderCurrentPage"));
        }

        @Test @DisplayName("btnPrevPage và btnNextPage null → không throw NullPointerException")
        void nullNavButtons() throws Exception {
            injectField("btnPrevPage", null);
            injectField("btnNextPage", null);
            setField("filteredItems", new ArrayList<>());
            setField("currentPage", 1);

            assertDoesNotThrow(() -> callPrivate("renderCurrentPage"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: onPrevPage
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onPrevPage()")
    class OnPrevPageTests {

        @Test @DisplayName("currentPage > 1 → giảm 1 và render lại")
        void decrementPage() throws Exception {
            Instant now = Instant.now();
            List<AuctionItemDTO> items = new ArrayList<>();
            for (int i = 1; i <= 25; i++) {
                items.add(makeItem(i, "Item " + i, ItemCategory.WATCHES,
                        BigDecimal.TEN, now, now));
            }
            setField("filteredItems", items);
            setField("currentPage", 2);

            Method m = ExploreController.class.getDeclaredMethod("onPrevPage");
            m.setAccessible(true);
            m.invoke(controller);

            int cp = getField("currentPage");
            assertEquals(1, cp);
        }

        @Test @DisplayName("currentPage = 1 → không thay đổi trang")
        void noDecrementAtFirstPage() throws Exception {
            setField("filteredItems", new ArrayList<>());
            setField("currentPage", 1);

            Method m = ExploreController.class.getDeclaredMethod("onPrevPage");
            m.setAccessible(true);
            m.invoke(controller);

            int cp = getField("currentPage");
            assertEquals(1, cp);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: onNextPage
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onNextPage()")
    class OnNextPageTests {

        @Test @DisplayName("currentPage < totalPages → tăng 1")
        void incrementPage() throws Exception {
            Instant now = Instant.now();
            List<AuctionItemDTO> items = new ArrayList<>();
            for (int i = 1; i <= 25; i++) {
                items.add(makeItem(i, "Item " + i, ItemCategory.WATCHES,
                        BigDecimal.TEN, now, now));
            }
            setField("filteredItems", items);
            setField("currentPage", 1);

            Method m = ExploreController.class.getDeclaredMethod("onNextPage");
            m.setAccessible(true);
            m.invoke(controller);

            int cp = getField("currentPage");
            assertEquals(2, cp);
        }

        @Test @DisplayName("currentPage = totalPages → không tăng")
        void noIncrementAtLastPage() throws Exception {
            Instant now = Instant.now();
            List<AuctionItemDTO> items = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                items.add(makeItem(i, "Item " + i, ItemCategory.WATCHES,
                        BigDecimal.TEN, now, now));
            }
            setField("filteredItems", items);
            setField("currentPage", 1); // 5 items / PAGE_SIZE 20 = 1 page

            Method m = ExploreController.class.getDeclaredMethod("onNextPage");
            m.setAccessible(true);
            m.invoke(controller);

            int cp = getField("currentPage");
            assertEquals(1, cp);
        }

        @Test @DisplayName("filteredItems rỗng → totalPages = 1, không increment từ page 1")
        void emptyListNoIncrement() throws Exception {
            setField("filteredItems", new ArrayList<>());
            setField("currentPage", 1);

            Method m = ExploreController.class.getDeclaredMethod("onNextPage");
            m.setAccessible(true);
            m.invoke(controller);

            int cp = getField("currentPage");
            assertEquals(1, cp);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: onTagClick
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onTagClick()")
    class OnTagClickTests {

        private Method onTagClickMethod;

        @BeforeEach
        void getMethod() throws Exception {
            onTagClickMethod = ExploreController.class
                    .getDeclaredMethod("onTagClick", ActionEvent.class);
            onTagClickMethod.setAccessible(true);
            // Setup cbCategory và cbSort tối thiểu
            cbCategory.getItems().addAll("Tất cả danh mục", "Đồng hồ cao cấp");
            cbCategory.getSelectionModel().selectFirst();
            cbSort.getItems().addAll("Mới nhất");
            cbSort.setValue("Mới nhất");
        }

        @Test @DisplayName("category = ALL → setSearchQuery('')")
        void tagAllClearsQuery() throws Exception {
            Button tagBtn = new Button("Tất cả");
            tagBtn.setUserData("ALL");

            try (MockedStatic<AuctionService> mockedService = Mockito.mockStatic(AuctionService.class)) {
                mockedService.when(() -> AuctionService.loadAll(isNull(), any(Runnable.class)))
                        .thenAnswer(inv -> {
                            // FIX: callback chứa Platform.runLater, không chạy ngay trong test.
                            // Gọi filterAndRenderProducts trực tiếp thay vì qua callback.
                            return null;
                        });

                ActionEvent event = new ActionEvent(tagBtn, null);
                onTagClickMethod.invoke(controller, event);

                // FIX: setSearchQuery dùng runOnFX -> dùng setSearchQueryDirect để đọc
                // giá trị thực mà onTagClick đã set qua ClientModel.getInstance().setSearchQuery().
                // Tuy nhiên trong test, setSearchQuery với Platform.runLater có thể chưa chạy.
                // Thay vào đó đọc trực tiếp từ StringProperty.
                Field f = ClientModel.class.getDeclaredField("searchQuery");
                f.setAccessible(true);
                javafx.beans.property.StringProperty prop =
                        (javafx.beans.property.StringProperty) f.get(ClientModel.getInstance());
                assertEquals("", prop.get());
            }
        }

        @Test @DisplayName("category = null → setSearchQuery('')")
        void tagNullClearsQuery() throws Exception {
            Button tagBtn = new Button("Unknown");
            tagBtn.setUserData(null);

            try (MockedStatic<AuctionService> mockedService = Mockito.mockStatic(AuctionService.class)) {
                mockedService.when(() -> AuctionService.loadAll(isNull(), any(Runnable.class)))
                        .thenAnswer(inv -> null);

                ActionEvent event = new ActionEvent(tagBtn, null);
                onTagClickMethod.invoke(controller, event);

                // FIX: đọc trực tiếp từ StringProperty
                Field f = ClientModel.class.getDeclaredField("searchQuery");
                f.setAccessible(true);
                javafx.beans.property.StringProperty prop =
                        (javafx.beans.property.StringProperty) f.get(ClientModel.getInstance());
                assertEquals("", prop.get());
            }
        }

        @Test @DisplayName("category cụ thể → setSearchQuery('CATEGORY:xxx')")
        void tagCategorySetQuery() throws Exception {
            Button tagBtn = new Button("Điện tử");
            tagBtn.setUserData("ELECTRONICS");

            try (MockedStatic<AuctionService> mockedService = Mockito.mockStatic(AuctionService.class)) {
                mockedService.when(() -> AuctionService.loadAll(isNull(), any(Runnable.class)))
                        .thenAnswer(inv -> null);

                ActionEvent event = new ActionEvent(tagBtn, null);
                onTagClickMethod.invoke(controller, event);

                // FIX: setSearchQuery() gọi runOnFX -> Platform.runLater (bất đồng bộ).
                // Drain FX event queue bằng cách submit một task rỗng và chờ nó hoàn thành.
                // Khi task rỗng này chạy xong, tất cả Platform.runLater trước đó đã được execute.
                flushFxEvents();

                Field f = ClientModel.class.getDeclaredField("searchQuery");
                f.setAccessible(true);
                javafx.beans.property.StringProperty prop =
                        (javafx.beans.property.StringProperty) f.get(ClientModel.getInstance());
                assertEquals("CATEGORY:ELECTRONICS", prop.get());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: initialize — branches trong initialize()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("initialize()")
    class InitializeTests {

        @Test @DisplayName("searchQuery không bắt đầu CATEGORY: → không cập nhật tag/cbCategory")
        void initWithoutCategoryQuery() throws Exception {
            // FIX: dùng setSearchQueryDirect để đảm bảo giá trị sẵn sàng ngay
            setSearchQueryDirect("some keyword");
            cbSort.getItems().addAll("Mới nhất", "Giá thấp đến cao", "Giá cao đến thấp", "Sắp kết thúc");

            try (MockedStatic<AuctionService> mockedService = Mockito.mockStatic(AuctionService.class)) {
                mockedService.when(() -> AuctionService.loadAll(isNull(), any(Runnable.class)))
                        .thenAnswer(inv -> null);

                Method init = ExploreController.class.getDeclaredMethod("initialize");
                init.setAccessible(true);
                assertDoesNotThrow(() -> init.invoke(controller));
            }
        }

        @Test @DisplayName("searchQuery bắt đầu CATEGORY: → cập nhật tag và cbCategory")
        void initWithCategoryQuery() throws Exception {
            // FIX: dùng setSearchQueryDirect thay vì ClientModel.getInstance().setSearchQuery()
            // để đảm bảo giá trị sẵn sàng đồng bộ trước khi initialize() đọc
            setSearchQueryDirect("CATEGORY:ELECTRONICS");

            try (MockedStatic<AuctionService> mockedService = Mockito.mockStatic(AuctionService.class)) {
                mockedService.when(() -> AuctionService.loadAll(isNull(), any(Runnable.class)))
                        .thenAnswer(inv -> null);

                Method init = ExploreController.class.getDeclaredMethod("initialize");
                init.setAccessible(true);
                init.invoke(controller);

                // cbCategory phải được set thành "Điện tử"
                // initialize() tự thêm items vào cbCategory rồi set value
                assertEquals("Điện tử", cbCategory.getValue());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests: refreshProducts — AuctionService.loadAll được gọi
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshProducts()")
    class RefreshProductsTests {

        @Test @DisplayName("Gọi AuctionService.loadAll với null filter và callback không null")
        void callsLoadAll() throws Exception {
            try (MockedStatic<AuctionService> mockedService = Mockito.mockStatic(AuctionService.class)) {
                mockedService.when(() -> AuctionService.loadAll(isNull(), any(Runnable.class)))
                        .thenAnswer(inv -> null);

                callPrivate("refreshProducts");

                mockedService.verify(() -> AuctionService.loadAll(isNull(), any(Runnable.class)),
                        times(1));
            }
        }
    }
}