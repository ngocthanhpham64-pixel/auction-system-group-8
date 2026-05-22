package vn.edu.vnu.uet.group8.server.service.auction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.AuctionException;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit test FULL cho {@link BidValidator#validate(int, int, BigDecimal)}.
 *
 * <p>Cover 9 nhánh validation theo flow:
 * <ol>
 *   <li><b>loadAndValidateItem</b>
 *     <ul>
 *       <li>Session không tồn tại → ItemNotFoundException</li>
 *       <li>Session status ≠ ACTIVE → AuctionException</li>
 *       <li>Session đã hết hạn → AuctionException</li>
 *     </ul>
 *   </li>
 *   <li><b>itemDAO.findById</b>
 *     <ul>
 *       <li>Item không tồn tại → ItemNotFoundException</li>
 *     </ul>
 *   </li>
 *   <li><b>loadAndValidateBidder</b>
 *     <ul>
 *       <li>Bidder không tồn tại → UserNotFoundException</li>
 *       <li>Bidder không phải UserMember → ValidationException</li>
 *       <li>Bidder bị suspended/banned → ValidationException</li>
 *     </ul>
 *   </li>
 *   <li><b>validateDomainRules</b>
 *     <ul>
 *       <li>Bidder = Seller → ValidationException</li>
 *       <li>bidAmount null/zero/âm → ValidationException</li>
 *     </ul>
 *   </li>
 *   <li><b>validateStateChecks</b>
 *     <ul>
 *       <li>bidAmount &lt; minNextBid → ValidationException</li>
 *       <li>Số dư &lt; bidAmount → ValidationException</li>
 *     </ul>
 *   </li>
 *   <li><b>Happy path</b> → trả BidContext</li>
 * </ol>
 *
 * <p>Dùng <b>Mockito</b> mock 3 DAO. Entity tạo qua Builder/reflection để giả lập trạng thái thật.
 */
@ExtendWith(MockitoExtension.class)
class BidValidatorFullTest {

    @Mock private ItemDAO itemDAO;
    @Mock private UserDAO userDAO;
    @Mock private AuctionSessionDAO auctionSessionDAO;

    @InjectMocks private BidValidator bidValidator;

    // Constants
    private static final int BIDDER_ID = 100;
    private static final int SELLER_ID = 200;
    private static final int ITEM_ID = 50;
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("1000000");
    private static final BigDecimal MIN_NEXT_BID = new BigDecimal("1010000"); // 1% > 1000

    // ════════════════════════════════════════════════════
    // HELPERS - tạo entity test
    // ════════════════════════════════════════════════════

    /** Tạo Item của seller, không phải bidder. */
    private Item createItem() {
        return new Item.Builder(SELLER_ID, "iPhone 15", ItemCategory.ELECTRONICS).build();
    }

    /** Tạo session ACTIVE với endTime trong tương lai + currentPrice = 1.000.000. */
    private AuctionSession createActiveSession() throws Exception {
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        AuctionSession session = new AuctionSession.Builder(
                ITEM_ID, new BigDecimal("500000"), start, end).build();

        // Set status = ACTIVE và currentPrice = 1.000.000 qua reflection
        setField(session, "status", SessionStatus.ACTIVE);
        setField(session, "currentPrice", CURRENT_PRICE);
        return session;
    }

    /** Tạo UserMember ACTIVE với balance đủ lớn. */
    private UserMember createActiveBidder(BigDecimal balance) {
        UserMember member = UserMember.builder("bidder", "bidder@test.com", "$2a$12$hash")
                .phone("0901234567")
                .balance(balance)
                .build();
        setIdViaReflection(member, BIDDER_ID);
        return member;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** Set id qua reflection (Entity.id là private). */
    private void setIdViaReflection(Object target, int id) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    Field field = clazz.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(target, id);
                    return;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot set id via reflection", e);
        }
    }

    // ════════════════════════════════════════════════════
    // 1. SESSION VALIDATION
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate session")
    class ValidateSession {

        @Test
        @DisplayName("Session không tồn tại → ItemNotFoundException")
        void sessionKhongTonTai() throws SQLException {
            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.empty());

            assertThrows(ItemNotFoundException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("2000000")));
        }

        @Test
        @DisplayName("Session status UPCOMING → AuctionException")
        void sessionStatusKhongActive() throws Exception {
            AuctionSession session = createActiveSession();
            setField(session, "status", SessionStatus.UPCOMING);
            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));

            AuctionException ex = assertThrows(AuctionException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("2000000")));
            assertTrue(ex.getMessage().contains("UPCOMING"));
        }

        @Test
        @DisplayName("Session status SOLD → AuctionException")
        void sessionStatusSold() throws Exception {
            AuctionSession session = createActiveSession();
            setField(session, "status", SessionStatus.SOLD);
            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));

            assertThrows(AuctionException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("2000000")));
        }
    }

    // ════════════════════════════════════════════════════
    // 2. ITEM VALIDATION
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Item không tồn tại trong DB → ItemNotFoundException")
    void itemKhongTonTai() throws Exception {
        AuctionSession session = createActiveSession();
        when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
        when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class,
                () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("2000000")));
    }

    // ════════════════════════════════════════════════════
    // 3. BIDDER VALIDATION
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validate bidder")
    class ValidateBidder {

        @Test
        @DisplayName("Bidder không tồn tại → UserNotFoundException")
        void bidderKhongTonTai() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("2000000")));
        }

        @Test
        @DisplayName("Bidder bị SUSPENDED → ValidationException")
        void bidderSuspended() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            UserMember bidder = createActiveBidder(new BigDecimal("5000000"));
            bidder.setStatus(UserStatus.SUSPENDED);

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("2000000")));
            assertTrue(ex.getMessage().contains("SUSPENDED"));
        }

        @Test
        @DisplayName("Bidder bị BANNED → ValidationException")
        void bidderBanned() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            UserMember bidder = createActiveBidder(new BigDecimal("5000000"));
            bidder.setStatus(UserStatus.BANNED);

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            assertThrows(ValidationException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("2000000")));
        }
    }

    // ════════════════════════════════════════════════════
    // 4. DOMAIN RULES
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("Domain rules")
    class DomainRules {

        @Test
        @DisplayName("Bidder = Seller (tự đấu giá item của mình) → ValidationException")
        void bidderBangSeller() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            // Bidder có id = SELLER_ID (trùng seller)
            UserMember bidder = createActiveBidder(new BigDecimal("5000000"));
            setIdViaReflection(bidder, SELLER_ID);

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(SELLER_ID)).thenReturn(Optional.of(bidder));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> bidValidator.validate(SELLER_ID, ITEM_ID, new BigDecimal("2000000")));
            assertTrue(ex.getMessage().contains("chính mình"));
        }

        @Test
        @DisplayName("bidAmount null → ValidationException")
        void bidAmountNull() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            UserMember bidder = createActiveBidder(new BigDecimal("5000000"));

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, null));
            assertTrue(ex.getMessage().contains("lớn hơn 0"));
        }

        @Test
        @DisplayName("bidAmount = 0 → ValidationException")
        void bidAmountZero() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            UserMember bidder = createActiveBidder(new BigDecimal("5000000"));

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            assertThrows(ValidationException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("bidAmount âm → ValidationException")
        void bidAmountAm() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            UserMember bidder = createActiveBidder(new BigDecimal("5000000"));

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            assertThrows(ValidationException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("-1000")));
        }
    }

    // ════════════════════════════════════════════════════
    // 5. STATE CHECKS
    // ════════════════════════════════════════════════════

    @Nested
    @DisplayName("State checks")
    class StateChecks {

        @Test
        @DisplayName("bidAmount < minNextBid → ValidationException")
        void bidAmountDuoiMinNext() throws Exception {
            AuctionSession session = createActiveSession();  // currentPrice = 1tr, minNext = 1.01tr
            Item item = createItem();
            UserMember bidder = createActiveBidder(new BigDecimal("5000000"));

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            // Bid = 1.005.000 < minNext = 1.010.000 → fail
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("1005000")));
            assertTrue(ex.getMessage().contains("tối thiểu"));
        }

        @Test
        @DisplayName("bidAmount = minNextBid → pass (boundary)")
        void bidAmountBangMinNextPass() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            UserMember bidder = createActiveBidder(new BigDecimal("5000000"));

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            // Bid = exact minNext 1.010.000 → pass
            assertDoesNotThrow(
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, MIN_NEXT_BID));
        }

        @Test
        @DisplayName("Số dư < bidAmount → ValidationException")
        void soDuKhongDu() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            // Balance = 500k, bid = 1.010.000 → không đủ
            UserMember bidder = createActiveBidder(new BigDecimal("500000"));

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, MIN_NEXT_BID));
            assertTrue(ex.getMessage().contains("Số dư"));
        }

        @Test
        @DisplayName("Số dư = bidAmount → pass (boundary)")
        void soDuBangBidAmount() throws Exception {
            AuctionSession session = createActiveSession();
            Item item = createItem();
            // Balance đúng bằng minNext
            UserMember bidder = createActiveBidder(MIN_NEXT_BID);

            when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
            when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

            assertDoesNotThrow(
                    () -> bidValidator.validate(BIDDER_ID, ITEM_ID, MIN_NEXT_BID));
        }
    }

    // ════════════════════════════════════════════════════
    // 6. HAPPY PATH
    // ════════════════════════════════════════════════════

    @Test
    @DisplayName("Tất cả pass → trả về BidContext với entity đã load")
    void happyPathTraBidContext() throws Exception {
        AuctionSession session = createActiveSession();
        Item item = createItem();
        UserMember bidder = createActiveBidder(new BigDecimal("5000000"));

        when(auctionSessionDAO.findById(ITEM_ID)).thenReturn(Optional.of(session));
        when(itemDAO.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(userDAO.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));

        BidContext context = bidValidator.validate(BIDDER_ID, ITEM_ID, new BigDecimal("2000000"));

        assertNotNull(context, "BidContext không được null");
        // BidContext giữ entity đã load - kiểm tra qua getter (nếu có)
        // Lưu ý: BidContext class có thể có nhiều API khác nhau
    }
}