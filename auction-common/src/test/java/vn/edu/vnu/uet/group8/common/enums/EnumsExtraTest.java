package vn.edu.vnu.uet.group8.common.enums;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test cho 7 enum chưa được cover trong EnumsTest gốc.
 */
class EnumsExtraTest {

    @Nested
    @DisplayName("AdminLevel")
    class AdminLevelTest {
        @Test
        @DisplayName("Đủ 2 giá trị: MODERATOR, SUPER_ADMIN")
        void duGiaTri() {
            assertEquals(2, AdminLevel.values().length);
            assertNotNull(AdminLevel.valueOf("MODERATOR"));
            assertNotNull(AdminLevel.valueOf("SUPER_ADMIN"));
        }

        @Test
        @DisplayName("MODERATOR != SUPER_ADMIN")
        void khacNhau() {
            assertNotEquals(AdminLevel.MODERATOR, AdminLevel.SUPER_ADMIN);
        }
    }

    @Nested
    @DisplayName("AuctionStatus")
    class AuctionStatusTest {
        @Test
        @DisplayName("Đủ 5 giá trị: OPEN, RUNNING, FINISHED, PAID, CANCELED")
        void duGiaTri() {
            assertEquals(5, AuctionStatus.values().length);
            assertNotNull(AuctionStatus.valueOf("OPEN"));
            assertNotNull(AuctionStatus.valueOf("RUNNING"));
            assertNotNull(AuctionStatus.valueOf("FINISHED"));
            assertNotNull(AuctionStatus.valueOf("PAID"));
            assertNotNull(AuctionStatus.valueOf("CANCELED"));
        }
    }

    @Nested
    @DisplayName("EventType")
    class EventTypeTest {
        @Test
        @DisplayName("Đủ 3 giá trị: PRICE_UPDATE, AUCTION_ENDED, NOTIFICATION")
        void duGiaTri() {
            assertEquals(3, EventType.values().length);
            assertNotNull(EventType.valueOf("PRICE_UPDATE"));
            assertNotNull(EventType.valueOf("AUCTION_ENDED"));
            assertNotNull(EventType.valueOf("NOTIFICATION"));
        }
    }

    @Nested
    @DisplayName("ItemCondition")
    class ItemConditionTest {

        @Test
        @DisplayName("Đủ 6 giá trị + tất cả có label")
        void duGiaTri() {
            assertEquals(6, ItemCondition.values().length);
            for (ItemCondition c : ItemCondition.values()) {
                assertNotNull(c.getLabel(), c.name() + " phải có label");
                assertFalse(c.getLabel().isBlank());
            }
        }

        @Test
        @DisplayName("NEW.getLabel() = 'Hàng mới'")
        void newLabel() {
            assertEquals("Mới 100%", ItemCondition.NEW.getLabel());
        }

        @Test
        @DisplayName("USED.getLabel() = 'Đã qua sử dụng'")
        void usedLabel() {
            assertEquals("Đã qua sử dụng", ItemCondition.USED.getLabel());
        }
    }

    @Nested
    @DisplayName("PaymentMethod")
    class PaymentMethodTest {

        @Test
        @DisplayName("Đủ 5 giá trị, có code + description")
        void duGiaTri() {
            assertEquals(5, PaymentMethod.values().length);
            for (PaymentMethod p : PaymentMethod.values()) {
                assertNotNull(p.getCode());
                assertNotNull(p.getDescription());
                assertFalse(p.getCode().isBlank());
            }
        }

        @Test
        @DisplayName("COD code = CASH_ON_DELIVERY")
        void codCode() {
            assertEquals("CASH_ON_DELIVERY", PaymentMethod.COD.getCode());
        }

        @Test
        @DisplayName("fromCode() - chuyển code String → enum")
        void fromCodeHopLe() {
            assertEquals(PaymentMethod.COD, PaymentMethod.fromCode("CASH_ON_DELIVERY"));
            assertEquals(PaymentMethod.E_WALLET, PaymentMethod.fromCode("E_WALLET"));
        }

        @Test
        @DisplayName("fromCode() không phân biệt hoa thường")
        void fromCodeIgnoreCase() {
            assertEquals(PaymentMethod.BANK_TRANSFER,
                    PaymentMethod.fromCode("bank_transfer"));
            assertEquals(PaymentMethod.BANK_TRANSFER,
                    PaymentMethod.fromCode("Bank_Transfer"));
        }

        @Test
        @DisplayName("fromCode() với code không hợp lệ → ném")
        void fromCodeKhongHopLe() {
            assertThrows(IllegalArgumentException.class,
                    () -> PaymentMethod.fromCode("INVALID_CODE"));
        }
    }

    @Nested
    @DisplayName("TransactionStatus")
    class TransactionStatusTest {

        @Test
        @DisplayName("Đủ 4 giá trị, mỗi cái có label")
        void duGiaTri() {
            assertEquals(4, TransactionStatus.values().length);
            for (TransactionStatus s : TransactionStatus.values()) {
                assertNotNull(s.getLabel());
            }
        }

        @Test
        @DisplayName("SUCCESS.getLabel() = 'Thành công'")
        void successLabel() {
            assertEquals("Thành công", TransactionStatus.SUCCESS.getLabel());
        }
    }

    @Nested
    @DisplayName("TransactionType")
    class TransactionTypeTest {

        @Test
        @DisplayName("Đủ 8 giá trị, có description")
        void duGiaTri() {
            assertEquals(8, TransactionType.values().length);
            for (TransactionType t : TransactionType.values()) {
                assertNotNull(t.getDescription());
                assertFalse(t.getDescription().isBlank());
            }
        }

        @Test
        @DisplayName("DEPOSIT.getDescription() = 'Nạp tiền vào ví'")
        void depositDesc() {
            assertEquals("Nạp tiền vào ví", TransactionType.DEPOSIT.getDescription());
        }
    }
}