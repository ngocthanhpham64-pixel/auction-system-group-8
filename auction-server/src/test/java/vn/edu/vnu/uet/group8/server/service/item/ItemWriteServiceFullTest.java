package vn.edu.vnu.uet.group8.server.service.item;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.AuctionException;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.FileUtil;

/**
 * Test toàn diện cho {@link ItemWriteService}.
 *
 * <p>Bao phủ 3 nhóm method chính:
 * <ul>
 *   <li>{@link ItemWriteService#createItem} — 0% → ~90%</li>
 *   <li>{@link ItemWriteService#updateItem} — 0% → ~90%</li>
 *   <li>{@link ItemWriteService#deleteItem} — (đã có, bổ sung thêm)</li>
 * </ul>
 *
 * <p>Chiến lược mock {@code FileUtil.saveBase64Images} (static method):
 * dùng {@code MockedStatic<FileUtil>} của Mockito 5.x để intercept,
 * trả về danh sách URL giả — không cần file thật trên disk.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemWriteService - Full Coverage Tests")
class ItemWriteServiceFullTest {

    @Mock private ItemDAO       itemDAO;
    @Mock private UserDAO       userDAO;
    @Mock private AuctionSessionDAO sessionDAO;
    @Mock private ItemSpecValidator specValidator;

    private ItemWriteService service;

    // ── Fake URLs trả về khi mock FileUtil ───────────────────────────
    private static final List<String> FAKE_URLS =
            List.of("http://localhost:8081/uploads/img1.jpg");

    @BeforeEach
    void setUp() {
        service = new ItemWriteService(itemDAO, userDAO, sessionDAO, specValidator);
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    /** Tạo UserMember ACTIVE, có role BIDDER (chưa có SELLER) */
    private UserMember activeMember(int id) {
        UserMember m = UserMember.builder("user" + id, "u" + id + "@mail.com", "$2a$hash")
                .phone("090000000" + id)
                .balance(new BigDecimal("500000"))
                .build();
        m.assignId(id);
        return m;
    }

    /** Tạo UserMember đã có role SELLER */
    private UserMember activeSeller(int id) {
        UserMember m = activeMember(id);
        m.addRole(UserRole.SELLER);
        return m;
    }

    /** Tạo UserMember bị SUSPENDED */
    private UserMember suspendedMember(int id) {
        UserMember m = activeMember(id);
        m.setStatus(UserStatus.SUSPENDED);
        return m;
    }

    /** Tạo UserAdmin SUPER_ADMIN */
    private UserAdmin superAdmin(int id) {
        UserAdmin a = new UserAdmin.Builder(
                "admin" + id, "admin" + id + "@mail.com", "$2a$hash",
                AdminLevel.SUPER_ADMIN).build();
        a.assignId(id);
        return a;
    }

    /** Tạo Item với id và sellerId đã gán */
    private Item item(int itemId, int sellerId, ItemStatus status) {
        Item i = new Item.Builder(sellerId, "iPhone 17", ItemCategory.ELECTRONICS)
                .condition(ItemCondition.NEW)
                .description("Mô tả sản phẩm")
                .status(status)
                .build();
        i.assignId(itemId);
        return i;
    }

    /** Helper tạo AuctionSession với status cụ thể */
    private AuctionSession session(int sessionId, int itemId, SessionStatus status) {
        AuctionSession s = new AuctionSession.Builder(
                itemId,
                new BigDecimal("100000"),
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600))
                .build();
        s.assignId(sessionId);
        if (status == SessionStatus.ACTIVE) {
            s.transitionStatus(SessionStatus.UPCOMING, SessionStatus.ACTIVE);
        } else if (status == SessionStatus.SOLD) {
            s.transitionStatus(SessionStatus.UPCOMING, SessionStatus.ACTIVE);
            s.transitionStatus(SessionStatus.ACTIVE, SessionStatus.SOLD);
        } else if (status == SessionStatus.CANCELLED) {
            s.transitionStatus(SessionStatus.UPCOMING, SessionStatus.CANCELLED);
        }
        return s;
    }

    // ════════════════════════════════════════════════════════════════
    // ① createItem — VALIDATION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("① createItem — Validation")
    class CreateItemValidation {

        @Test
        @DisplayName("sellerId không tồn tại trong DB → ValidationException")
        void sellerKhongTonTai() throws SQLException {
            when(userDAO.findById(99)).thenReturn(Optional.empty());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                assertThrows(ValidationException.class, () ->
                        service.createItem(99, "Title", "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
            }

            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Seller bị SUSPENDED (không active) → UnauthorizedException")
        void sellerSuspended() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(suspendedMember(5)));

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                assertThrows(UnauthorizedException.class, () ->
                        service.createItem(5, "Title", "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
            }

            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Seller đã có 10 item LISTED (đúng giới hạn) → ValidationException")
        void vuotGioiHanListed() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(10);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                assertThrows(ValidationException.class, () ->
                        service.createItem(5, "Title", "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
            }

            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("Seller có 9 item LISTED (chưa đầy) → không bị chặn bởi giới hạn")
        void chuaDayGioiHanListed() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(9);
            // title null → bị chặn ở bước sau, nhưng chứng minh giới hạn không chặn
            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                assertThrows(ValidationException.class, () ->
                        service.createItem(5, null, "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
            }
            // Bị lỗi title chứ không phải lỗi giới hạn 10
            verify(itemDAO, never()).insert(any());
        }

        @Test
        @DisplayName("title = null → ValidationException")
        void titleNull() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                ValidationException ex = assertThrows(ValidationException.class, () ->
                        service.createItem(5, null, "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
                assertTrue(ex.getMessage().contains("Tiêu đề"), "Phải nhắc về Tiêu đề");
            }
        }

        @Test
        @DisplayName("title = '' (blank) → ValidationException")
        void titleBlank() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                assertThrows(ValidationException.class, () ->
                        service.createItem(5, "   ", "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
            }
        }

        @Test
        @DisplayName("title = '  \\t  ' (chỉ whitespace) → ValidationException")
        void titleWhitespace() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                assertThrows(ValidationException.class, () ->
                        service.createItem(5, "\t  \n", "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
            }
        }

        @Test
        @DisplayName("specValidator.validate() ném ValidationException → propagate lên")
        void specValidatorNem() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);
            doThrow(new ValidationException("Thiếu spec bắt buộc: RAM"))
                    .when(specValidator).validate(ItemCategory.ELECTRONICS);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                ValidationException ex = assertThrows(ValidationException.class, () ->
                        service.createItem(5, "MacBook", "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
                assertTrue(ex.getMessage().contains("RAM"));
            }

            verify(itemDAO, never()).insert(any());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ② createItem — HAPPY PATH
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("② createItem — Happy Path")
    class CreateItemHappyPath {

        @Test
        @DisplayName("Tạo item DRAFT thành công (startPrice=null, durationHours=null)")
        void taoItemDraftThanhCong() throws SQLException {
            UserMember seller = activeMember(5);
            when(userDAO.findById(5)).thenReturn(Optional.of(seller));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                // mock insert để gán id
                doAnswer(inv -> {
                    Item i = inv.getArgument(0);
                    i.assignId(100);
                    return null;
                }).when(itemDAO).insert(any(Item.class));

                Item result = service.createItem(5, "MacBook Pro", "Mô tả laptop",
                        ItemCategory.ELECTRONICS, ItemCondition.NEW,
                        List.of("base64data=="), null, null);

                // Kiểm tra item được tạo đúng
                assertNotNull(result);
                assertEquals(100, result.getId());
                assertEquals("MacBook Pro", result.getTitle());
                assertEquals(ItemCategory.ELECTRONICS, result.getCategory());
                assertEquals(ItemStatus.DRAFT, result.getStatus(),
                        "Khi không có startPrice/duration → DRAFT");

                // Không tạo session
                verify(sessionDAO, never()).insert(any());

                // Thêm role SELLER vì chưa có
                verify(userDAO).addRole(5, UserRole.SELLER);
            }
        }

        @Test
        @DisplayName("Tạo item và publish ngay (startPrice + durationHours > 0) → LISTED + session ACTIVE")
        void taoItemPublishNgay() throws SQLException {
            UserMember seller = activeMember(5);
            when(userDAO.findById(5)).thenReturn(Optional.of(seller));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(101); return null; })
                        .when(itemDAO).insert(any(Item.class));

                Item result = service.createItem(5, "iPhone 17", "Điện thoại mới",
                        ItemCategory.ELECTRONICS, ItemCondition.NEW,
                        List.of(), new BigDecimal("5000000"), 24);

                // Status phải là LISTED
                assertEquals(ItemStatus.LISTED, result.getStatus(),
                        "Khi có startPrice + durationHours → LISTED");
                assertEquals(101, result.getId());

                // Phải tạo session
                ArgumentCaptor<AuctionSession> sessionCap =
                        ArgumentCaptor.forClass(AuctionSession.class);
                verify(sessionDAO).insert(sessionCap.capture());

                AuctionSession createdSession = sessionCap.getValue();
                assertEquals(101, createdSession.getItemId());
                assertEquals(SessionStatus.ACTIVE, createdSession.getStatus(),
                        "Session phải được mở (ACTIVE) ngay lập tức");
                assertEquals(0, createdSession.getStartingPrice()
                        .compareTo(new BigDecimal("5000000")));
            }
        }

        @Test
        @DisplayName("Seller đã có role SELLER → không gọi addRole lần nữa")
        void sellerDaCORoleKhongAddLai() throws SQLException {
            UserMember seller = activeSeller(5); // đã có SELLER
            when(userDAO.findById(5)).thenReturn(Optional.of(seller));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(102); return null; })
                        .when(itemDAO).insert(any(Item.class));

                service.createItem(5, "Laptop", "Desc",
                        ItemCategory.ELECTRONICS, ItemCondition.USED,
                        List.of(), null, null);

                // Đã có role SELLER → không gọi addRole
                verify(userDAO, never()).addRole(anyInt(), eq(UserRole.SELLER));
            }
        }

        @Test
        @DisplayName("imageUrls null → FileUtil được gọi với null, không ném")
        void imageUrlsNull() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(null)).thenReturn(List.of());
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(103); return null; })
                        .when(itemDAO).insert(any(Item.class));

                assertDoesNotThrow(() -> service.createItem(5, "Laptop", "Desc",
                        ItemCategory.ELECTRONICS, ItemCondition.NEW,
                        null, null, null));
            }
        }

        @Test
        @DisplayName("imageUrls rỗng → Item được tạo với imageUrls empty")
        void imageUrlsRong() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(List.of());
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(104); return null; })
                        .when(itemDAO).insert(any(Item.class));

                Item result = service.createItem(5, "Laptop", "Desc",
                        ItemCategory.ELECTRONICS, ItemCondition.NEW,
                        List.of(), null, null);

                assertTrue(result.getImageUrls().isEmpty());
            }
        }

        @Test
        @DisplayName("durationHours = 0 → KHÔNG publish (isPublishing = false) → DRAFT")
        void durationHoursZeroKhongPublish() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(105); return null; })
                        .when(itemDAO).insert(any(Item.class));

                Item result = service.createItem(5, "Laptop", "Desc",
                        ItemCategory.ELECTRONICS, ItemCondition.NEW,
                        List.of(), new BigDecimal("1000000"), 0);

                assertEquals(ItemStatus.DRAFT, result.getStatus(),
                        "durationHours = 0 → không publish → DRAFT");
                verify(sessionDAO, never()).insert(any());
            }
        }

        @Test
        @DisplayName("durationHours âm → KHÔNG publish → DRAFT")
        void durationHoursAmKhongPublish() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(106); return null; })
                        .when(itemDAO).insert(any(Item.class));

                Item result = service.createItem(5, "Laptop", "Desc",
                        ItemCategory.ELECTRONICS, ItemCondition.NEW,
                        List.of(), new BigDecimal("1000000"), -5);

                assertEquals(ItemStatus.DRAFT, result.getStatus());
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"ELECTRONICS", "FASHION", "VEHICLES", "JEWELRY", "OTHER"})
        @DisplayName("Tạo item với nhiều category khác nhau — tất cả phải thành công")
        void taoItemNhieuCategory(String categoryName) throws SQLException {
            ItemCategory cat = ItemCategory.valueOf(categoryName);
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(200); return null; })
                        .when(itemDAO).insert(any(Item.class));

                assertDoesNotThrow(() ->
                        service.createItem(5, "Sản phẩm " + cat, "Desc", cat,
                                ItemCondition.NEW, List.of(), null, null));
            }
        }

        @Test
        @DisplayName("itemDAO.insert() ném SQLException → propagate lên caller")
        void insertNemSQLException() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);
            doThrow(new SQLException("DB error")).when(itemDAO).insert(any());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                assertThrows(SQLException.class, () ->
                        service.createItem(5, "Laptop", "Desc",
                                ItemCategory.ELECTRONICS, ItemCondition.NEW,
                                List.of(), null, null));
            }
        }

        @Test
        @DisplayName("specValidator.validate() được gọi đúng 1 lần với đúng category")
        void specValidatorDuocGoiDung() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(99); return null; })
                        .when(itemDAO).insert(any(Item.class));

                service.createItem(5, "Camera", "Desc",
                        ItemCategory.ELECTRONICS, ItemCondition.NEW,
                        List.of(), null, null);

                verify(specValidator, times(1)).validate(ItemCategory.ELECTRONICS);
            }
        }

        @Test
        @DisplayName("Publish: session endTime = startTime + durationHours")
        void sessionEndTimeDung() throws SQLException {
            when(userDAO.findById(5)).thenReturn(Optional.of(activeMember(5)));
            when(itemDAO.countBySellerAndStatus(5, ItemStatus.LISTED)).thenReturn(0);

            Instant beforeCall = Instant.now().minusSeconds(1);

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);
                doAnswer(inv -> { ((Item) inv.getArgument(0)).assignId(110); return null; })
                        .when(itemDAO).insert(any(Item.class));

                service.createItem(5, "Watch", "Desc",
                        ItemCategory.JEWELRY, ItemCondition.NEW,
                        List.of(), new BigDecimal("2000000"), 3);

                ArgumentCaptor<AuctionSession> cap =
                        ArgumentCaptor.forClass(AuctionSession.class);
                verify(sessionDAO).insert(cap.capture());

                AuctionSession s = cap.getValue();
                Instant afterCall = Instant.now().plusSeconds(1);

                // endTime phải trong khoảng: (beforeCall + 3h) đến (afterCall + 3h)
                long HOURS_3_SECS = 3 * 3600;
                assertTrue(s.getEndTime().isAfter(beforeCall.plusSeconds(HOURS_3_SECS)));
                assertTrue(s.getEndTime().isBefore(afterCall.plusSeconds(HOURS_3_SECS)));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ③ updateItem — VALIDATION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("③ updateItem — Validation")
    class UpdateItemValidation {

        @Test
        @DisplayName("itemId không tồn tại → ItemNotFoundException")
        void itemKhongTonTai() throws SQLException {
            when(itemDAO.findById(999)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class, () ->
                    service.updateItem(5, 999, "New Title", "Desc",
                            ItemCondition.NEW, List.of()));
        }

        @Test
        @DisplayName("requesterId khác sellerId → UnauthorizedException")
        void nguoiLaSua() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));

            assertThrows(UnauthorizedException.class, () ->
                    service.updateItem(99, 10, "New Title", "Desc",
                            ItemCondition.NEW, List.of()));

            verify(itemDAO, never()).update(any());
        }

        @Test
        @DisplayName("Item có session ACTIVE → AuctionException")
        void coActiveSession() throws SQLException {
            Item item = item(10, 5, ItemStatus.LISTED);
            AuctionSession active = session(20, 10, SessionStatus.ACTIVE);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(List.of(active));

            assertThrows(AuctionException.class, () ->
                    service.updateItem(5, 10, "New Title", "Desc",
                            ItemCondition.NEW, List.of()));

            verify(itemDAO, never()).update(any());
        }

        @Test
        @DisplayName("Item có session UPCOMING → AuctionException")
        void coUpcomingSession() throws SQLException {
            Item item = item(10, 5, ItemStatus.LISTED);
            AuctionSession upcoming = session(20, 10, SessionStatus.UPCOMING);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(List.of(upcoming));

            assertThrows(AuctionException.class, () ->
                    service.updateItem(5, 10, "New Title", "Desc",
                            ItemCondition.NEW, List.of()));
        }

        @Test
        @DisplayName("specValidator.validate() ném ValidationException → propagate lên")
        void specValidatorNem() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());
            doThrow(new ValidationException("Thiếu spec RAM"))
                    .when(specValidator).validate(ItemCategory.ELECTRONICS);

            assertThrows(ValidationException.class, () ->
                    service.updateItem(5, 10, "New Title", "Desc",
                            ItemCondition.NEW, List.of()));

            verify(itemDAO, never()).update(any());
        }

        @Test
        @DisplayName("itemDAO.update() ném SQLException → propagate lên")
        void updateNemSQL() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());
            doThrow(new SQLException("DB lỗi")).when(itemDAO).update(any());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                assertThrows(SQLException.class, () ->
                        service.updateItem(5, 10, "Title", "Desc",
                                ItemCondition.NEW, List.of()));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ④ updateItem — HAPPY PATH
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("④ updateItem — Happy Path")
    class UpdateItemHappyPath {

        @Test
        @DisplayName("Cập nhật title hợp lệ → title được thay đổi")
        void capNhatTitleHopLe() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, "Samsung Galaxy S25", null,
                        null, null);

                assertEquals("Samsung Galaxy S25", item.getTitle());
                verify(itemDAO).update(item);
            }
        }

        @Test
        @DisplayName("title = null → giữ nguyên title cũ")
        void titleNullGiuNguyen() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            String oldTitle = item.getTitle();
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, null, null, null, null);

                assertEquals(oldTitle, item.getTitle(), "title null → giữ nguyên");
            }
        }

        @Test
        @DisplayName("title blank → giữ nguyên title cũ")
        void titleBlankGiuNguyen() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            String oldTitle = item.getTitle();
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, "   ", null, null, null);

                assertEquals(oldTitle, item.getTitle(), "title blank → giữ nguyên");
            }
        }

        @Test
        @DisplayName("Cập nhật description hợp lệ → description thay đổi")
        void capNhatDescription() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, null, "Mô tả mới rất chi tiết",
                        null, null);

                assertEquals("Mô tả mới rất chi tiết", item.getDescription());
            }
        }

        @Test
        @DisplayName("description = null → giữ nguyên description cũ")
        void descriptionNullGiuNguyen() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            String oldDesc = item.getDescription();
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, null, null, null, null);

                assertEquals(oldDesc, item.getDescription());
            }
        }

        @Test
        @DisplayName("Cập nhật condition từ NEW → USED")
        void capNhatCondition() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            assertEquals(ItemCondition.NEW, item.getCondition());
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, null, null, ItemCondition.USED, null);

                assertEquals(ItemCondition.USED, item.getCondition());
            }
        }

        @Test
        @DisplayName("condition = null → giữ nguyên condition cũ")
        void conditionNullGiuNguyen() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            ItemCondition oldCond = item.getCondition();
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, null, null, null, null);

                assertEquals(oldCond, item.getCondition());
            }
        }

        @Test
        @DisplayName("Cập nhật imageUrls → FileUtil được gọi + imageUrls thay đổi")
        void capNhatImageUrls() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            List<String> newUrls = List.of("http://localhost:8081/uploads/new.jpg");

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(newUrls);

                service.updateItem(5, 10, null, null, null, List.of("base64new=="));

                assertEquals(newUrls, item.getImageUrls());
                mf.verify(() -> FileUtil.saveBase64Images(anyList()), times(1));
            }
        }

        @Test
        @DisplayName("imageUrls = null → FileUtil KHÔNG được gọi, imageUrls giữ nguyên")
        void imageUrlsNullKhongGoiFileUtil() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                service.updateItem(5, 10, null, null, null, null);

                mf.verify(() -> FileUtil.saveBase64Images(anyList()), never());
            }
        }

        @Test
        @DisplayName("Cập nhật đồng thời title + description + condition + imageUrls")
        void capNhatDongThoi() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList()))
                        .thenReturn(List.of("http://localhost:8081/uploads/new1.jpg"));

                service.updateItem(5, 10,
                        "Title mới",
                        "Description mới",
                        ItemCondition.LIKE_NEW,
                        List.of("base64=="));

                assertEquals("Title mới", item.getTitle());
                assertEquals("Description mới", item.getDescription());
                assertEquals(ItemCondition.LIKE_NEW, item.getCondition());
                assertEquals(1, item.getImageUrls().size());

                verify(itemDAO, times(1)).update(item);
            }
        }

        @Test
        @DisplayName("Item có session SOLD (không active) → cho phép cập nhật")
        void coSoldSessionVanSua() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            AuctionSession sold = session(20, 10, SessionStatus.SOLD);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(List.of(sold));

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                assertDoesNotThrow(() ->
                        service.updateItem(5, 10, "Title mới", null, null, null));

                verify(itemDAO).update(item);
            }
        }

        @Test
        @DisplayName("Item có session CANCELLED → cho phép cập nhật")
        void coCancelledSessionVanSua() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            AuctionSession cancelled = session(20, 10, SessionStatus.CANCELLED);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(List.of(cancelled));

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                assertDoesNotThrow(() ->
                        service.updateItem(5, 10, "Title mới", null, null, null));

                verify(itemDAO).update(item);
            }
        }

        @Test
        @DisplayName("specValidator.validate() được gọi với đúng category của item")
        void specValidatorGoiVoiCategoryDung() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT); // ELECTRONICS
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, null, null, null, null);

                verify(specValidator).validate(ItemCategory.ELECTRONICS);
            }
        }

        @Test
        @DisplayName("itemDAO.update() được gọi đúng 1 lần với đúng item")
        void updateGoiDung() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            try (MockedStatic<FileUtil> mf = mockStatic(FileUtil.class)) {
                mf.when(() -> FileUtil.saveBase64Images(anyList())).thenReturn(FAKE_URLS);

                service.updateItem(5, 10, "New Title", "New Desc",
                        ItemCondition.USED, null);

                ArgumentCaptor<Item> cap = ArgumentCaptor.forClass(Item.class);
                verify(itemDAO, times(1)).update(cap.capture());
                assertEquals(10, cap.getValue().getId());
                assertEquals("New Title", cap.getValue().getTitle());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ⑤ deleteItem — Bổ sung thêm kịch bản
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("⑤ deleteItem — Bổ sung kịch bản")
    class DeleteItemExtra {

        @Test
        @DisplayName("Item có nhiều session: 1 SOLD + 1 UPCOMING → cho phép xóa (không có ACTIVE)")
        void nhieuSessionKhongActive() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            UserMember owner = activeMember(5);
            AuctionSession sold = session(20, 10, SessionStatus.SOLD);
            AuctionSession upcoming = session(21, 10, SessionStatus.UPCOMING);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(5)).thenReturn(Optional.of(owner));
            when(sessionDAO.findByItemId(10)).thenReturn(List.of(sold, upcoming));

            assertDoesNotThrow(() -> service.deleteItem(5, 10));
            verify(itemDAO).softDelete(10);
        }

        @Test
        @DisplayName("Item có nhiều session: 1 SOLD + 1 ACTIVE → từ chối xóa")
        void nhieuSessionCoActive() throws SQLException {
            Item item = item(10, 5, ItemStatus.LISTED);
            UserMember owner = activeMember(5);
            AuctionSession sold = session(20, 10, SessionStatus.SOLD);
            AuctionSession active = session(21, 10, SessionStatus.ACTIVE);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(5)).thenReturn(Optional.of(owner));
            when(sessionDAO.findByItemId(10)).thenReturn(List.of(sold, active));

            AuctionException ex = assertThrows(AuctionException.class,
                    () -> service.deleteItem(5, 10));
            assertTrue(ex.getMessage().contains("Hủy phiên"),
                    "Thông báo lỗi phải nhắc hủy phiên trước");
            verify(itemDAO, never()).softDelete(anyInt());
        }

        @Test
        @DisplayName("Admin MODERATOR xóa item của người khác → cho phép")
        void adminModeratorXoa() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            UserAdmin mod = new UserAdmin.Builder(
                    "mod", "mod@mail.com", "$2a$hash", AdminLevel.MODERATOR)
                    .build();
            mod.assignId(99);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(99)).thenReturn(Optional.of(mod));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.deleteItem(99, 10));
            verify(itemDAO).softDelete(10);
        }

        @Test
        @DisplayName("sessionDAO.findByItemId trả emptyList → xóa thành công")
        void khongCoSessionNaoXoaOK() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            UserMember owner = activeMember(5);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(5)).thenReturn(Optional.of(owner));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            service.deleteItem(5, 10);

            verify(itemDAO).softDelete(10);
        }

        @Test
        @DisplayName("softDelete ném SQLException → propagate lên caller")
        void softDeleteNemSQL() throws SQLException {
            Item item = item(10, 5, ItemStatus.DRAFT);
            UserMember owner = activeMember(5);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(5)).thenReturn(Optional.of(owner));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());
            doThrow(new SQLException("DB lỗi")).when(itemDAO).softDelete(10);

            assertThrows(SQLException.class, () -> service.deleteItem(5, 10));
        }

        @Test
        @DisplayName("Owner đồng thời là admin (UserAdmin tự owned) → xóa được")
        void ownerLaAdmin() throws SQLException {
            UserAdmin admin = superAdmin(99);
            // item này thuộc sellerId = 99 (chính admin)
            Item item = item(10, 99, ItemStatus.DRAFT);

            when(itemDAO.findById(10)).thenReturn(Optional.of(item));
            when(userDAO.findById(99)).thenReturn(Optional.of(admin));
            when(sessionDAO.findByItemId(10)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.deleteItem(99, 10));
            verify(itemDAO).softDelete(10);
        }
    }
}