package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link UserBidHistoryDTO}.
 */
class UserBidHistoryDTOTest {

    @Test
    @DisplayName("Factory of() → tất cả field đúng")
    void taoOf() {
        Instant bidTime = Instant.parse("2026-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2026-01-02T10:00:00Z");
        UserBidHistoryDTO dto = UserBidHistoryDTO.of(
                1, 2, 3,
                "iPhone 17",
                new BigDecimal("500"),
                new BigDecimal("700"),
                bidTime,
                endTime);

        assertEquals(1, dto.getBidId());
        assertEquals(2, dto.getSessionId());
        assertEquals(3, dto.getItemId());
        assertEquals("iPhone 17", dto.getItemTitle());
        assertEquals(new BigDecimal("500"), dto.getBidAmount());
        assertEquals(new BigDecimal("700"), dto.getCurrentSessionPrice());
        assertEquals(bidTime, dto.getBidTime());
        assertEquals(endTime, dto.getSessionEndTime());
    }

    @Test
    @DisplayName("Cho phép title null (DTO không validate)")
    void titleNull() {
        UserBidHistoryDTO dto = UserBidHistoryDTO.of(
                1, 1, 1, null,
                BigDecimal.ONE, BigDecimal.ONE,
                Instant.now(), Instant.now());
        assertNull(dto.getItemTitle());
    }
}