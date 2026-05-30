package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
/**
 * Test cho {@link BidRecord} - Builder + validation.
 */
class BidRecordTest {

    /** Helper - tạo builder hợp lệ default. */
    private BidRecord.Builder mauHopLe() {
        return BidRecord.builder()
                .bidId(1)
                .itemId(2)
                .userId(3)
                .displayName("Alice")
                .amount(new BigDecimal("100"))
                .placedAt(Instant.now().minusSeconds(10));
    }

    @Nested
    @DisplayName("Builder hợp lệ")
    class HopLe {

        @Test
        @DisplayName("Build với input hợp lệ → tất cả field đúng")
        void buildHopLe() {
            Instant placed = Instant.now().minusSeconds(60);
            BidRecord rec = BidRecord.builder()
                    .bidId(1).itemId(2).userId(3)
                    .displayName("Bob")
                    .amount(new BigDecimal("500"))
                    .placedAt(placed)
                    .isAutoBid(true)
                    .build();
            assertEquals(1, rec.getBidId());
            assertEquals(2, rec.getItemId());
            assertEquals(3, rec.getUserId());
            assertEquals("Bob", rec.getDisplayName());
            assertEquals(new BigDecimal("500"), rec.getAmount());
            assertEquals(placed, rec.getPlacedAt());
            assertTrue(rec.isAutoBid());
        }

        @Test
        @DisplayName("isAutoBid default = false")
        void isAutoBidDefault() {
            BidRecord rec = mauHopLe().build();
            assertFalse(rec.isAutoBid(), "isAutoBid mặc định false");
        }
    }

    @Nested
    @DisplayName("Validation - bidId")
    class BidIdVal {
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("bidId <= 0 → ném")
        void bidIdSai(int bid) {
            assertThrows(IllegalArgumentException.class,
                    () -> mauHopLe().bidId(bid).build());
        }
    }

    @Nested
    @DisplayName("Validation - itemId")
    class ItemIdVal {
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("itemId <= 0 → ném")
        void itemIdSai(int item) {
            assertThrows(IllegalArgumentException.class,
                    () -> mauHopLe().itemId(item).build());
        }
    }

    @Nested
    @DisplayName("Validation - userId")
    class UserIdVal {
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("userId <= 0 → ném")
        void userIdSai(int user) {
            assertThrows(IllegalArgumentException.class,
                    () -> mauHopLe().userId(user).build());
        }
    }

    @Nested
    @DisplayName("Validation - displayName")
    class DisplayNameVal {
        @Test
        @DisplayName("displayName null → ném")
        void displayNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> mauHopLe().displayName(null).build());
        }

        @Test
        @DisplayName("displayName rỗng → ném")
        void displayRong() {
            assertThrows(IllegalArgumentException.class,
                    () -> mauHopLe().displayName("  ").build());
        }
    }

    @Nested
    @DisplayName("Validation - amount")
    class AmountVal {
        @Test
        @DisplayName("amount null → ném")
        void amountNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> mauHopLe().amount(null).build());
        }

        @Test
        @DisplayName("amount = 0 → ném")
        void amountKhong() {
            assertThrows(IllegalArgumentException.class,
                    () -> mauHopLe().amount(BigDecimal.ZERO).build());
        }

        @Test
        @DisplayName("amount âm → ném")
        void amountAm() {
            assertThrows(IllegalArgumentException.class,
                    () -> mauHopLe().amount(new BigDecimal("-1")).build());
        }
    }

    @Test
    @DisplayName("placedAt tương lai → ném")
    void placedAtTuongLai() {
        assertThrows(IllegalArgumentException.class,
                () -> mauHopLe().placedAt(Instant.now().plusSeconds(3600)).build());
    }

    @Test
    @DisplayName("toString() chứa các field chính")
    void toStringFull() {
        BidRecord rec = mauHopLe().build();
        String s = rec.toString();
        assertTrue(s.contains("1"));
        assertTrue(s.contains("Alice"));
        assertTrue(s.contains("100"));
    }
}