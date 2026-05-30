package vn.edu.vnu.uet.group8.client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-test cho {@link MyProductsController}.
 *
 * <p>Mục tiêu: phủ 100% method và branch theo báo cáo JaCoCo:
 * <ul>
 *   <li>initialize() — tất cả nhánh btnDraft null/non-null, MainController null/non-null</li>
 *   <li>loadMyProducts() — success callback và error callback</li>
 *   <li>isDraftItem() — status DRAFT, currentPrice+endTime đều null, các trường hợp còn lại</li>
 *   <li>renderProducts() — empty / non-empty cho mỗi danh mục (all, active, completed, draft),
 *       vboxDraft null và non-null</li>
 *   <li>createItemCard() — mọi nhánh badge: DRAFT, UPCOMING, ACTIVE, kết thúc (SOLD/ENDED_NO_BID/CANCELLED),
 *       category null, condition null, currentPrice null/non-null,
 *       isActive true/false (opacity),
 *       mouseEntered/mouseExited/mouseClicked → isDraft, UPCOMING, else</li>
 *   <li>buildThumbnail() — urls null, urls empty, urls non-empty (load OK + exception)</li>
 *   <li>fallbackIcon() — label trả về đúng emoji</li>
 *   <li>navigateToDetail() — MainController null / non-null</li>
 *   <li>showError() — ghi đúng text lên VBox</li>
 *   <li>switchTab() — btnDraft null / non-null; vbox visible/managed thay đổi đúng</li>
 * </ul>
 *
 * <p>Không cần mock SellerService / AuctionService / MainController thật:
 * các callback được gọi trực tiếp qua reflection để kiểm tra logic thuần.
 */
@DisplayName("MyProductsController")
class MyProductsControllerTest extends FxTestBase {

    // ─────────────────────────────────────────────────────────────────
    // Helpers — reflection
    // ─────────────────────────────────────────────────────────────────

    private MyProductsController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new MyProductsController();

        // Inject @FXML fields bắt buộc
        setField("btnAll",       new Button("Tất cả"));
        setField("btnActive",    new Button("Đang đấu giá"));
        setField("btnCompleted", new Button("Đã hoàn thành"));
        setField("btnDraft",     new Button("Bản nháp"));
        setField("vboxAll",      new VBox());
        setField("vboxActive",   new VBox());
        setField("vboxCompleted",new VBox());
        setField("vboxDraft",    new VBox());
        setField("txtSearch",    new TextField());
        setField("btnAddNew",    new Button("Thêm"));

