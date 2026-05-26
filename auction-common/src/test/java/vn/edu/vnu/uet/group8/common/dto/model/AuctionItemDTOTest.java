package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

/**
 * Test toàn diện cho {@link AuctionItemDTO}.
 *
 * <p>Phạm vi kiểm thử:
 * <ul>
 *   <li><b>from(AuctionSession, Item, seller, bidCount)</b>: factory chính với session</li>
 *   <li><b>from(null session)</b>: khi item chưa có session → status UPCOMING</li>
 *   <li><b>from với sellerRating</b>: overload đầy đủ thông tin seller</li>
 *   <li><b>of()</b>: factory thủ công với tất cả tham số</li>
 *   <li><b>isActive/isExpired</b>: helper queries cho UI logic</li>
 *   <li><b>imageUrls immutability</b>: trả unmodifiable, null → emptyList</li>
 *   <li><b>setCurrentPrice</b>: cập nhật giá realtime</li>
 *   <li><b>toString</b>: chứa thông tin cơ bản</li>
 * </ul>
 */
@DisplayName("AuctionItemDTO - Unit Tests")
class AuctionItemDTOTest {

    private Item item;
    private AuctionSession session;
    private static final Instant FUTURE = Instant.now().plus(1, ChronoUnit.HOURS);

    @BeforeEach
    void setUp() {
        item = new Item.Builder(1, "iPhone 17 Pro Max", ItemCategory.ELECTRONICS)
                .description("Điện thoại mới nhất của Apple")
                .condition(ItemCondition.NEW)
                .status(ItemStatus.LISTED)
                .imageUrls(List.of("https://img.example.com/1.jpg", "https://img.example.com/2.jpg"))
                .build();
        item.assignId(10);

        session = new AuctionSession.Builder(
                10,
                new BigDecimal("20000000"),
                Instant.now(),
                FUTURE
        ).build();
        session.assignId(5);
    }

    // ═══════════════════════════════════════════════════
    // from(AuctionSession, Item, sellerUsername, bidCount)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("from(session, item, seller, bidCount) - factory chính")
    class FromWithSession {

        @Test
        @DisplayName("Các field cơ bản từ Item được map đúng")
        void fromMapFieldTuItem() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller_xyz", 5);

            assertEquals(10, dto.getItemId());
            assertEquals("iPhone 17 Pro Max", dto.getTitle());
            assertEquals("Điện thoại mới nhất của Apple", dto.getDescription());
            assertEquals(ItemCategory.ELECTRONICS, dto.getCategory());
            assertEquals(ItemCondition.NEW, dto.getCondition());
            assertEquals("seller_xyz", dto.getSellerUsername());
            assertEquals(5, dto.getBidCount());
        }

        @Test
        @DisplayName("Các field từ AuctionSession được map đúng")
        void fromMapFieldTuSession() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller_xyz", 5);

