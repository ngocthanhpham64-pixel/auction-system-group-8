package vn.edu.vnu.uet.group8.common.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link BidResponse} - DTO đơn giản.
 */
class BidResponseTest {

    @Test
    @DisplayName("Constructor success=true")
    void success() {
        BidResponse res = new BidResponse(true, "Đặt giá thành công");
        assertTrue(res.isSuccess());
        assertEquals("Đặt giá thành công", res.getMessage());
    }

    @Test
    @DisplayName("Constructor success=false")
    void failed() {
        BidResponse res = new BidResponse(false, "Giá thấp hơn giá hiện tại");
        assertFalse(res.isSuccess());
        assertEquals("Giá thấp hơn giá hiện tại", res.getMessage());
    }

    @Test
    @DisplayName("Message null vẫn tạo được")
    void messageNull() {
        BidResponse res = new BidResponse(true, null);
        assertTrue(res.isSuccess());
        assertNull(res.getMessage());
    }

    @Test
    @DisplayName("toString() chứa success + message")
    void toStringFull() {
        BidResponse res = new BidResponse(true, "OK");
        String s = res.toString();
        assertTrue(s.toLowerCase().contains("success"));
        assertTrue(s.contains("OK"));
    }
}