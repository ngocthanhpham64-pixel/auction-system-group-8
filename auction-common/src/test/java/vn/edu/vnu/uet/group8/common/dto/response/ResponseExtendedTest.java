package vn.edu.vnu.uet.group8.common.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ResponseExtendedTest {

    // ═══════════════════════════════════════════════════
    // AuctionEndedBroadcastResponse - additional coverage
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("AuctionEndedBroadcastResponse - extended")
    class AuctionEndedExtTest {

        @Test
        @DisplayName("sold() - tất cả getter đúng")
        void soldAllGetters() {
            AuctionEndedBroadcastResponse resp = AuctionEndedBroadcastResponse.sold(
                    10, "iPhone 17 Pro", new BigDecimal("25000000"), "winner_user");

            assertEquals("AUCTION_ENDED", resp.getType());
            assertEquals(10, resp.getItemId());
            assertEquals("iPhone 17 Pro", resp.getItemTitle());
            assertEquals("SOLD", resp.getFinalStatus());
            assertEquals(0, resp.getFinalPrice().compareTo(new BigDecimal("25000000")));
            assertEquals("winner_user", resp.getWinnerUsername());
            assertTrue(resp.hasSold());
        }

        @Test
        @DisplayName("noBid() - tất cả getter đúng, price và winner null")
        void noBidAllGetters() {
            AuctionEndedBroadcastResponse resp =
                    AuctionEndedBroadcastResponse.noBid(5, "Đồng hồ cổ");

            assertEquals("AUCTION_ENDED", resp.getType());
            assertEquals(5, resp.getItemId());
            assertEquals("Đồng hồ cổ", resp.getItemTitle());
            assertEquals("ENDED_NO_BID", resp.getFinalStatus());
            assertNull(resp.getFinalPrice());
            assertNull(resp.getWinnerUsername());
            assertFalse(resp.hasSold());
        }

        @Test
        @DisplayName("hasSold() - SOLD true, ENDED_NO_BID false")
        void hasSoldBothBranches() {
            assertTrue(AuctionEndedBroadcastResponse.sold(1, "T",
                    BigDecimal.ONE, "u").hasSold());
            assertFalse(AuctionEndedBroadcastResponse.noBid(1, "T").hasSold());
        }

        @Test
        @DisplayName("sold với finalPrice = 0 vẫn hợp lệ")
        void soldZeroPrice() {
            AuctionEndedBroadcastResponse resp = AuctionEndedBroadcastResponse.sold(
                    1, "T", BigDecimal.ZERO, "u");
            assertEquals(0, resp.getFinalPrice().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("sold với winnerUsername null vẫn tạo được")
        void soldWinnerNull() {
            assertDoesNotThrow(() -> AuctionEndedBroadcastResponse.sold(
                    1, "T", BigDecimal.ONE, null));
        }
    }

    // ═══════════════════════════════════════════════════
    // BidResponse - additional coverage
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("BidResponse - extended")
    class BidResponseExtTest {

        @Test
        @DisplayName("Constructor - success=false, message=null")
        void failureWithNullMessage() {
            BidResponse resp = new BidResponse(false, null);
            assertFalse(resp.isSuccess());
            assertNull(resp.getMessage());
        }

        @Test
        @DisplayName("success=true, message dài")
        void successLongMessage() {
            String msg = "Đặt giá thành công. Giá hiện tại: 2,000,000 VNĐ. Bạn đang dẫn đầu!";
            BidResponse resp = new BidResponse(true, msg);
            assertTrue(resp.isSuccess());
            assertEquals(msg, resp.getMessage());
        }

        @Test
        @DisplayName("toString chứa success và message")
        void toStringBothFields() {
            BidResponse resp = new BidResponse(true, "OK");
            String s = resp.toString();
            assertTrue(s.contains("success=true"));
            assertTrue(s.contains("OK"));
        }
    }

    // ═══════════════════════════════════════════════════
    // LoginResponse - additional coverage
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("LoginResponse - extended")
    class LoginResponseExtTest {

        @Test
        @DisplayName("success() - tất cả getter đúng")
        void successAllGetters() {
            LoginResponse resp = LoginResponse.success(
                    10, "user01", "Nguyen Van A",
                    "user01@email.com", "MEMBER", "auth-token-xyz");

            assertTrue(resp.isSuccess());
            assertEquals("Đăng nhập thành công", resp.getMessage());
            assertEquals(10, resp.getUserId());
            assertEquals("user01", resp.getUsername());
            assertEquals("Nguyen Van A", resp.getFullName());
            assertEquals("user01@email.com", resp.getEmail());
            assertEquals("MEMBER", resp.getRole());
            assertEquals("auth-token-xyz", resp.getAuthToken());
        }

        @Test
        @DisplayName("failure() - tất cả getter đúng")
        void failureAllGetters() {
            LoginResponse resp = LoginResponse.failure("Sai mật khẩu");

            assertFalse(resp.isSuccess());
            assertEquals("Sai mật khẩu", resp.getMessage());
            assertEquals(0, resp.getUserId());
            assertNull(resp.getUsername());
            assertNull(resp.getFullName());
            assertNull(resp.getEmail());
            assertNull(resp.getRole());
            assertNull(resp.getAuthToken());
        }

        @Test
        @DisplayName("role = ADMIN")
        void roleAdmin() {
            LoginResponse resp = LoginResponse.success(
                    1, "admin", "Admin", "a@e.com", "ADMIN", "tok");
            assertEquals("ADMIN", resp.getRole());
        }

        @Test
        @DisplayName("role = SUPER_ADMIN")
        void roleSuperAdmin() {
            LoginResponse resp = LoginResponse.success(
                    1, "super", "Super", "s@e.com", "SUPER_ADMIN", "tok");
            assertEquals("SUPER_ADMIN", resp.getRole());
        }

        @Test
        @DisplayName("toString không chứa authToken")
        void toStringNoAuthToken() {
            LoginResponse resp = LoginResponse.success(
                    1, "u", "N", "u@e.com", "MEMBER", "SECRET-TOKEN");
            String s = resp.toString();
            assertFalse(s.contains("SECRET-TOKEN"), "authToken không được xuất hiện trong toString");
        }

        @Test
        @DisplayName("toString failure chứa message lỗi")
        void toStringFailure() {
            LoginResponse resp = LoginResponse.failure("Tài khoản bị khoá");
            assertTrue(resp.toString().contains("Tài khoản bị khoá"));
        }
    }

    // ═══════════════════════════════════════════════════
    // PriceUpdateBroadcastResponse - additional coverage
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("PriceUpdateBroadcastResponse - extended")
    class PriceUpdateExtTest {

        @Test
        @DisplayName("of() - tất cả getter đúng")
        void ofAllGetters() {
            Instant endTime = Instant.now().plus(30, ChronoUnit.MINUTES);
            PriceUpdateBroadcastResponse resp = PriceUpdateBroadcastResponse.of(
                    10, new BigDecimal("2500000"), 5,
                    endTime, "bidder_user", true);

            assertEquals("PRICE_UPDATE", resp.getType());
            assertEquals(10, resp.getItemId());
            assertEquals(0, resp.getNewPrice().compareTo(new BigDecimal("2500000")));
            assertEquals(5, resp.getTotalBids());
            assertEquals(endTime, resp.getNewEndTime());
            assertEquals("bidder_user", resp.getBidderUsername());
            assertTrue(resp.isExtended());
        }

        @Test
        @DisplayName("isExtended = false khi không có anti-sniping")
        void notExtended() {
            PriceUpdateBroadcastResponse resp = PriceUpdateBroadcastResponse.of(
                    1, BigDecimal.ONE, 1,
                    Instant.now(), "u", false);
            assertFalse(resp.isExtended());
        }

        @Test
        @DisplayName("newEndTime null được phép")
        void newEndTimeNull() {
            PriceUpdateBroadcastResponse resp = PriceUpdateBroadcastResponse.of(
                    1, BigDecimal.ONE, 0, null, "u", false);
            assertNull(resp.getNewEndTime());
        }

        @Test
        @DisplayName("totalBids = 0 (bid đầu tiên)")
        void totalBidsZero() {
            PriceUpdateBroadcastResponse resp = PriceUpdateBroadcastResponse.of(
                    5, new BigDecimal("1000000"), 0,
                    Instant.now(), "first_bidder", false);
            assertEquals(0, resp.getTotalBids());
        }

        @Test
        @DisplayName("bidderUsername null được phép")
        void bidderUsernameNull() {
            PriceUpdateBroadcastResponse resp = PriceUpdateBroadcastResponse.of(
                    1, BigDecimal.ONE, 1, Instant.now(), null, false);
            assertNull(resp.getBidderUsername());
        }

        @Test
        @DisplayName("type luôn = PRICE_UPDATE")
        void typeLuonPriceUpdate() {
            PriceUpdateBroadcastResponse resp = PriceUpdateBroadcastResponse.of(
                    1, BigDecimal.ONE, 1, Instant.now(), "u", false);
            assertEquals("PRICE_UPDATE", resp.getType());
        }
    }
}