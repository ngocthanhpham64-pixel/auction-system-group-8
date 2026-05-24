package vn.edu.vnu.uet.group8.common.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestBasicTest {

    // ═══════════════════════════════════════════════════
    // AutoBidRequest
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("AutoBidRequest")
    class AutoBidRequestTest {

        private AutoBidRequest valid() {
            return AutoBidRequest.builder()
                    .itemId(1).userId(2)
                    .maxPrice(new BigDecimal("500000"))
                    .build();
        }

        @Test
        @DisplayName("Builder hợp lệ - tất cả getter trả đúng")
        void builderHopLe() {
            AutoBidRequest req = AutoBidRequest.builder()
                    .itemId(10).userId(5)
                    .maxPrice(new BigDecimal("1000000"))
                    .stepAmount(new BigDecimal("50000"))
                    .build();

            assertEquals(10, req.getItemId());
            assertEquals(5, req.getUserId());
            assertEquals(0, req.getMaxPrice().compareTo(new BigDecimal("1000000")));
            assertEquals(0, req.getStepAmount().compareTo(new BigDecimal("50000")));
        }

        @Test
        @DisplayName("stepAmount null được phép (optional)")
        void stepAmountNull() {
            AutoBidRequest req = AutoBidRequest.builder()
                    .itemId(1).userId(1)
                    .maxPrice(new BigDecimal("100000"))
                    .build();
            assertNull(req.getStepAmount());
        }

        @Test
        @DisplayName("itemId <= 0 → IllegalArgumentException")
        void itemIdZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    AutoBidRequest.builder().itemId(0).userId(1)
                            .maxPrice(BigDecimal.TEN).build());
        }

        @Test
        @DisplayName("itemId âm → IllegalArgumentException")
        void itemIdNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    AutoBidRequest.builder().itemId(-1).userId(1)
                            .maxPrice(BigDecimal.TEN).build());
        }

        @Test
        @DisplayName("userId <= 0 → IllegalArgumentException")
        void userIdZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    AutoBidRequest.builder().itemId(1).userId(0)
                            .maxPrice(BigDecimal.TEN).build());
        }

        @Test
        @DisplayName("maxPrice null → IllegalArgumentException")
        void maxPriceNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    AutoBidRequest.builder().itemId(1).userId(1)
                            .maxPrice(null).build());
        }

        @Test
        @DisplayName("maxPrice = 0 → IllegalArgumentException")
        void maxPriceZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    AutoBidRequest.builder().itemId(1).userId(1)
                            .maxPrice(BigDecimal.ZERO).build());
        }

        @Test
        @DisplayName("maxPrice âm → IllegalArgumentException")
        void maxPriceNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    AutoBidRequest.builder().itemId(1).userId(1)
                            .maxPrice(new BigDecimal("-1")).build());
        }

        @Test
        @DisplayName("stepAmount = 0 → IllegalArgumentException")
        void stepAmountZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    AutoBidRequest.builder().itemId(1).userId(1)
                            .maxPrice(BigDecimal.TEN)
                            .stepAmount(BigDecimal.ZERO).build());
        }

        @Test
        @DisplayName("stepAmount âm → IllegalArgumentException")
        void stepAmountNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    AutoBidRequest.builder().itemId(1).userId(1)
                            .maxPrice(BigDecimal.TEN)
                            .stepAmount(new BigDecimal("-100")).build());
        }

        @Test
        @DisplayName("toString chứa itemId và userId")
        void toStringCoInfo() {
            String s = valid().toString();
            assertTrue(s.contains("itemId=1"));
            assertTrue(s.contains("userId=2"));
        }
    }

    // ═══════════════════════════════════════════════════
    // BidRequest
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("BidRequest")
    class BidRequestTest {

        @Test
        @DisplayName("Constructor hợp lệ - tất cả getter đúng")
        void constructorHopLe() {
            BidRequest req = new BidRequest(5, 3, new BigDecimal("2000000"));
            assertEquals(5, req.getItemId());
            assertEquals(3, req.getBidderId());
            assertEquals(0, req.getAmount().compareTo(new BigDecimal("2000000")));
        }

        @Test
        @DisplayName("itemId = 0 → IllegalArgumentException")
        void itemIdZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    new BidRequest(0, 1, BigDecimal.TEN));
        }

        @Test
        @DisplayName("itemId âm → IllegalArgumentException")
        void itemIdNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    new BidRequest(-1, 1, BigDecimal.TEN));
        }

        @Test
        @DisplayName("bidderId = 0 → IllegalArgumentException")
        void bidderIdZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    new BidRequest(1, 0, BigDecimal.TEN));
        }

        @Test
        @DisplayName("bidderId âm → IllegalArgumentException")
        void bidderIdNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    new BidRequest(1, -5, BigDecimal.TEN));
        }

        @Test
        @DisplayName("amount null → IllegalArgumentException")
        void amountNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new BidRequest(1, 1, null));
        }

        @Test
        @DisplayName("amount = 0 → IllegalArgumentException")
        void amountZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    new BidRequest(1, 1, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("amount âm → IllegalArgumentException")
        void amountNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    new BidRequest(1, 1, new BigDecimal("-100")));
        }

        @Test
        @DisplayName("toString chứa itemId, bidderId, amount")
        void toStringCoInfo() {
            BidRequest req = new BidRequest(3, 7, new BigDecimal("500000"));
            String s = req.toString();
            assertTrue(s.contains("itemId=3"));
            assertTrue(s.contains("bidderId=7"));
            assertTrue(s.contains("500000"));
        }
    }

    // ═══════════════════════════════════════════════════
    // CreateItemRequest
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("CreateItemRequest")
    class CreateItemRequestTest {

        @Test
        @DisplayName("Constructor đầy đủ - tất cả field đúng")
        void constructorDayDu() {
            List<String> urls = List.of("img1.png", "img2.png");
            CreateItemRequest req = new CreateItemRequest(
                    "iPhone 17", "ELECTRONICS", "NEW",
                    "Điện thoại mới nhất",
                    new BigDecimal("20000000"), new BigDecimal("500000"),
                    72, urls, true, "Apple", "CERT-001");

            assertEquals("iPhone 17", req.name);
            assertEquals("ELECTRONICS", req.category);
            assertEquals("NEW", req.condition);
            assertEquals("Điện thoại mới nhất", req.description);
            assertEquals(0, req.startPrice.compareTo(new BigDecimal("20000000")));
            assertEquals(0, req.bidStep.compareTo(new BigDecimal("500000")));
            assertEquals(72, req.durationHours);
            assertEquals(2, req.imageUrls.size());
            assertTrue(req.hasCert);
            assertEquals("Apple", req.certBody);
            assertEquals("CERT-001", req.certId);
        }

        @Test
        @DisplayName("Không có chứng nhận - hasCert = false, certBody = null")
        void khongCoCertification() {
            CreateItemRequest req = new CreateItemRequest(
                    "Đồng hồ cổ", "WATCHES", "USED_FAIR",
                    "Cổ điển", new BigDecimal("5000000"),
                    new BigDecimal("100000"), 48,
                    List.of(), false, null, null);

            assertFalse(req.hasCert);
            assertNull(req.certBody);
            assertNull(req.certId);
        }

        @Test
        @DisplayName("imageUrls rỗng → list rỗng")
        void imageUrlsRong() {
            CreateItemRequest req = new CreateItemRequest(
                    "Item", "OTHER", null,
                    null, BigDecimal.ONE, null, 24,
                    List.of(), false, null, null);
            assertTrue(req.imageUrls.isEmpty());
        }

        @Test
        @DisplayName("Tất cả field nullable không null-check trong constructor")
        void nullableFields() {
            assertDoesNotThrow(() -> new CreateItemRequest(
                    "Item", "OTHER", null,
                    null, BigDecimal.ONE, null, 24,
                    null, false, null, null));
        }
    }

    // ═══════════════════════════════════════════════════
    // DepositRequest
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("DepositRequest")
    class DepositRequestTest {

        @Test
        @DisplayName("Constructor hợp lệ - getter đúng")
        void constructorHopLe() {
            DepositRequest req = new DepositRequest(new BigDecimal("100000"));
            assertEquals(0, req.getAmount().compareTo(new BigDecimal("100000")));
        }

        @Test
        @DisplayName("amount = 0 → IllegalArgumentException")
        void amountZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    new DepositRequest(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("amount âm → IllegalArgumentException")
        void amountNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    new DepositRequest(new BigDecimal("-1000")));
        }

        @Test
        @DisplayName("amount null → IllegalArgumentException")
        void amountNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new DepositRequest(null));
        }

        @Test
        @DisplayName("amount = 1 (biên dưới hợp lệ)")
        void amountBienDuoi() {
            assertDoesNotThrow(() -> new DepositRequest(BigDecimal.ONE));
        }

        @Test
        @DisplayName("amount rất lớn vẫn hợp lệ")
        void amountRatLon() {
            assertDoesNotThrow(() -> new DepositRequest(new BigDecimal("9999999999")));
        }
    }
}