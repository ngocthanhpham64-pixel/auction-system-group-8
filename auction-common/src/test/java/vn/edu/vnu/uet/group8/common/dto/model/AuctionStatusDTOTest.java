package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link AuctionStatusDTO} - constructor + immutability.
 */
class AuctionStatusDTOTest {

    @Test
    @DisplayName("Constructor hợp lệ với bidHistory")
    void hopLe() {
        Instant end = Instant.now().plusSeconds(3600);
        List<BidRecord> history = List.of(
                BidRecord.builder().bidId(1).itemId(1).userId(1)
                        .displayName("a").amount(BigDecimal.TEN)
                        .placedAt(Instant.now().minusSeconds(10)).build()
        );
        AuctionStatusDTO dto = new AuctionStatusDTO(1, new BigDecimal("100"), end, history);
        assertEquals(1, dto.getItemId());
        assertEquals(new BigDecimal("100"), dto.getCurrentPrice());
        assertEquals(end, dto.getEndTime());
        assertEquals(1, dto.getBidHistory().size());
    }

    @Test
    @DisplayName("bidHistory = null → tự coi như List.of()")
    void historyNull() {
        AuctionStatusDTO dto = new AuctionStatusDTO(
                1, BigDecimal.ONE, Instant.now(), null);
        assertNotNull(dto.getBidHistory());
        assertTrue(dto.getBidHistory().isEmpty());
    }

    @Test
    @DisplayName("itemId <= 0 → ném")
    void itemIdSai() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionStatusDTO(0, BigDecimal.ONE, Instant.now(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionStatusDTO(-1, BigDecimal.ONE, Instant.now(), null));
    }

    @Test
    @DisplayName("currentPrice null → ném")
    void priceNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionStatusDTO(1, null, Instant.now(), null));
    }

    @Test
    @DisplayName("currentPrice âm → ném")
    void priceAm() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionStatusDTO(1, new BigDecimal("-1"), Instant.now(), null));
    }

    @Test
    @DisplayName("currentPrice = 0 → hợp lệ (giá khởi điểm)")
    void priceKhong() {
        assertDoesNotThrow(() -> new AuctionStatusDTO(1, BigDecimal.ZERO, Instant.now(), null));
    }

    @Test
    @DisplayName("endTime null → ném")
    void endNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionStatusDTO(1, BigDecimal.ONE, null, null));
    }

    @Test
    @DisplayName("bidHistory được copy - sửa list gốc KHÔNG ảnh hưởng DTO")
    void historyImmutable() {
        BidRecord rec = BidRecord.builder().bidId(1).itemId(1).userId(1)
                .displayName("a").amount(BigDecimal.TEN)
                .placedAt(Instant.now().minusSeconds(10)).build();
        java.util.ArrayList<BidRecord> mutable = new java.util.ArrayList<>(List.of(rec));
        AuctionStatusDTO dto = new AuctionStatusDTO(1, BigDecimal.ONE, Instant.now(), mutable);

        mutable.clear();
        assertEquals(1, dto.getBidHistory().size(),
                "DTO không bị ảnh hưởng khi list gốc bị clear");
    }

    @Test
    @DisplayName("getBidHistory() trả unmodifiable - không sửa được")
    void historyUnmodifiable() {
        AuctionStatusDTO dto = new AuctionStatusDTO(
                1, BigDecimal.ONE, Instant.now(), Collections.emptyList());
        assertThrows(UnsupportedOperationException.class,
                () -> dto.getBidHistory().add(null));
    }

    @Test
    @DisplayName("toString() chứa các field chính")
    void toStringFull() {
        AuctionStatusDTO dto = new AuctionStatusDTO(
                42, new BigDecimal("999"), Instant.now(), null);
        String s = dto.toString();
        assertTrue(s.contains("42"));
        assertTrue(s.contains("999"));
    }
}