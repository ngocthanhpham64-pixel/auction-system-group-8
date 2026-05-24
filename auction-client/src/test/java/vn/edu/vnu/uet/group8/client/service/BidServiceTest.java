package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BidService (client-side validation)")
class BidServiceTest {

    @BeforeEach
    void clearSession() {
        SessionManager.clearSession();
    }

    // =========================================================
    // placeBid – validation
    // =========================================================
    @Nested
    @DisplayName("placeBid – validate")
    class PlaceBidValidate {

        @Test
        @DisplayName("chưa đăng nhập -> BidResponse.success = false")
        void notLoggedInReturnsFalse() {

            AtomicReference<BidResponse> result =
                    new AtomicReference<>();

            BidService.placeBid(
                    1,
                    new BigDecimal("100000"),
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get().isSuccess());
            assertTrue(
                    result.get().getMessage().contains("đăng nhập")
            );
        }

        @Test
        @DisplayName("amount null -> BidResponse.success = false")
        void nullAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<BidResponse> result =
                    new AtomicReference<>();

            BidService.placeBid(
                    1,
                    null,
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get().isSuccess());
        }

        @Test
        @DisplayName("amount = 0 -> BidResponse.success = false")
        void zeroAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<BidResponse> result =
                    new AtomicReference<>();

            BidService.placeBid(
                    1,
                    BigDecimal.ZERO,
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get().isSuccess());
        }

        @Test
        @DisplayName("amount âm -> BidResponse.success = false")
        void negativeAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<BidResponse> result =
                    new AtomicReference<>();

            BidService.placeBid(
                    1,
                    new BigDecimal("-1"),
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get().isSuccess());
        }

        @Test
        @DisplayName(
                "đã đăng nhập, amount hợp lệ, không kết nối -> false"
        )
        void noConnectionReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<BidResponse> result =
                    new AtomicReference<>();

            BidService.placeBid(
                    1,
                    new BigDecimal("500000"),
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get().isSuccess());
        }
    }

    // =========================================================
    // setAutoBid – validation
    // =========================================================
    @Nested
    @DisplayName("setAutoBid – validate")
    class SetAutoBidValidate {

        @Test
        @DisplayName("chưa đăng nhập -> false")
        void notLoggedInReturnsFalse() {

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            BidService.setAutoBid(
                    1,
                    new BigDecimal("1000000"),
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }

        @Test
        @DisplayName("maxAmount null -> false")
        void nullMaxAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            BidService.setAutoBid(
                    1,
                    null,
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }

        @Test
        @DisplayName("maxAmount = 0 -> false")
        void zeroMaxAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            BidService.setAutoBid(
                    1,
                    BigDecimal.ZERO,
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }

        @Test
        @DisplayName("maxAmount âm -> false")
        void negativeMaxAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            BidService.setAutoBid(
                    1,
                    new BigDecimal("-100"),
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }

        @Test
        @DisplayName("không kết nối -> false")
        void noConnectionReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            BidService.setAutoBid(
                    1,
                    new BigDecimal("1000000"),
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }
    }

    // =========================================================
    // loadHistory – no connection
    // =========================================================
    @Nested
    @DisplayName("loadHistory – no connection")
    class LoadHistory {

        @Test
        @DisplayName("không kết nối -> trả về list rỗng")
        void noConnectionReturnsEmptyList() {

            AtomicReference<List<BidRecord>> result =
                    new AtomicReference<>();

            BidService.loadHistory(
                    1,
                    result::set
            );

            assertNotNull(result.get());
            assertTrue(result.get().isEmpty());
        }
    }
}