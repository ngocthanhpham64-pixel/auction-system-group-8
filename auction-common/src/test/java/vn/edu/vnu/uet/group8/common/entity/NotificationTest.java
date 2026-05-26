package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.vnu.uet.group8.common.enums.NotificationType;

/**
 * Test toàn diện cho {@link Notification} entity.
 *
 * <p>Phạm vi:
 * <ul>
 *   <li><b>Builder pattern</b>: tạo mới với mọi NotificationType</li>
 *   <li><b>isRead state</b>: mặc định false, markAsRead() chuyển thành true</li>
 *   <li><b>Reconstructor (Reconstruct) pattern</b>: nạp từ DB</li>
 *   <li><b>Immutable fields</b>: userId, title, message, type</li>
 *   <li><b>ID management & soft delete</b>: kế thừa từ Entity</li>
 *   <li><b>markAsRead idempotency</b>: gọi nhiều lần vẫn đúng</li>
 * </ul>
 */
@DisplayName("Notification - Unit Tests")
class NotificationTest {

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════

    private Notification buildDefault() {
        return Notification.builder()
                .userId(1)
                .title("Bạn đã bị vượt giá")
                .message("Người khác đã đặt giá cao hơn bạn cho sản phẩm iPhone 17")
                .type(NotificationType.OUTBID)
                .build();
    }

    private Notification reconstructDefault() {
        return Notification.reconstruct(
                new Notification.Reconstruct()
                        .id(100)
                        .createdAt(Instant.now())
                        .isDeleted(false)
                        .userId(1)
                        .title("Thắng đấu giá!")
                        .message("Bạn đã thắng phiên đấu giá sản phẩm Rolex")
                        .type(NotificationType.AUCTION_WON)
                        .isRead(false)
        );
    }

    // ═══════════════════════════════════════════════════
    // BUILDER
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Builder pattern")
    class BuilderTest {

        @Test
        @DisplayName("Builder hợp lệ - tạo Notification thành công")
        void builderHopLe() {
            Notification n = buildDefault();

            assertNotNull(n);
            assertEquals(1, n.getUserId());
            assertEquals("Bạn đã bị vượt giá", n.getTitle());
            assertEquals("Người khác đã đặt giá cao hơn bạn cho sản phẩm iPhone 17", n.getMessage());
            assertEquals(NotificationType.OUTBID, n.getType());
        }

        @Test
        @DisplayName("isRead = false ngay khi tạo mới (chưa được đọc)")
        void isReadFalseMacDinh() {
            Notification n = buildDefault();
            assertFalse(n.isRead(), "Notification mới tạo phải chưa được đọc");
        }

        @Test
        @DisplayName("id = 0 khi tạo mới (chưa persist)")
        void idZeroKhiTaoMoi() {
            Notification n = buildDefault();
            assertEquals(0, n.getId());
            assertFalse(n.isPersisted());
        }

        @Test
        @DisplayName("isDeleted = false khi tạo mới")
        void isDeletedFalse() {
            Notification n = buildDefault();
            assertFalse(n.isDeleted());
        }

        @Test
        @DisplayName("createdAt không null khi tạo mới")
        void createdAtKhongNull() {
            Instant before = Instant.now().minusSeconds(1);
            Notification n = buildDefault();
            Instant after = Instant.now().plusSeconds(1);

            assertNotNull(n.getCreatedAt());
            assertTrue(n.getCreatedAt().isAfter(before));
            assertTrue(n.getCreatedAt().isBefore(after));
        }