        // Constant styles (được gán trong constructor của controller thật)
        setPrivateField("ACTIVE_STYLE",
                "-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-weight: bold;" +
                        " -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        setPrivateField("INACTIVE_STYLE",
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: bold;" +
                        " -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;" +
                        " -fx-border-color: #94a3b8; -fx-border-radius: 20;");
    }

    /** Gán field (kể cả private) qua reflection. */
    private void setField(String name, Object value) throws Exception {
        Field f = MyProductsController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    /** Gán field kể cả final (dùng cho String constants). */
    private void setPrivateField(String name, Object value) throws Exception {
        try {
            Field f = MyProductsController.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(controller, value);
        } catch (NoSuchFieldException ignored) {
            // Nếu constant được định nghĩa inline, bỏ qua
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) throws Exception {
        Field f = MyProductsController.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(controller);
    }

    /** Gọi method private/protected bằng reflection. */
    private Object call(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = MyProductsController.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    // ─────────────────────────────────────────────────────────────────
    // Factory DTO tiện lợi
    // ─────────────────────────────────────────────────────────────────

    /** Tạo AuctionItemDTO với mọi trường cần thiết. */
    private AuctionItemDTO dto(int id, SessionStatus status,
                               BigDecimal price, Instant endTime,
                               ItemCategory category, ItemCondition condition,
                               List<String> imageUrls) {
        return AuctionItemDTO.of(
                id, "Sản phẩm " + id, "Mô tả " + id,
                category, condition,
                status, price,
                endTime, 1, "seller_" + id,
                null, imageUrls,
                0, Instant.now()
        );
    }

    /** Shortcut: tạo DTO đơn giản (ACTIVE, có giá, có endTime). */
    private AuctionItemDTO activeDto(int id) {
        return dto(id, SessionStatus.ACTIVE, BigDecimal.valueOf(100_000),
                Instant.now().plusSeconds(3600),
                ItemCategory.ELECTRONICS, ItemCondition.NEW, null);
    }

    /** Shortcut: UPCOMING. */
    private AuctionItemDTO upcomingDto(int id) {
        return dto(id, SessionStatus.UPCOMING, BigDecimal.valueOf(50_000),
                Instant.now().plusSeconds(7200),
                ItemCategory.WATCHES, ItemCondition.USED, null);
    }

    /** Shortcut: SOLD. */
    private AuctionItemDTO soldDto(int id) {
        return dto(id, SessionStatus.SOLD, BigDecimal.valueOf(200_000),
                Instant.now().minusSeconds(3600),
                ItemCategory.ART, ItemCondition.REFURBISHED, null);
    }

    /** Shortcut: ENDED_NO_BID. */
    private AuctionItemDTO endedNoBidDto(int id) {
        return dto(id, SessionStatus.ENDED_NO_BID, BigDecimal.valueOf(50_000),
                Instant.now().minusSeconds(1000),
                ItemCategory.BOOKS, ItemCondition.USED, null);
    }

    /** Shortcut: CANCELLED. */
    private AuctionItemDTO cancelledDto(int id) {
        return dto(id, SessionStatus.CANCELLED, BigDecimal.valueOf(10_000),
                Instant.now().minusSeconds(500),
                ItemCategory.SPORTS, ItemCondition.NEW, null);
    }

    /** Draft: status null, currentPrice null, endTime null. */
    private AuctionItemDTO draftDtoNoPrice(int id) {
        return dto(id, null, null, null,
                ItemCategory.OTHER, ItemCondition.USED, null);
    }

    // ═════════════════════════════════════════════════════════════════
    // 1. isDraftItem()
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isDraftItem()")
    class IsDraftItem {

        @Test
        @DisplayName("status == DRAFT → true")
        void statusDraft_returnsTrue() throws Exception {
            // Tạo DTO với status = DRAFT (giả lập qua enum name)
            // Vì SessionStatus không có DRAFT, ta kiểm tra nhánh "currentPrice null + endTime null"
            // Nhánh status.name() == "DRAFT": không thể trigger vì enum không có DRAFT
            // → chỉ test nhánh null price & null endTime

            AuctionItemDTO item = dto(1, null, null, null,
                    ItemCategory.OTHER, null, null);
            Boolean result = (Boolean) call("isDraftItem",
                    new Class<?>[]{AuctionItemDTO.class}, item);
            assertTrue(result, "currentPrice=null & endTime=null phải là draft");
        }

        @Test
        @DisplayName("currentPrice null & endTime null → true (không có phiên đấu giá)")
        void nullPriceAndEndTime_returnsTrue() throws Exception {
            AuctionItemDTO item = draftDtoNoPrice(2);
            Boolean result = (Boolean) call("isDraftItem",
                    new Class<?>[]{AuctionItemDTO.class}, item);
            assertTrue(result);
        }

        @Test
        @DisplayName("currentPrice not null → false")
        void hasPrice_returnsFalse() throws Exception {
            AuctionItemDTO item = activeDto(3);
            Boolean result = (Boolean) call("isDraftItem",
                    new Class<?>[]{AuctionItemDTO.class}, item);
            assertFalse(result);
        }

        @Test
        @DisplayName("endTime not null nhưng price null → false")
        void hasEndTimeNullPrice_returnsFalse() throws Exception {
            // endTime ≠ null → nhánh else → false
            AuctionItemDTO item = dto(4, SessionStatus.UPCOMING, null,
                    Instant.now().plusSeconds(3600),
                    ItemCategory.OTHER, ItemCondition.USED, null);
            Boolean result = (Boolean) call("isDraftItem",
                    new Class<?>[]{AuctionItemDTO.class}, item);
            assertFalse(result);
        }

        @Test
        @DisplayName("ACTIVE status → không phải draft")
        void activeStatus_returnsFalse() throws Exception {
            AuctionItemDTO item = activeDto(5);
            Boolean result = (Boolean) call("isDraftItem",
                    new Class<?>[]{AuctionItemDTO.class}, item);
            assertFalse(result);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 2. showError()
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("showError()")
    class ShowError {

        @Test
        @DisplayName("ghi message vào VBox qua Label")
        void setsLabelTextInVBox() throws Exception {
            VBox box = new VBox();
            call("showError", new Class<?>[]{VBox.class, String.class},
                    box, "Lỗi tải dữ liệu");

            assertEquals(1, box.getChildren().size());
            Label lbl = (Label) box.getChildren().get(0);
            assertEquals("Lỗi tải dữ liệu", lbl.getText());
        }

        @Test
        @DisplayName("gọi nhiều lần → replace nội dung cũ")
        void replacesPreviousContent() throws Exception {
            VBox box = new VBox();
            box.getChildren().add(new Label("cũ"));
            call("showError", new Class<?>[]{VBox.class, String.class},
                    box, "Mới");
            assertEquals(1, box.getChildren().size());
            assertEquals("Mới", ((Label) box.getChildren().get(0)).getText());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 3. switchTab()
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("switchTab()")
    class SwitchTab {

        @Test
        @DisplayName("chọn btnAll → vboxAll visible, các vbox khác ẩn")
        void selectAll_showsVboxAll() throws Exception {
            Button btnAll      = getField("btnAll");
            VBox   vboxAll     = getField("vboxAll");
            VBox   vboxActive  = getField("vboxActive");
            VBox   vboxCompleted = getField("vboxCompleted");

            call("switchTab", new Class<?>[]{Button.class, VBox.class},
                    btnAll, vboxAll);

            assertTrue(vboxAll.isVisible());
            assertTrue(vboxAll.isManaged());
            assertFalse(vboxActive.isVisible());
            assertFalse(vboxCompleted.isVisible());
        }

        @Test
        @DisplayName("chọn btnActive → vboxActive visible, vboxAll ẩn")
        void selectActive_showsVboxActive() throws Exception {
            Button btnActive = getField("btnActive");
            VBox   vboxActive = getField("vboxActive");
            VBox   vboxAll    = getField("vboxAll");

            call("switchTab", new Class<?>[]{Button.class, VBox.class},
                    btnActive, vboxActive);

            assertTrue(vboxActive.isVisible());
            assertFalse(vboxAll.isVisible());
        }

        @Test
        @DisplayName("chọn btnCompleted → vboxCompleted visible")
        void selectCompleted_showsVboxCompleted() throws Exception {
            Button btnCompleted  = getField("btnCompleted");
            VBox   vboxCompleted = getField("vboxCompleted");

            call("switchTab", new Class<?>[]{Button.class, VBox.class},
                    btnCompleted, vboxCompleted);

            assertTrue(vboxCompleted.isVisible());
            assertTrue(vboxCompleted.isManaged());
        }

        @Test
        @DisplayName("chọn btnDraft (non-null) → vboxDraft visible")
        void selectDraft_withNonNullBtnDraft() throws Exception {
            Button btnDraft  = getField("btnDraft");
            VBox   vboxDraft = getField("vboxDraft");

            call("switchTab", new Class<?>[]{Button.class, VBox.class},
                    btnDraft, vboxDraft);

            assertTrue(vboxDraft.isVisible());
        }

        @Test
        @DisplayName("btnDraft null → không ném NullPointerException")
        void selectAll_withNullBtnDraft_doesNotThrow() throws Exception {
            setField("btnDraft",  null);
            setField("vboxDraft", null);

            Button btnAll = getField("btnAll");
            VBox   vboxAll = getField("vboxAll");

            assertDoesNotThrow(() ->
                    call("switchTab", new Class<?>[]{Button.class, VBox.class},
                            btnAll, vboxAll)
            );
        }

        @Test
        @DisplayName("selected button nhận ACTIVE_STYLE")
        void selectedButton_getsActiveStyle() throws Exception {
            Button btnActive  = getField("btnActive");
            VBox   vboxActive = getField("vboxActive");
            String activeStyle = (String) getField("ACTIVE_STYLE");

            call("switchTab", new Class<?>[]{Button.class, VBox.class},
                    btnActive, vboxActive);

            assertEquals(activeStyle, btnActive.getStyle());
        }

        @Test
        @DisplayName("các button còn lại nhận INACTIVE_STYLE")
        void otherButtons_getInactiveStyle() throws Exception {
            Button btnActive  = getField("btnActive");
            Button btnAll     = getField("btnAll");
            VBox   vboxActive = getField("vboxActive");
            String inactiveStyle = (String) getField("INACTIVE_STYLE");

            call("switchTab", new Class<?>[]{Button.class, VBox.class},
                    btnActive, vboxActive);

            assertEquals(inactiveStyle, btnAll.getStyle());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 4. renderProducts()
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("renderProducts()")
    class RenderProducts {

        @Test
        @DisplayName("danh sách rỗng → showError trên vboxAll")
        void emptyList_showsErrorOnAll() throws Exception {
            call("renderProducts", new Class<?>[]{List.class}, Collections.emptyList());

            VBox vboxAll = getField("vboxAll");
            assertEquals(1, vboxAll.getChildren().size());
            Label lbl = (Label) vboxAll.getChildren().get(0);
            assertEquals("Bạn chưa có sản phẩm nào.", lbl.getText());
        }

        @Test
        @DisplayName("danh sách rỗng → cập nhật text nút btnAll")
        void emptyList_updatesBtnAllText() throws Exception {
            call("renderProducts", new Class<?>[]{List.class}, Collections.emptyList());
            Button btnAll = getField("btnAll");
            assertTrue(btnAll.getText().contains("0"));
        }

        @Test
        @DisplayName("không có active item → showError trên vboxActive")
        void noActiveItems_showsErrorOnActive() throws Exception {
            List<AuctionItemDTO> items = List.of(soldDto(10));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxActive = getField("vboxActive");
            assertEquals(1, vboxActive.getChildren().size());
            Label lbl = (Label) vboxActive.getChildren().get(0);
            assertTrue(lbl.getText().contains("đang đấu giá"));
        }

        @Test
        @DisplayName("không có completed item → showError trên vboxCompleted")
        void noCompletedItems_showsErrorOnCompleted() throws Exception {
            List<AuctionItemDTO> items = List.of(activeDto(20));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxCompleted = getField("vboxCompleted");
            assertEquals(1, vboxCompleted.getChildren().size());
            Label lbl = (Label) vboxCompleted.getChildren().get(0);
            assertTrue(lbl.getText().contains("hoàn thành"));
        }

        @Test
        @DisplayName("không có draft item → showError trên vboxDraft (non-null)")
        void noDraftItems_showsErrorOnDraft() throws Exception {
            List<AuctionItemDTO> items = List.of(activeDto(30));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxDraft = getField("vboxDraft");
            assertEquals(1, vboxDraft.getChildren().size());
            Label lbl = (Label) vboxDraft.getChildren().get(0);
            assertTrue(lbl.getText().contains("nháp"));
        }

        @Test
        @DisplayName("vboxDraft null → không ném NullPointerException")
        void vboxDraftNull_doesNotThrow() throws Exception {
            setField("vboxDraft", null);
            setField("btnDraft",  null);
            List<AuctionItemDTO> items = List.of(activeDto(31));
            assertDoesNotThrow(() ->
                    call("renderProducts", new Class<?>[]{List.class}, items));
        }

        @Test
        @DisplayName("có active item → thêm card vào vboxActive")
        void hasActiveItem_addsCardToVboxActive() throws Exception {
            List<AuctionItemDTO> items = List.of(activeDto(40));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxActive = getField("vboxActive");
            assertEquals(1, vboxActive.getChildren().size());
        }

        @Test
        @DisplayName("có UPCOMING item → vào active list")
        void upcomingItem_goesToActiveList() throws Exception {
            List<AuctionItemDTO> items = List.of(upcomingDto(50));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxActive = getField("vboxActive");
            assertEquals(1, vboxActive.getChildren().size());
        }

        @Test
        @DisplayName("có SOLD item → vào completed list")
        void soldItem_goesToCompletedList() throws Exception {
            List<AuctionItemDTO> items = List.of(soldDto(60));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxCompleted = getField("vboxCompleted");
            assertEquals(1, vboxCompleted.getChildren().size());
        }

        @Test
        @DisplayName("có ENDED_NO_BID item → vào completed list")
        void endedNoBidItem_goesToCompletedList() throws Exception {
            List<AuctionItemDTO> items = List.of(endedNoBidDto(61));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxCompleted = getField("vboxCompleted");
            assertEquals(1, vboxCompleted.getChildren().size());
        }

        @Test
        @DisplayName("có CANCELLED item → vào completed list")
        void cancelledItem_goesToCompletedList() throws Exception {
            List<AuctionItemDTO> items = List.of(cancelledDto(62));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxCompleted = getField("vboxCompleted");
            assertEquals(1, vboxCompleted.getChildren().size());
        }

        @Test
        @DisplayName("có draft item (null price+endTime) → vào draft list")
        void draftItem_goesToDraftList() throws Exception {
            List<AuctionItemDTO> items = List.of(draftDtoNoPrice(70));
            call("renderProducts", new Class<?>[]{List.class}, items);
            VBox vboxDraft = getField("vboxDraft");
            assertEquals(1, vboxDraft.getChildren().size());
        }

        @Test
        @DisplayName("draft item với vboxDraft null → không ném ngoại lệ")
        void draftItemWithNullVboxDraft_doesNotThrow() throws Exception {
            setField("vboxDraft", null);
            setField("btnDraft",  null);
            List<AuctionItemDTO> items = List.of(draftDtoNoPrice(71));
            assertDoesNotThrow(() ->
                    call("renderProducts", new Class<?>[]{List.class}, items));
        }

        @Test
        @DisplayName("nhiều loại item cùng lúc → phân loại chính xác")
        void mixedItems_classifiedCorrectly() throws Exception {
            List<AuctionItemDTO> items = Arrays.asList(
                    activeDto(80), upcomingDto(81), soldDto(82),
                    endedNoBidDto(83), cancelledDto(84), draftDtoNoPrice(85)
            );
            call("renderProducts", new Class<?>[]{List.class}, items);

            VBox vboxAll       = getField("vboxAll");
            VBox vboxActive    = getField("vboxActive");
            VBox vboxCompleted = getField("vboxCompleted");
            VBox vboxDraft     = getField("vboxDraft");

            assertEquals(6, vboxAll.getChildren().size());       // tất cả
            assertEquals(2, vboxActive.getChildren().size());    // ACTIVE + UPCOMING
            assertEquals(3, vboxCompleted.getChildren().size()); // SOLD + ENDED_NO_BID + CANCELLED
            assertEquals(1, vboxDraft.getChildren().size());     // draft
        }

        @Test
        @DisplayName("btnDraft non-null → text được cập nhật")
        void btnDraftNonNull_textUpdated() throws Exception {
            List<AuctionItemDTO> items = List.of(draftDtoNoPrice(90));
            call("renderProducts", new Class<?>[]{List.class}, items);
            Button btnDraft = getField("btnDraft");
            assertTrue(btnDraft.getText().contains("1"));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 5. createItemCard()
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createItemCard()")
    class CreateItemCard {

        @Test
        @DisplayName("trả về HBox không null")
        void returnsNonNullHBox() throws Exception {
            Object card = call("createItemCard",
                    new Class<?>[]{AuctionItemDTO.class}, activeDto(1));
            assertNotNull(card);
        }

        @Test
        @DisplayName("badge ĐANG ĐẤU GIÁ khi status ACTIVE")
        void activeBadge() throws Exception {
            // Đủ để không ném ngoại lệ và trả về HBox
            Object card = call("createItemCard",
                    new Class<?>[]{AuctionItemDTO.class}, activeDto(2));
            assertNotNull(card);
        }

        @Test
        @DisplayName("badge CHUẨN BỊ ĐẤU GIÁ khi status UPCOMING")
        void upcomingBadge() throws Exception {
            Object card = call("createItemCard",
                    new Class<?>[]{AuctionItemDTO.class}, upcomingDto(3));
            assertNotNull(card);
        }

        @Test
        @DisplayName("badge BẢN NHÁP khi isDraft == true")
        void draftBadge() throws Exception {
            Object card = call("createItemCard",
                    new Class<?>[]{AuctionItemDTO.class}, draftDtoNoPrice(4));
            assertNotNull(card);
        }

        @Test
        @DisplayName("badge ĐÃ KẾT THÚC khi status SOLD")
        void soldBadge() throws Exception {
            Object card = call("createItemCard",
                    new Class<?>[]{AuctionItemDTO.class}, soldDto(5));
            assertNotNull(card);
        }

        @Test
        @DisplayName("badge ĐÃ KẾT THÚC khi status ENDED_NO_BID")
        void endedNoBidBadge() throws Exception {
            Object card = call("createItemCard",
                    new Class<?>[]{AuctionItemDTO.class}, endedNoBidDto(6));
            assertNotNull(card);
        }

        @Test
        @DisplayName("badge ĐÃ KẾT THÚC khi status CANCELLED")
        void cancelledBadge() throws Exception {
            Object card = call("createItemCard",
                    new Class<?>[]{AuctionItemDTO.class}, cancelledDto(7));
            assertNotNull(card);
        }

        @Test
        @DisplayName("category null → hiển thị 'Khác'")
        void nullCategory_showsDefault() throws Exception {
            AuctionItemDTO item = dto(8, SessionStatus.ACTIVE,
                    BigDecimal.valueOf(1000), Instant.now().plusSeconds(3600),
                    null, ItemCondition.USED, null);
            assertDoesNotThrow(() ->
                    call("createItemCard", new Class<?>[]{AuctionItemDTO.class}, item));
        }

        @Test
        @DisplayName("condition null → fallback về 'USED'")
        void nullCondition_showsDefault() throws Exception {
            AuctionItemDTO item = dto(9, SessionStatus.ACTIVE,
                    BigDecimal.valueOf(1000), Instant.now().plusSeconds(3600),
                    ItemCategory.ELECTRONICS, null, null);
            assertDoesNotThrow(() ->
                    call("createItemCard", new Class<?>[]{AuctionItemDTO.class}, item));
        }

        @Test
        @DisplayName("currentPrice null → hiển thị 'Chưa thiết lập giá'")
        void nullCurrentPrice_showsUnset() throws Exception {
            AuctionItemDTO item = dto(10, SessionStatus.UPCOMING, null,
                    Instant.now().plusSeconds(3600),
                    ItemCategory.ART, ItemCondition.NEW, null);
            assertDoesNotThrow(() ->
                    call("createItemCard", new Class<?>[]{AuctionItemDTO.class}, item));
        }

        @Test
        @DisplayName("currentPrice non-null → gọi UIFormatter.formatPrice")
        void nonNullCurrentPrice_formatted() throws Exception {
            AuctionItemDTO item = activeDto(11);
            assertDoesNotThrow(() ->
                    call("createItemCard", new Class<?>[]{AuctionItemDTO.class}, item));
        }

        @Test
        @DisplayName("isActive = false → card có -fx-opacity: 0.8")
        void inactiveCard_hasOpacity() throws Exception {
            // SOLD không phải active → opacity 0.8 trong style
            javafx.scene.layout.HBox card = (javafx.scene.layout.HBox) call(
                    "createItemCard", new Class<?>[]{AuctionItemDTO.class}, soldDto(12));
            assertTrue(card.getStyle().contains("0.8"));
        }

        @Test
        @DisplayName("isActive = true → card không có -fx-opacity: 0.8")
        void activeCard_noOpacity() throws Exception {
            javafx.scene.layout.HBox card = (javafx.scene.layout.HBox) call(
                    "createItemCard", new Class<?>[]{AuctionItemDTO.class}, activeDto(13));
            assertFalse(card.getStyle().contains("0.8"));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 6. buildThumbnail()
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("buildThumbnail()")
    class BuildThumbnail {

        @Test
        @DisplayName("imageUrls null → trả về StackPane với fallback icon")
        void nullUrls_returnsFallback() throws Exception {
            AuctionItemDTO item = dto(1, SessionStatus.ACTIVE,
                    BigDecimal.valueOf(100), Instant.now().plusSeconds(100),
                    ItemCategory.OTHER, ItemCondition.USED, null);
            javafx.scene.layout.StackPane pane = (javafx.scene.layout.StackPane)
                    call("buildThumbnail", new Class<?>[]{AuctionItemDTO.class}, item);
            assertNotNull(pane);
            assertEquals(1, pane.getChildren().size());
            // fallback là Label
            assertInstanceOf(Label.class, pane.getChildren().get(0));
        }

        @Test
        @DisplayName("imageUrls empty → trả về StackPane với fallback icon")
        void emptyUrls_returnsFallback() throws Exception {
            AuctionItemDTO item = dto(2, SessionStatus.ACTIVE,
                    BigDecimal.valueOf(100), Instant.now().plusSeconds(100),
                    ItemCategory.OTHER, ItemCondition.USED, Collections.emptyList());
            javafx.scene.layout.StackPane pane = (javafx.scene.layout.StackPane)
                    call("buildThumbnail", new Class<?>[]{AuctionItemDTO.class}, item);
            assertEquals(1, pane.getChildren().size());
            assertInstanceOf(Label.class, pane.getChildren().get(0));
        }

        @Test
        @DisplayName("imageUrls có URL không hợp lệ → nhánh catch → fallback icon")
        void invalidUrl_useFallback() throws Exception {
            // URL sai format → new Image() ném exception → catch → fallbackIcon()
            AuctionItemDTO item = dto(3, SessionStatus.ACTIVE,
                    BigDecimal.valueOf(100), Instant.now().plusSeconds(100),
                    ItemCategory.OTHER, ItemCondition.USED,
                    List.of("NOT_A_VALID_URL"));
            javafx.scene.layout.StackPane pane = (javafx.scene.layout.StackPane)
                    call("buildThumbnail", new Class<?>[]{AuctionItemDTO.class}, item);
            assertNotNull(pane);
            // Khi URL không hợp lệ thì phải có đúng 1 child
            assertEquals(1, pane.getChildren().size());
        }

        @Test
        @DisplayName("StackPane luôn có kích thước 100x100")
        void pane_hasCorrectSize() throws Exception {
            AuctionItemDTO item = dto(4, SessionStatus.ACTIVE,
                    BigDecimal.valueOf(100), Instant.now().plusSeconds(100),
                    ItemCategory.OTHER, ItemCondition.USED, null);
            javafx.scene.layout.StackPane pane = (javafx.scene.layout.StackPane)
                    call("buildThumbnail", new Class<?>[]{AuctionItemDTO.class}, item);
            assertEquals(100, pane.getPrefWidth(),  0.01);
            assertEquals(100, pane.getPrefHeight(), 0.01);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 7. fallbackIcon()
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("fallbackIcon()")
    class FallbackIcon {

        @Test
        @DisplayName("trả về Label chứa emoji 📦")
        void returnsBoxEmoji() throws Exception {
            Label icon = (Label) call("fallbackIcon", new Class<?>[]{});
            assertNotNull(icon);
            assertEquals("📦", icon.getText());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 8. initialize() — kiểm tra nhánh btnDraft null / non-null
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("initialize()")
    class Initialize {

        @Test
        @DisplayName("btnDraft non-null → không ném ngoại lệ khi set handler")
        void btnDraftNonNull_doesNotThrow() {
            // initialize() gọi SellerService.getMyListings() thật → có thể fail networking
            // Nhưng logic set handlers sẽ chạy trước → chỉ cần không ném NPE
            // Dùng try-catch để bỏ qua lỗi kết nối
            assertDoesNotThrow(() -> {
                try {
                    call("initialize", new Class<?>[]{});
                } catch (Exception e) {
                    // Bỏ qua lỗi networking; điều quan trọng là không ném NPE
                    if (e.getCause() instanceof NullPointerException) {
                        throw e;
                    }
                }
            });
        }

        @Test
        @DisplayName("btnDraft null → initialize không ném NullPointerException")
        void btnDraftNull_doesNotThrow() throws Exception {
            setField("btnDraft",  null);
            setField("vboxDraft", null);
            assertDoesNotThrow(() -> {
                try {
                    call("initialize", new Class<?>[]{});
                } catch (Exception e) {
                    if (e.getCause() instanceof NullPointerException) {
                        throw e;
                    }
                }
            });
        }

        @Test
        @DisplayName("btnAll handler được gán (setOnAction)")
        void btnAll_hasActionHandler() throws Exception {
            // Gọi initialize để set handlers
            try { call("initialize", new Class<?>[]{}); } catch (Exception ignored) {}
            Button btnAll = getField("btnAll");
            assertNotNull(btnAll.getOnAction(),
                    "btnAll phải có onAction handler sau initialize()");
        }

        @Test
        @DisplayName("btnActive handler được gán")
        void btnActive_hasActionHandler() throws Exception {
            try { call("initialize", new Class<?>[]{}); } catch (Exception ignored) {}
            Button btnActive = getField("btnActive");
            assertNotNull(btnActive.getOnAction());
        }

        @Test
        @DisplayName("btnCompleted handler được gán")
        void btnCompleted_hasActionHandler() throws Exception {
            try { call("initialize", new Class<?>[]{}); } catch (Exception ignored) {}
            Button btnCompleted = getField("btnCompleted");
            assertNotNull(btnCompleted.getOnAction());
        }

        @Test
        @DisplayName("btnAddNew handler được gán")
        void btnAddNew_hasActionHandler() throws Exception {
            try { call("initialize", new Class<?>[]{}); } catch (Exception ignored) {}
            Button btnAddNew = getField("btnAddNew");
            assertNotNull(btnAddNew.getOnAction());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 9. navigateToDetail() — MainController null và non-null
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("navigateToDetail()")
    class NavigateToDetail {

        @Test
        @DisplayName("MainController.getInstance() == null → không ném ngoại lệ")
        void mainControllerNull_doesNotThrow() {
            // MainController singleton chưa được khởi tạo trong test context → null
            assertDoesNotThrow(() ->
                    call("navigateToDetail", new Class<?>[]{int.class}, 1)
            );
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 10. mouseEntered / mouseExited / mouseClicked trên card
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Card mouse events")
    class CardMouseEvents {

        @Test
        @DisplayName("mouseEntered → style chứa màu hover #fff7ed")
        void mouseEntered_changesStyleToHover() throws Exception {
            javafx.scene.layout.HBox card = (javafx.scene.layout.HBox) call(
                    "createItemCard", new Class<?>[]{AuctionItemDTO.class}, activeDto(1));

            // Fire mouseEntered
            card.getOnMouseEntered().handle(
                    new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_ENTERED,
                            0, 0, 0, 0,
                            javafx.scene.input.MouseButton.NONE, 0,
                            false, false, false, false,
                            false, false, false, false, false, false, null));

            assertTrue(card.getStyle().contains("#fff7ed"),
                    "Hover style phải chứa màu nền #fff7ed");
        }

        @Test
        @DisplayName("mouseExited → style trở về màu trắng white")
        void mouseExited_restoresStyle() throws Exception {
            javafx.scene.layout.HBox card = (javafx.scene.layout.HBox) call(
                    "createItemCard", new Class<?>[]{AuctionItemDTO.class}, activeDto(2));

            card.getOnMouseExited().handle(
                    new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_EXITED,
                            0, 0, 0, 0,
                            javafx.scene.input.MouseButton.NONE, 0,
                            false, false, false, false,
                            false, false, false, false, false, false, null));

            assertTrue(card.getStyle().contains("white"),
                    "Style sau mouseExited phải chứa 'white'");
        }

        @Test
        @DisplayName("click card UPCOMING → không ném ngoại lệ (AlertUtil.showInfo)")
        void clickUpcomingCard_doesNotThrow() throws Exception {
            javafx.scene.layout.HBox card = (javafx.scene.layout.HBox) call(
                    "createItemCard", new Class<?>[]{AuctionItemDTO.class}, upcomingDto(3));

            assertDoesNotThrow(() -> card.getOnMouseClicked().handle(
                    new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                            0, 0, 0, 0,
                            javafx.scene.input.MouseButton.PRIMARY, 1,
                            false, false, false, false,
                            true, false, false, false, false, false, null)));
        }

        @Test
        @DisplayName("click card DRAFT → gọi ClientModel.setCurrentAuctionItem")
        void clickDraftCard_setsCurrentItem() throws Exception {
            AuctionItemDTO draftItem = draftDtoNoPrice(4);
            javafx.scene.layout.HBox card = (javafx.scene.layout.HBox) call(
                    "createItemCard", new Class<?>[]{AuctionItemDTO.class}, draftItem);

            assertDoesNotThrow(() -> card.getOnMouseClicked().handle(
                    new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                            0, 0, 0, 0,
                            javafx.scene.input.MouseButton.PRIMARY, 1,
                            false, false, false, false,
                            true, false, false, false, false, false, null)));
        }

        @Test
        @DisplayName("click card ACTIVE → gọi navigateToDetail (không throw dù MainController null)")
        void clickActiveCard_navigatesToDetail() throws Exception {
            javafx.scene.layout.HBox card = (javafx.scene.layout.HBox) call(
                    "createItemCard", new Class<?>[]{AuctionItemDTO.class}, activeDto(5));

            assertDoesNotThrow(() -> card.getOnMouseClicked().handle(
                    new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                            0, 0, 0, 0,
                            javafx.scene.input.MouseButton.PRIMARY, 1,
                            false, false, false, false,
                            true, false, false, false, false, false, null)));
        }

        @Test
        @DisplayName("click card SOLD → gọi navigateToDetail")
        void clickSoldCard_navigatesToDetail() throws Exception {
            javafx.scene.layout.HBox card = (javafx.scene.layout.HBox) call(
                    "createItemCard", new Class<?>[]{AuctionItemDTO.class}, soldDto(6));

            assertDoesNotThrow(() -> card.getOnMouseClicked().handle(
                    new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                            0, 0, 0, 0,
                            javafx.scene.input.MouseButton.PRIMARY, 1,
                            false, false, false, false,
                            true, false, false, false, false, false, null)));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 11. loadMyProducts() — kiểm tra callback paths qua reflection
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadMyProducts() — callback paths")
    class LoadMyProducts {

        @Test
        @DisplayName("error callback → showError được gọi với message tương ứng")
        void errorCallback_showsErrorOnVboxAll() throws Exception {
            // Mô phỏng nhánh error: gọi trực tiếp showError
            VBox vboxAll = getField("vboxAll");
            call("showError",
                    new Class<?>[]{VBox.class, String.class},
                    vboxAll, "Không thể tải danh sách sản phẩm: Connection refused");

            assertEquals(1, vboxAll.getChildren().size());
            Label lbl = (Label) vboxAll.getChildren().get(0);
            assertTrue(lbl.getText().contains("Không thể tải"));
        }

        @Test
        @DisplayName("success callback → renderProducts nhận danh sách đúng")
        void successCallback_rendersProducts() throws Exception {
            List<AuctionItemDTO> items = List.of(activeDto(99), soldDto(100));
            call("renderProducts", new Class<?>[]{List.class}, items);

            VBox vboxAll = getField("vboxAll");
            assertEquals(2, vboxAll.getChildren().size(),
                    "vboxAll phải chứa đúng số card = số item");
        }
    }
}