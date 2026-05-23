package vn.edu.vnu.uet.group8.common.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link PriceUpdateBroadcastResponse}.
 */
class PriceUpdateBroadcastResponseTest {

    @Test
    @DisplayName("of() - tạo response price update")
    void taoOf() {
        Instant end = Instant.parse("2026-12-31T00:00:00Z");
        PriceUpdateBroadcastResponse res = PriceUpdateBroadcastResponse.of(
                1, new BigDecimal("1500"), 5, end, "bob", false);
        assertEquals(1, res.getItemId());
        assertEquals(new BigDecimal("1500"), res.getNewPrice());
        assertEquals(5, res.getTotalBids());
        assertEquals(end, res.getNewEndTime());
        assertEquals("bob", res.getBidderUsername());
        assertFalse(res.isExtended());
    }

    @Test
    @DisplayName("isExtended() = true khi anti-sniping gia hạn")
    void extended() {
        PriceUpdateBroadcastResponse res = PriceUpdateBroadcastResponse.of(
                1, BigDecimal.TEN, 1, Instant.now(), "alice", true);
        assertTrue(res.isExtended());
    }

    @Test
    @DisplayName("getType() = PRICE_UPDATE")
    void getType() {
        PriceUpdateBroadcastResponse res = PriceUpdateBroadcastResponse.of(
                1, BigDecimal.ONE, 1, Instant.now(), "u", false);
        assertEquals("PRICE_UPDATE", res.getType());
    }
}