        @ParameterizedTest
        @EnumSource(NotificationType.class)
        @DisplayName("Builder chấp nhận mọi NotificationType")
        void builderChapNhanMoiType(NotificationType type) {
            Notification n = Notification.builder()
                    .userId(1)
                    .title("Test")
                    .message("Nội dung test")
                    .type(type)
                    .build();

            assertEquals(type, n.getType());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 99, 1000, Integer.MAX_VALUE})
        @DisplayName("Builder với nhiều userId khác nhau")
        void builderNhieuUserId(int userId) {
            Notification n = Notification.builder()
                    .userId(userId)
                    .title("T")
                    .message("M")
                    .type(NotificationType.SYSTEM)
                    .build();
            assertEquals(userId, n.getUserId());
        }
    }

    // ═══════════════════════════════════════════════════
    // isRead STATE MACHINE
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("isRead - trạng thái đọc thông báo")
    class IsReadState {

        @Test
        @DisplayName("markAsRead() chuyển isRead từ false → true")
        void markAsReadChuyenTrangThai() {
            Notification n = buildDefault();
            assertFalse(n.isRead());

            n.markAsRead();
            assertTrue(n.isRead());
        }

        @Test
        @DisplayName("markAsRead() nhiều lần - vẫn là true (idempotent)")
        void markAsReadIdempotent() {
            Notification n = buildDefault();
            n.markAsRead();
            n.markAsRead();
            n.markAsRead();
            assertTrue(n.isRead(), "Gọi markAsRead() nhiều lần vẫn phải true");
        }

        @Test
        @DisplayName("Notification reconstruct với isRead = true - đã đọc từ DB")
        void reconstructWithIsReadTrue() {
            Notification n = Notification.reconstruct(
                    new Notification.Reconstruct()
                            .id(1).createdAt(Instant.now()).isDeleted(false)
                            .userId(1).title("T").message("M")
                            .type(NotificationType.SYSTEM).isRead(true)
            );
            assertTrue(n.isRead());
        }

        @Test
        @DisplayName("Notification reconstruct với isRead = false - chưa đọc từ DB")
        void reconstructWithIsReadFalse() {
            Notification n = Notification.reconstruct(
                    new Notification.Reconstruct()
                            .id(1).createdAt(Instant.now()).isDeleted(false)
                            .userId(1).title("T").message("M")
                            .type(NotificationType.OUTBID).isRead(false)
            );
            assertFalse(n.isRead());
        }
    }

    // ═══════════════════════════════════════════════════
    // RECONSTRUCTOR
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Reconstructor (Reconstruct) pattern - nạp từ DB")
    class ReconstructorTest {

        @Test
        @DisplayName("Reconstruct.build() - build thành công")
        void reconstructBuildHopLe() {
            Instant now = Instant.now();
            Notification n = new Notification.Reconstruct()
                    .id(55)
                    .createdAt(now)
                    .isDeleted(false)
                    .userId(7)
                    .title("Sản phẩm yêu thích lên sàn")
                    .message("Rolex Submariner vừa bắt đầu đấu giá")
                    .type(NotificationType.FAVORITE_STARTED)
                    .isRead(true)
                    .build();

            assertEquals(55, n.getId());
            assertEquals(7, n.getUserId());
            assertEquals("Sản phẩm yêu thích lên sàn", n.getTitle());
            assertEquals(NotificationType.FAVORITE_STARTED, n.getType());
            assertTrue(n.isRead());
            assertEquals(now, n.getCreatedAt());
        }

        @Test
        @DisplayName("Notification.reconstruct() static method hoạt động")
        void staticReconstructHopLe() {
            Notification n = reconstructDefault();
            assertEquals(100, n.getId());
            assertEquals(NotificationType.AUCTION_WON, n.getType());
            assertTrue(n.isPersisted());
        }

        @Test
        @DisplayName("Reconstruct với isDeleted = true - soft-deleted record")
        void reconstructSoftDeleted() {
            Notification n = new Notification.Reconstruct()
                    .id(1).createdAt(Instant.now()).isDeleted(true)
                    .userId(1).title("T").message("M")
                    .type(NotificationType.SYSTEM).isRead(false)
                    .build();
            assertTrue(n.isDeleted());
        }

        @Test
        @DisplayName("reconstructor() instance method trả về Reconstruct object mới")
        void instanceReconstructorMethod() {
            Notification n = buildDefault();
            Notification.Reconstruct r = n.reconstructor();
            assertNotNull(r, "reconstructor() không được trả null");
        }
    }

    // ═══════════════════════════════════════════════════
    // IMMUTABILITY - fields không thay đổi
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Immutable fields - userId, title, message, type")
    class ImmutableFields {

        @Test
        @DisplayName("userId không thay đổi sau markAsRead")
        void userIdKhongDoiSauMarkAsRead() {
            Notification n = buildDefault();
            n.markAsRead();
            assertEquals(1, n.getUserId());
        }

        @Test
        @DisplayName("title không thay đổi sau markAsRead")
        void titleKhongDoi() {
            Notification n = buildDefault();
            n.markAsRead();
            assertEquals("Bạn đã bị vượt giá", n.getTitle());
        }

        @Test
        @DisplayName("message không thay đổi sau markAsRead")
        void messageKhongDoi() {
            Notification n = buildDefault();
            n.markAsRead();
            assertEquals("Người khác đã đặt giá cao hơn bạn cho sản phẩm iPhone 17", n.getMessage());
        }

        @Test
        @DisplayName("type không thay đổi sau markAsRead")
        void typeKhongDoi() {
            Notification n = buildDefault();
            n.markAsRead();
            assertEquals(NotificationType.OUTBID, n.getType());
        }
    }

    // ═══════════════════════════════════════════════════
    // ID MANAGEMENT & SOFT DELETE (kế thừa Entity)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("ID Management & Soft Delete (kế thừa Entity)")
    class EntityBehavior {

        @Test
        @DisplayName("assignId() hợp lệ - isPersisted() = true")
        void assignIdHopLe() {
            Notification n = buildDefault();
            n.assignId(77);
            assertEquals(77, n.getId());
            assertTrue(n.isPersisted());
        }

        @Test
        @DisplayName("assignId() lần 2 - ném IllegalStateException")
        void assignIdLanHai() {
            Notification n = buildDefault();
            n.assignId(1);
            assertThrows(IllegalStateException.class, () -> n.assignId(2));
        }

        @Test
        @DisplayName("markAsDeleted() + restore() hoạt động")
        void softDeleteRestore() {
            Notification n = buildDefault();
            n.markAsDeleted();
            assertTrue(n.isDeleted());

            n.restore();
            assertFalse(n.isDeleted());
        }

        @Test
        @DisplayName("Soft delete không ảnh hưởng đến isRead state")
        void softDeleteKhongAnhHuongIsRead() {
            Notification n = buildDefault();
            n.markAsRead();
            n.markAsDeleted();

            assertTrue(n.isRead(), "isRead không bị ảnh hưởng bởi soft delete");
            assertTrue(n.isDeleted());
        }
    }

    // ═══════════════════════════════════════════════════
    // MULTIPLE NOTIFICATIONS - độc lập nhau
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("Nhiều Notification cho cùng user - hoạt động độc lập")
    void nhieuNotificationDocLap() {
        Notification n1 = Notification.builder()
                .userId(1).title("T1").message("M1").type(NotificationType.OUTBID).build();
        Notification n2 = Notification.builder()
                .userId(1).title("T2").message("M2").type(NotificationType.AUCTION_WON).build();
        Notification n3 = Notification.builder()
                .userId(1).title("T3").message("M3").type(NotificationType.SYSTEM).build();

        // Đọc n1 không ảnh hưởng n2, n3
        n1.markAsRead();

        assertTrue(n1.isRead());
        assertFalse(n2.isRead());
        assertFalse(n3.isRead());
    }

    @Test
    @DisplayName("Notification cho nhiều user khác nhau - userId chính xác")
    void notificationNhieuUser() {
        int[] userIds = {1, 2, 3, 100, 999};
        for (int uid : userIds) {
            Notification n = Notification.builder()
                    .userId(uid).title("T").message("M").type(NotificationType.SYSTEM).build();
            assertEquals(uid, n.getUserId(), "userId phải đúng cho user " + uid);
        }
    }

    // ═══════════════════════════════════════════════════
    // ALL NotificationType SCENARIOS
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Nghiệp vụ từng NotificationType")
    class NotificationTypeScenarios {

        @Test
        @DisplayName("OUTBID - bị vượt giá")
        void outbidScenario() {
            Notification n = Notification.builder()
                    .userId(10)
                    .title("Bạn đã bị vượt giá")
                    .message("Người dùng 'thanh_bid' đã đặt giá 2,000,000 VND cho iPhone 16")
                    .type(NotificationType.OUTBID)
                    .build();

            assertEquals(NotificationType.OUTBID, n.getType());
            assertFalse(n.isRead());
        }

        @Test
        @DisplayName("AUCTION_WON - thắng đấu giá")
        void auctionWonScenario() {
            Notification n = Notification.builder()
                    .userId(10)
                    .title("Chúc mừng! Bạn đã thắng")
                    .message("Bạn đã thắng phiên đấu giá sản phẩm Rolex Submariner với giá 50,000,000 VND")
                    .type(NotificationType.AUCTION_WON)
                    .build();

            assertEquals(NotificationType.AUCTION_WON, n.getType());
            n.markAsRead();
            assertTrue(n.isRead());
        }

        @Test
        @DisplayName("FAVORITE_STARTED - sản phẩm yêu thích lên sàn")
        void favoriteStartedScenario() {
            Notification n = Notification.builder()
                    .userId(5)
                    .title("Sản phẩm bạn quan tâm bắt đầu đấu giá")
                    .message("MacBook Pro M4 đã bắt đầu phiên đấu giá. Bắt đầu lúc 10:00")
                    .type(NotificationType.FAVORITE_STARTED)
                    .build();

            assertEquals(NotificationType.FAVORITE_STARTED, n.getType());
        }

        @Test
        @DisplayName("SYSTEM - thông báo hệ thống")
        void systemScenario() {
            Notification n = Notification.builder()
                    .userId(1)
                    .title("Bảo trì hệ thống")
                    .message("Hệ thống sẽ bảo trì từ 02:00 - 04:00 ngày 01/06/2026")
                    .type(NotificationType.SYSTEM)
                    .build();

            assertEquals(NotificationType.SYSTEM, n.getType());
        }
    }
}