            assertEquals(SessionStatus.UPCOMING, dto.getStatus());
            assertEquals(FUTURE, dto.getEndTime());
            assertEquals(0, dto.getCurrentPrice().compareTo(new BigDecimal("20000000")));
        }

        @Test
        @DisplayName("bidCount = 0 khi không có ai bid")
        void bidCountZero() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller_xyz", 0);
            assertEquals(0, dto.getBidCount());
        }

        @Test
        @DisplayName("createdAt lấy từ Item.getCreatedAt()")
        void createdAtTuItem() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller_xyz", 0);
            assertEquals(item.getCreatedAt(), dto.getCreatedAt());
        }

        @Test
        @DisplayName("imageUrls được copy đúng từ Item")
        void imageUrlsTuItem() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller_xyz", 0);
            assertEquals(2, dto.getImageUrls().size());
            assertTrue(dto.getImageUrls().contains("https://img.example.com/1.jpg"));
        }

        @Test
        @DisplayName("Item không có imageUrls → getImageUrls() trả emptyList")
        void itemKhongCoImageUrls() {
            Item itemKhongAnh = new Item.Builder(1, "Test Item", ItemCategory.OTHER)
                    .imageUrls(List.of())
                    .build();
            itemKhongAnh.assignId(20);

            AuctionItemDTO dto = AuctionItemDTO.from(session, itemKhongAnh, "seller", 0);
            assertNotNull(dto.getImageUrls());
            assertTrue(dto.getImageUrls().isEmpty());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 10, 100, 9999})
        @DisplayName("bidCount với nhiều giá trị khác nhau")
        void bidCountNhieuGiaTri(int count) {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller", count);
            assertEquals(count, dto.getBidCount());
        }
    }

    // ═══════════════════════════════════════════════════
    // from() với session = null
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("from(null session) - item chưa có phiên đấu giá")
    class FromNullSession {

        @Test
        @DisplayName("session null → status = UPCOMING")
        void nullSessionStatusUpcoming() {
            AuctionItemDTO dto = AuctionItemDTO.from(null, item, "seller", 0);
            assertEquals(SessionStatus.UPCOMING, dto.getStatus());
        }

        @Test
        @DisplayName("session null → endTime = null")
        void nullSessionEndTimeNull() {
            AuctionItemDTO dto = AuctionItemDTO.from(null, item, "seller", 0);
            assertNull(dto.getEndTime());
        }

        @Test
        @DisplayName("session null → currentPrice = null")
        void nullSessionCurrentPriceNull() {
            AuctionItemDTO dto = AuctionItemDTO.from(null, item, "seller", 0);
            assertNull(dto.getCurrentPrice());
        }

        @Test
        @DisplayName("session null → các field từ Item vẫn được map đúng")
        void nullSessionItemFieldsOK() {
            AuctionItemDTO dto = AuctionItemDTO.from(null, item, "apple_seller", 0);
            assertEquals(10, dto.getItemId());
            assertEquals("iPhone 17 Pro Max", dto.getTitle());
            assertEquals("apple_seller", dto.getSellerUsername());
        }
    }

    // ═══════════════════════════════════════════════════
    // from() với sellerRating overload
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("from() overload với sellerRating và totalItemsSold")
    class FromWithSellerRating {

        @Test
        @DisplayName("sellerRating và totalItemsSold được set đúng")
        void sellerRatingDuocSet() {
            BigDecimal rating = new BigDecimal("4.8");
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "top_seller", 10, rating, 250);

            assertEquals(0, dto.getSellerRating().compareTo(rating));
            assertEquals(250, dto.getTotalItemsSold());
        }

        @Test
        @DisplayName("sellerRating null - vẫn tạo được DTO")
        void sellerRatingNull() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "new_seller", 0, null, 0);
            assertNull(dto.getSellerRating());
            assertEquals(0, dto.getTotalItemsSold());
        }

        @Test
        @DisplayName("sellerRating = 5.0 (tối đa)")
        void sellerRatingMax() {
            BigDecimal maxRating = new BigDecimal("5.0");
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "perfect_seller", 0, maxRating, 100);
            assertEquals(0, dto.getSellerRating().compareTo(maxRating));
        }

        @Test
        @DisplayName("overload 6 tham số vẫn giữ đúng các field từ overload 4 tham số")
        void overloadGiuFieldTuCo() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller", 3, new BigDecimal("4.5"), 10);
            assertEquals("iPhone 17 Pro Max", dto.getTitle());
            assertEquals(3, dto.getBidCount());
            assertEquals(SessionStatus.UPCOMING, dto.getStatus());
        }
    }

    // ═══════════════════════════════════════════════════
    // from(session, item, seller) - 3 tham số
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("from(session, item, seller) 3 tham số - bidCount mặc định = 0")
    void fromBaThamSoBidCountZero() {
        AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller_3args");
        assertEquals(0, dto.getBidCount());
        assertEquals("seller_3args", dto.getSellerUsername());
    }

    // ═══════════════════════════════════════════════════
    // of() static factory thủ công
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("of() - factory thủ công với tất cả tham số")
    class OfFactory {

        @Test
        @DisplayName("of() với đầy đủ tham số - map đúng")
        void ofDayDu() {
            Instant now = Instant.now();
            AuctionItemDTO dto = AuctionItemDTO.of(
                    99, "MacBook Pro M4", "Laptop mạnh nhất",
                    ItemCategory.ELECTRONICS, ItemCondition.NEW,
                    SessionStatus.ACTIVE, new BigDecimal("50000000"),
                    FUTURE, "macstore",
                    null, List.of("img1.jpg"),
                    7, now
            );

            assertEquals(99, dto.getItemId());
            assertEquals("MacBook Pro M4", dto.getTitle());
            assertEquals("Laptop mạnh nhất", dto.getDescription());
            assertEquals(ItemCategory.ELECTRONICS, dto.getCategory());
            assertEquals(ItemCondition.NEW, dto.getCondition());
            assertEquals(SessionStatus.ACTIVE, dto.getStatus());
            assertEquals(0, dto.getCurrentPrice().compareTo(new BigDecimal("50000000")));
            assertEquals(FUTURE, dto.getEndTime());
            assertEquals("macstore", dto.getSellerUsername());
            assertEquals(7, dto.getBidCount());
            assertEquals(now, dto.getCreatedAt());
        }

        @Test
        @DisplayName("of() với imageUrls null → getImageUrls() trả emptyList")
        void ofImageUrlsNull() {
            AuctionItemDTO dto = AuctionItemDTO.of(
                    1, "T", "D", ItemCategory.OTHER, ItemCondition.USED,
                    SessionStatus.UPCOMING, BigDecimal.TEN,
                    FUTURE, "seller", null, null, 0, Instant.now()
            );
            assertNotNull(dto.getImageUrls());
            assertTrue(dto.getImageUrls().isEmpty());
        }

        @Test
        @DisplayName("of() với imageUrls hợp lệ - trả đúng danh sách")
        void ofImageUrlsHopLe() {
            List<String> urls = List.of("a.jpg", "b.jpg", "c.jpg");
            AuctionItemDTO dto = AuctionItemDTO.of(
                    1, "T", "D", ItemCategory.OTHER, ItemCondition.USED,
                    SessionStatus.UPCOMING, BigDecimal.TEN,
                    FUTURE, "seller", null, urls, 0, Instant.now()
            );
            assertEquals(3, dto.getImageUrls().size());
        }
    }

    // ═══════════════════════════════════════════════════
    // isActive() & isExpired() - UI helpers
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("isActive() và isExpired() - UI helper queries")
    class UiHelpers {

        @Test
        @DisplayName("isActive() = true khi status = ACTIVE")
        void isActiveTrueKhiActive() {
            AuctionItemDTO dto = AuctionItemDTO.of(
                    1, "T", "D", ItemCategory.OTHER, ItemCondition.USED,
                    SessionStatus.ACTIVE, BigDecimal.TEN,
                    FUTURE, "seller", null, null, 0, Instant.now()
            );
            assertTrue(dto.isActive());
        }

        @Test
        @DisplayName("isActive() = false khi status = UPCOMING")
        void isActiveFalseKhiUpcoming() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller", 0);
            assertFalse(dto.isActive(), "UPCOMING session không phải ACTIVE");
        }

        @Test
        @DisplayName("isActive() = false khi status = SOLD")
        void isActiveFalseKhiSold() {
            AuctionItemDTO dto = AuctionItemDTO.of(
                    1, "T", "D", ItemCategory.OTHER, ItemCondition.USED,
                    SessionStatus.SOLD, BigDecimal.TEN,
                    Instant.now().minus(1, ChronoUnit.HOURS),
                    "seller", null, null, 5, Instant.now()
            );
            assertFalse(dto.isActive());
        }

        @Test
        @DisplayName("isActive() = false khi status = CANCELLED")
        void isActiveFalseKhiCancelled() {
            AuctionItemDTO dto = AuctionItemDTO.of(
                    1, "T", "D", ItemCategory.OTHER, ItemCondition.USED,
                    SessionStatus.CANCELLED, BigDecimal.TEN,
                    FUTURE, "seller", null, null, 0, Instant.now()
            );
            assertFalse(dto.isActive());
        }

        @Test
        @DisplayName("isExpired() = true khi endTime đã qua")
        void isExpiredTrueKhiEndTimeDaQua() {
            Instant pastEnd = Instant.now().minus(1, ChronoUnit.HOURS);
            AuctionItemDTO dto = AuctionItemDTO.of(
                    1, "T", "D", ItemCategory.OTHER, ItemCondition.USED,
                    SessionStatus.ENDED_NO_BID, BigDecimal.TEN,
                    pastEnd, "seller", null, null, 0, Instant.now()
            );
            assertTrue(dto.isExpired());
        }

        @Test
        @DisplayName("isExpired() = false khi endTime còn ở tương lai")
        void isExpiredFalseKhiConThoiGian() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller", 0);
            assertFalse(dto.isExpired());
        }

        @Test
        @DisplayName("isExpired() = false khi endTime = null")
        void isExpiredFalseKhiEndTimeNull() {
            AuctionItemDTO dto = AuctionItemDTO.from(null, item, "seller", 0);
            assertNull(dto.getEndTime());
            assertFalse(dto.isExpired(), "endTime null → isExpired() phải false");
        }
    }

    // ═══════════════════════════════════════════════════
    // setCurrentPrice - cập nhật giá realtime
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("setCurrentPrice() - cập nhật giá realtime")
    class SetCurrentPrice {

        @Test
        @DisplayName("setCurrentPrice() cập nhật giá thành công")
        void setCurrentPriceHopLe() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller", 0);
            BigDecimal newPrice = new BigDecimal("25000000");
            dto.setCurrentPrice(newPrice);
            assertEquals(0, dto.getCurrentPrice().compareTo(newPrice));
        }

        @Test
        @DisplayName("setCurrentPrice() nhiều lần - giữ giá trị cuối cùng")
        void setCurrentPriceNhieuLan() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller", 0);
            dto.setCurrentPrice(new BigDecimal("21000000"));
            dto.setCurrentPrice(new BigDecimal("22000000"));
            dto.setCurrentPrice(new BigDecimal("30000000"));
            assertEquals(0, dto.getCurrentPrice().compareTo(new BigDecimal("30000000")));
        }
    }

    // ═══════════════════════════════════════════════════
    // imageUrls IMMUTABILITY
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("imageUrls - immutability")
    class ImageUrlsImmutability {

        @Test
        @DisplayName("getImageUrls() trả unmodifiable - không add được từ ngoài")
        void getImageUrlsUnmodifiable() {
            AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller", 0);
            assertThrows(UnsupportedOperationException.class,
                    () -> dto.getImageUrls().add("hack.jpg"));
        }

        @Test
        @DisplayName("getImageUrls() khi null - trả emptyList, không null")
        void getImageUrlsKhiNullTraEmptyList() {
            Item itemKhongAnh = new Item.Builder(2, "No Image Item", ItemCategory.OTHER).build();
            itemKhongAnh.assignId(20);

            AuctionItemDTO dto = AuctionItemDTO.from(session, itemKhongAnh, "seller", 0);
            assertNotNull(dto.getImageUrls(), "getImageUrls() không được trả null");
        }
    }

    // ═══════════════════════════════════════════════════
    // toString
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("toString() chứa itemId, title, category")
    void toStringChuaThongTinChinh() {
        AuctionItemDTO dto = AuctionItemDTO.from(session, item, "seller", 0);
        String s = dto.toString();

        assertTrue(s.contains("10"), "toString phải chứa itemId");
        assertTrue(s.contains("iPhone 17 Pro Max"), "toString phải chứa title");
        assertTrue(s.contains("ELECTRONICS"), "toString phải chứa category");
    }

    // ═══════════════════════════════════════════════════
    // MỌI ItemCategory - kiểm tra không bị lỗi
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("from() hoạt động với mọi ItemCategory")
    void fromVoiMoiCategory() {
        for (ItemCategory cat : ItemCategory.values()) {
            Item i = new Item.Builder(1, "Test " + cat.name(), cat).build();
            i.assignId(1);
            assertDoesNotThrow(() -> AuctionItemDTO.from(session, i, "seller", 0),
                    "from() không được ném với category " + cat);
        }
    }

    @Test
    @DisplayName("from() hoạt động với mọi ItemCondition")
    void fromVoiMoiCondition() {
        for (ItemCondition cond : ItemCondition.values()) {
            Item i = new Item.Builder(1, "Test", ItemCategory.OTHER)
                    .condition(cond).build();
            i.assignId(1);
            AuctionItemDTO dto = AuctionItemDTO.from(session, i, "seller", 0);
            assertEquals(cond, dto.getCondition());
        }
    }
}