package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test cho {@link NotificationDTO} - Builder đơn giản.
 */
class NotificationDTOTest {

    @Test
    @DisplayName("Builder full → tất cả field đúng")
    void buildFull() {
        Instant now = Instant.now();
        NotificationDTO dto = NotificationDTO.builder()
                .id(1)
                .userId(2)
                .title("Bạn vừa thắng đấu giá")
                .message("Sản phẩm iPhone đã thuộc về bạn")
                .isRead(false)
                .createdAt(now)
                .type("AUCTION_WON")
                .build();
        assertEquals(1, dto.getId());
        assertEquals(2, dto.getUserId());
        assertEquals("Bạn vừa thắng đấu giá", dto.getTitle());
        assertEquals("Sản phẩm iPhone đã thuộc về bạn", dto.getMessage());
        assertFalse(dto.isRead());
        assertEquals(now, dto.getCreatedAt());
        assertEquals("AUCTION_WON", dto.getType());
    }

    @Test
    @DisplayName("Builder rỗng → field default")
    void buildRong() {
        NotificationDTO dto = NotificationDTO.builder().build();
        assertEquals(0, dto.getId());
        assertEquals(0, dto.getUserId());
        assertNull(dto.getTitle());
        assertFalse(dto.isRead());
    }

    @Test
    @DisplayName("isRead = true sau khi user đọc")
    void isReadTrue() {
        NotificationDTO dto = NotificationDTO.builder()
                .id(1).isRead(true).build();
        assertTrue(dto.isRead());
    }

    @Test
    @DisplayName("Mỗi field set độc lập")
    void setDocLap() {
        NotificationDTO a = NotificationDTO.builder().title("A").build();
        NotificationDTO b = NotificationDTO.builder().title("B").build();
        assertNotEquals(a.getTitle(), b.getTitle());
    }
}