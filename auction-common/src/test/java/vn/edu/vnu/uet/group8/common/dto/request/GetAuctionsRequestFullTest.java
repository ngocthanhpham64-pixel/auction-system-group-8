package vn.edu.vnu.uet.group8.common.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;

@DisplayName("GetAuctionsRequest - Full Coverage")
class GetAuctionsRequestFullTest {

    private GetAuctionsRequest.Builder valid() {
        return GetAuctionsRequest.builder();
    }

    // ════════════════════════════════════════════════════
    // Builder - defaults
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Builder - default values")
    class DefaultsTest {

        @Test @DisplayName("Tất cả default đúng khi không set gì")
        void allDefaults() {
            GetAuctionsRequest req = valid().build();
            assertNull(req.getCategory());
            assertNull(req.getMinPrice());
            assertNull(req.getMaxPrice());
            assertEquals(GetAuctionsRequest.SortOption.NEWEST, req.getSortBy());
            assertEquals(GetAuctionsRequest.DEFAULT_PAGE, req.getPage());
            assertEquals(GetAuctionsRequest.DEFAULT_PAGE_SIZE, req.getPageSize());
        }

        @Test @DisplayName("DEFAULT_PAGE = 1")
        void defaultPage() { assertEquals(1, GetAuctionsRequest.DEFAULT_PAGE); }

        @Test @DisplayName("DEFAULT_PAGE_SIZE = 20")
        void defaultPageSize() { assertEquals(20, GetAuctionsRequest.DEFAULT_PAGE_SIZE); }

        @Test @DisplayName("MAX_PAGE_SIZE = 100")
        void maxPageSize() { assertEquals(100, GetAuctionsRequest.MAX_PAGE_SIZE); }
    }

    // ════════════════════════════════════════════════════
    // Builder - tất cả setter methods
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Builder - setters")
    class SettersTest {

        @Test @DisplayName("category(ELECTRONICS) đúng")
        void categoryElectronics() {
            assertEquals(ItemCategory.ELECTRONICS,
                    valid().category(ItemCategory.ELECTRONICS).build().getCategory());
        }

        @Test @DisplayName("category(null) → null")
        void categoryNull() {
            assertNull(valid().category(null).build().getCategory());
        }

        @Test @DisplayName("minPrice set đúng")
        void minPrice() {
            BigDecimal min = new BigDecimal("100000");
            assertEquals(0, valid().minPrice(min).build().getMinPrice().compareTo(min));
        }

        @Test @DisplayName("maxPrice set đúng")
        void maxPrice() {
            BigDecimal max = new BigDecimal("5000000");
            assertEquals(0, valid().maxPrice(max).build().getMaxPrice().compareTo(max));
        }

        @Test @DisplayName("page = 1 biên dưới")
        void pageMin() { assertEquals(1, valid().page(1).build().getPage()); }

        @Test @DisplayName("page = 999 lớn")
        void pageLarge() { assertEquals(999, valid().page(999).build().getPage()); }

        @Test @DisplayName("pageSize = 1 biên dưới")
        void pageSizeMin() { assertEquals(1, valid().pageSize(1).build().getPageSize()); }

        @Test @DisplayName("pageSize = MAX_PAGE_SIZE biên trên")
        void pageSizeMax() {
            assertEquals(GetAuctionsRequest.MAX_PAGE_SIZE,
                    valid().pageSize(GetAuctionsRequest.MAX_PAGE_SIZE).build().getPageSize());
        }

        @ParameterizedTest
        @EnumSource(GetAuctionsRequest.SortOption.class)
        @DisplayName("Tất cả SortOption đều build được")
        void allSortOptions(GetAuctionsRequest.SortOption opt) {
            assertEquals(opt, valid().sortBy(opt).build().getSortBy());
        }

        @Test @DisplayName("Builder chain fluent - tất cả fields")
        void builderChain() {
            GetAuctionsRequest req = valid()
                    .category(ItemCategory.WATCHES)
                    .minPrice(new BigDecimal("100000"))
                    .maxPrice(new BigDecimal("1000000"))
                    .sortBy(GetAuctionsRequest.SortOption.PRICE_ASC)
                    .page(2)
                    .pageSize(50)
                    .build();

            assertEquals(ItemCategory.WATCHES, req.getCategory());
            assertEquals(0, req.getMinPrice().compareTo(new BigDecimal("100000")));
            assertEquals(0, req.getMaxPrice().compareTo(new BigDecimal("1000000")));
            assertEquals(GetAuctionsRequest.SortOption.PRICE_ASC, req.getSortBy());
            assertEquals(2, req.getPage());
            assertEquals(50, req.getPageSize());
        }
    }

    // ════════════════════════════════════════════════════
    // Builder.build() - validation ALL branches
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("Builder.build() - validation branches")
    class ValidationTest {

        @Test @DisplayName("page = 0 → IllegalArgumentException")
        void pageZero() {
            assertThrows(IllegalArgumentException.class, () -> valid().page(0).build());
        }

        @Test @DisplayName("page âm → IllegalArgumentException")
        void pageNegative() {
            assertThrows(IllegalArgumentException.class, () -> valid().page(-1).build());
        }

        @Test @DisplayName("pageSize = 0 → IllegalArgumentException")
        void pageSizeZero() {
            assertThrows(IllegalArgumentException.class, () -> valid().pageSize(0).build());
        }

        @Test @DisplayName("pageSize âm → IllegalArgumentException")
        void pageSizeNegative() {
            assertThrows(IllegalArgumentException.class, () -> valid().pageSize(-5).build());
        }

        @Test @DisplayName("pageSize > MAX_PAGE_SIZE → IllegalArgumentException")
        void pageSizeOverMax() {
            assertThrows(IllegalArgumentException.class,
                    () -> valid().pageSize(GetAuctionsRequest.MAX_PAGE_SIZE + 1).build());
        }

        @Test @DisplayName("pageSize = MAX_PAGE_SIZE + 100 → IllegalArgumentException")
        void pageSizeWayOverMax() {
            assertThrows(IllegalArgumentException.class,
                    () -> valid().pageSize(GetAuctionsRequest.MAX_PAGE_SIZE + 100).build());
        }

        @Test @DisplayName("minPrice > maxPrice → IllegalArgumentException")
        void minMoreThanMax() {
            assertThrows(IllegalArgumentException.class, () ->
                    valid().minPrice(new BigDecimal("5000000"))
                            .maxPrice(new BigDecimal("1000000")).build());
        }

        @Test @DisplayName("minPrice = maxPrice hợp lệ (biên)")
        void minEqualsMax() {
            assertDoesNotThrow(() -> valid()
                    .minPrice(new BigDecimal("1000000"))
                    .maxPrice(new BigDecimal("1000000")).build());
        }

        @Test @DisplayName("minPrice âm → IllegalArgumentException")
        void minPriceNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> valid().minPrice(new BigDecimal("-1")).build());
        }

        @Test @DisplayName("minPrice = 0 hợp lệ (biên dưới)")
        void minPriceZero() {
            assertDoesNotThrow(() -> valid().minPrice(BigDecimal.ZERO).build());
        }

        @Test @DisplayName("maxPrice âm → IllegalArgumentException")
        void maxPriceNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> valid().maxPrice(new BigDecimal("-100")).build());
        }

        @Test @DisplayName("maxPrice = 0 hợp lệ")
        void maxPriceZero() {
            assertDoesNotThrow(() -> valid().maxPrice(BigDecimal.ZERO).build());
        }

        @Test @DisplayName("sortBy = null → IllegalArgumentException")
        void sortByNull() {
            assertThrows(IllegalArgumentException.class, () -> valid().sortBy(null).build());
        }

        @Test @DisplayName("minPrice null, maxPrice có giá trị → hợp lệ")
        void minNullMaxSet() {
            assertDoesNotThrow(() -> valid().maxPrice(new BigDecimal("5000000")).build());
        }

        @Test @DisplayName("minPrice có giá trị, maxPrice null → hợp lệ")
        void minSetMaxNull() {
            assertDoesNotThrow(() -> valid().minPrice(new BigDecimal("100000")).build());
        }

        @Test @DisplayName("Cả minPrice và maxPrice null → hợp lệ")
        void bothPriceNull() {
            assertDoesNotThrow(() -> valid().build());
        }
    }

    // ════════════════════════════════════════════════════
    // SortOption enum
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("SortOption enum")
    class SortOptionTest {

        @Test @DisplayName("NEWEST tồn tại")
        void newest() { assertNotNull(GetAuctionsRequest.SortOption.NEWEST); }

        @Test @DisplayName("ENDING_SOON tồn tại")
        void endingSoon() { assertNotNull(GetAuctionsRequest.SortOption.ENDING_SOON); }

        @Test @DisplayName("PRICE_ASC tồn tại")
        void priceAsc() { assertNotNull(GetAuctionsRequest.SortOption.PRICE_ASC); }

        @Test @DisplayName("PRICE_DESC tồn tại")
        void priceDesc() { assertNotNull(GetAuctionsRequest.SortOption.PRICE_DESC); }

        @Test @DisplayName("HOT tồn tại")
        void hot() { assertNotNull(GetAuctionsRequest.SortOption.HOT); }

        @Test @DisplayName("5 SortOption tổng cộng")
        void count() { assertEquals(5, GetAuctionsRequest.SortOption.values().length); }
    }

    // ════════════════════════════════════════════════════
    // toString
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test @DisplayName("toString chứa tất cả field")
        void containsAllFields() {
            GetAuctionsRequest req = valid()
                    .category(ItemCategory.ELECTRONICS)
                    .sortBy(GetAuctionsRequest.SortOption.HOT)
                    .page(3).pageSize(10).build();
            String s = req.toString();
            assertTrue(s.contains("ELECTRONICS"));
            assertTrue(s.contains("HOT"));
            assertTrue(s.contains("page=3"));
            assertTrue(s.contains("pageSize=10"));
        }

        @Test @DisplayName("toString khi category null - không NPE")
        void toStringNullCategory() {
            assertDoesNotThrow(() -> valid().build().toString());
        }

        @Test @DisplayName("toString khi minPrice/maxPrice null - không NPE")
        void toStringNullPrices() {
            assertDoesNotThrow(() -> valid().build().toString());
        }
    }
}