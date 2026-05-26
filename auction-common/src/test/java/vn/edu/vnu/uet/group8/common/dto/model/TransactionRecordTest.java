package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.vnu.uet.group8.common.enums.TransactionStatus;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

/**
 * Test toàn diện cho {@link TransactionRecord} DTO.
 *
 * <p>Phạm vi kiểm thử:
 * <ul>
 *   <li><b>Constructor đầy đủ</b>: map đúng tất cả field</li>
 *   <li><b>No-arg constructor</b>: dùng cho Gson deserialization</li>
 *   <li><b>Getters (accessor methods)</b>: trả về đúng kiểu và giá trị</li>
 *   <li><b>sessionId nullable</b>: null khi giao dịch không liên quan phiên đấu giá</li>
 *   <li><b>Mọi TransactionType</b>: DEPOSIT, WITHDRAWAL, BID_PAYMENT, REFUND...</li>
 *   <li><b>Mọi TransactionStatus</b>: PENDING, COMPLETED, FAILED...</li>
 *   <li><b>Hai instance độc lập</b>: không chia sẻ state</li>
 * </ul>
 *
 * <p>Lưu ý: {@code TransactionRecord} sử dụng accessor methods (không phải getX),
 * ví dụ: {@code amount()} thay vì {@code getAmount()}.
 */
@DisplayName("TransactionRecord - Unit Tests")
class TransactionRecordTest {

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════

    private TransactionRecord buildDefault() {
        return new TransactionRecord(
                "TXN-001",
                42,
                new BigDecimal("500000"),
                TransactionType.DEPOSIT,
                Instant.parse("2026-01-15T10:00:00Z"),
                TransactionStatus.SUCCESS,
                "Nạp tiền vào tài khoản",
                null
        );
    }

    private TransactionRecord buildWithSession() {
        return new TransactionRecord(
                "TXN-002",
                42,
                new BigDecimal("2000000"),
                TransactionType.BID_PAYMENT,
                Instant.parse("2026-01-16T14:30:00Z"),
                TransactionStatus.SUCCESS,
                "Thanh toán phiên đấu giá #5",
                5
        );
    }

    // ═══════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("No-arg constructor (Gson) - tạo được, tất cả field là null/default")
        void noArgConstructor() {
            TransactionRecord tr = new TransactionRecord();
            assertNotNull(tr, "No-arg constructor không được ném exception");
            // Tất cả field là null (reference types) hoặc 0 (primitive int)
            assertNull(tr.transactionId());
            assertEquals(0, tr.userId());
            assertNull(tr.amount());
            assertNull(tr.transactionType());
            assertNull(tr.createdAt());
            assertNull(tr.status());
            assertNull(tr.description());
            assertNull(tr.sessionId());
        }

        @Test
        @DisplayName("Constructor đầy đủ - map đúng tất cả field")
        void constructorDayDu() {
            Instant now = Instant.now();
            TransactionRecord tr = new TransactionRecord(
                    "TXN-ABC",
                    99,
                    new BigDecimal("1500000"),
                    TransactionType.DEPOSIT,
                    now,
                    TransactionStatus.PENDING,
                    "Mô tả giao dịch",
                    10
            );

            assertEquals("TXN-ABC", tr.transactionId());
            assertEquals(99, tr.userId());
            assertEquals(0, tr.amount().compareTo(new BigDecimal("1500000")));
            assertEquals(TransactionType.DEPOSIT, tr.transactionType());
            assertEquals(now, tr.createdAt());
            assertEquals(TransactionStatus.PENDING, tr.status());
            assertEquals("Mô tả giao dịch", tr.description());
            assertEquals(10, tr.sessionId());
        }

        @Test
        @DisplayName("Constructor với sessionId = null - giao dịch không liên quan phiên")
        void constructorSessionIdNull() {
            TransactionRecord tr = buildDefault();
            assertNull(tr.sessionId(), "sessionId = null khi giao dịch không có phiên");
        }

        @Test
        @DisplayName("Constructor với sessionId có giá trị - giao dịch liên quan phiên")
        void constructorSessionIdCoGiaTri() {
            TransactionRecord tr = buildWithSession();
            assertNotNull(tr.sessionId());
            assertEquals(5, tr.sessionId());
        }
    }

    // ═══════════════════════════════════════════════════
    // ACCESSOR METHODS (GETTERS)
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Accessor methods (transactionId, userId, amount, ...)")
    class Accessors {

        @Test
        @DisplayName("transactionId() trả đúng ID")
        void transactionIdDung() {
            assertEquals("TXN-001", buildDefault().transactionId());
        }

        @Test
        @DisplayName("userId() trả đúng userId")
        void userIdDung() {
            assertEquals(42, buildDefault().userId());
        }

        @Test
        @DisplayName("amount() trả đúng BigDecimal (không mất độ chính xác)")
        void amountDung() {
            BigDecimal expected = new BigDecimal("500000");
            assertEquals(0, buildDefault().amount().compareTo(expected));
        }

        @Test
        @DisplayName("transactionType() trả đúng enum")
        void transactionTypeDung() {
            assertEquals(TransactionType.DEPOSIT, buildDefault().transactionType());
        }

        @Test
        @DisplayName("createdAt() trả đúng Instant")
        void createdAtDung() {
            Instant expected = Instant.parse("2026-01-15T10:00:00Z");
            assertEquals(expected, buildDefault().createdAt());
        }

        @Test
        @DisplayName("status() trả đúng TransactionStatus")
        void statusDung() {
            assertEquals(TransactionStatus.SUCCESS, buildDefault().status());
        }

        @Test
        @DisplayName("description() trả đúng mô tả")
        void descriptionDung() {
            assertEquals("Nạp tiền vào tài khoản", buildDefault().description());
        }

        @Test
        @DisplayName("sessionId() trả null khi không có session")
        void sessionIdNull() {
            assertNull(buildDefault().sessionId());
        }

        @Test
        @DisplayName("sessionId() trả đúng giá trị khi có session")
        void sessionIdCoGiaTri() {
            assertEquals(5, buildWithSession().sessionId());
        }
    }

    // ═══════════════════════════════════════════════════
    // MỌI TransactionType
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Mọi TransactionType - không bị lỗi")
    class AllTransactionTypes {

        @ParameterizedTest
        @EnumSource(TransactionType.class)
        @DisplayName("Constructor chấp nhận mọi TransactionType")
        void moiTransactionType(TransactionType type) {
            TransactionRecord tr = new TransactionRecord(
                    "TXN-" + type.name(), 1,
                    new BigDecimal("100000"),
                    type,
                    Instant.now(),
                    TransactionStatus.SUCCESS,
                    "Test " + type, null
            );
            assertEquals(type, tr.transactionType());
        }
    }

    // ═══════════════════════════════════════════════════
    // MỌI TransactionStatus
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Mọi TransactionStatus - không bị lỗi")
    class AllTransactionStatuses {

        @ParameterizedTest
        @EnumSource(TransactionStatus.class)
        @DisplayName("Constructor chấp nhận mọi TransactionStatus")
        void moiTransactionStatus(TransactionStatus status) {
            TransactionRecord tr = new TransactionRecord(
                    "TXN-S", 1,
                    new BigDecimal("100000"),
                    TransactionType.DEPOSIT,
                    Instant.now(),
                    status,
                    "Test status", null
            );
            assertEquals(status, tr.status());
        }
    }

    // ═══════════════════════════════════════════════════
    // NGHIỆP VỤ - CÁC LOẠI GIAO DỊCH THỰC TẾ
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Kịch bản nghiệp vụ thực tế")
    class BusinessScenarios {

        @Test
        @DisplayName("DEPOSIT - Nạp tiền vào tài khoản")
        void depositScenario() {
            TransactionRecord tr = new TransactionRecord(
                    "DEP-001", 10,
                    new BigDecimal("1000000"),
                    TransactionType.DEPOSIT,
                    Instant.now(),
                    TransactionStatus.SUCCESS,
                    "Nạp tiền qua VNPay",
                    null
            );
            assertEquals(TransactionType.DEPOSIT, tr.transactionType());
            assertNull(tr.sessionId(), "Nạp tiền không liên quan phiên đấu giá");
            assertEquals(0, tr.amount().compareTo(new BigDecimal("1000000")));
        }

        @Test
        @DisplayName("BID_PAYMENT - Thanh toán trúng đấu giá")
        void bidPaymentScenario() {
            TransactionRecord tr = new TransactionRecord(
                    "PAY-001", 15,
                    new BigDecimal("50000000"),
                    TransactionType.BID_PAYMENT,
                    Instant.now(),
                    TransactionStatus.SUCCESS,
                    "Thanh toán Rolex Submariner phiên #12",
                    12
            );
            assertEquals(TransactionType.BID_PAYMENT, tr.transactionType());
            assertNotNull(tr.sessionId(), "Thanh toán đấu giá phải có sessionId");
            assertEquals(12, tr.sessionId());
        }

        @Test
        @DisplayName("REFUND - Hoàn tiền khi không thắng")
        void refundScenario() {
            TransactionRecord tr = new TransactionRecord(
                    "REF-001", 20,
                    new BigDecimal("5000000"),
                    TransactionType.REFUND,
                    Instant.now(),
                    TransactionStatus.SUCCESS,
                    "Hoàn tiền đặt cọc phiên #8",
                    8
            );
            assertEquals(TransactionType.REFUND, tr.transactionType());
            assertEquals(TransactionStatus.SUCCESS, tr.status());
        }

        @Test
        @DisplayName("Giao dịch PENDING - chờ xác nhận")
        void pendingTransaction() {
            TransactionRecord tr = new TransactionRecord(
                    "TXN-PEND", 5,
                    new BigDecimal("200000"),
                    TransactionType.DEPOSIT,
                    Instant.now(),
                    TransactionStatus.PENDING,
                    "Đang chờ xác nhận từ ngân hàng",
                    null
            );
            assertEquals(TransactionStatus.PENDING, tr.status());
        }

        @Test
        @DisplayName("transactionId với nhiều định dạng - không bị thay đổi")
        void transactionIdNhieuDinhDang() {
            String[] ids = {
                    "TXN-001",
                    "VNP-20260115-001",
                    "uuid-550e8400-e29b-41d4-a716",
                    "1234567890"
            };
            for (String id : ids) {
                TransactionRecord tr = new TransactionRecord(
                        id, 1, BigDecimal.TEN, TransactionType.DEPOSIT,
                        Instant.now(), TransactionStatus.SUCCESS, "D", null
                );
                assertEquals(id, tr.transactionId());
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // EDGE CASES
    // ═══════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases - giá trị biên")
    class EdgeCases {

        @Test
        @DisplayName("amount rất lớn - không mất độ chính xác")
        void amountRatLon() {
            BigDecimal bigAmount = new BigDecimal("999999999999999.99");
            TransactionRecord tr = new TransactionRecord(
                    "TXN-BIG", 1, bigAmount,
                    TransactionType.BID_PAYMENT, Instant.now(),
                    TransactionStatus.SUCCESS, "D", null
            );
            assertEquals(0, tr.amount().compareTo(bigAmount));
        }

        @Test
        @DisplayName("amount = 0 - trường hợp edge case")
        void amountZero() {
            TransactionRecord tr = new TransactionRecord(
                    "TXN-ZERO", 1, BigDecimal.ZERO,
                    TransactionType.REFUND, Instant.now(),
                    TransactionStatus.SUCCESS, "D", null
            );
            assertEquals(0, tr.amount().compareTo(BigDecimal.ZERO));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 10, 999, Integer.MAX_VALUE})
        @DisplayName("userId với nhiều giá trị khác nhau")
        void userIdNhieuGiaTri(int userId) {
            TransactionRecord tr = new TransactionRecord(
                    "TXN", userId, BigDecimal.TEN,
                    TransactionType.DEPOSIT, Instant.now(),
                    TransactionStatus.SUCCESS, "D", null
            );
            assertEquals(userId, tr.userId());
        }

        @Test
        @DisplayName("description null - không ném exception")
        void descriptionNull() {
            assertDoesNotThrow(() -> new TransactionRecord(
                    "TXN", 1, BigDecimal.TEN,
                    TransactionType.DEPOSIT, Instant.now(),
                    TransactionStatus.SUCCESS, null, null
            ));
        }

        @Test
        @DisplayName("Hai TransactionRecord hoàn toàn độc lập - không chia sẻ state")
        void haiInstanceDocLap() {
            TransactionRecord tr1 = buildDefault();
            TransactionRecord tr2 = buildWithSession();

            assertEquals("TXN-001", tr1.transactionId());
            assertEquals("TXN-002", tr2.transactionId());
            assertEquals(42, tr1.userId());
            assertEquals(42, tr2.userId());

            // amount khác nhau
            assertNotEquals(0, tr1.amount().compareTo(tr2.amount()));
        }

        @Test
        @DisplayName("createdAt với nhiều giá trị Instant khác nhau")
        void createdAtNhieuGiaTri() {
            Instant[] instants = {
                    Instant.EPOCH,
                    Instant.parse("2020-01-01T00:00:00Z"),
                    Instant.parse("2026-05-25T12:00:00Z"),
                    Instant.now()
            };
            for (Instant t : instants) {
                TransactionRecord tr = new TransactionRecord(
                        "TXN", 1, BigDecimal.TEN, TransactionType.DEPOSIT,
                        t, TransactionStatus.SUCCESS, "D", null
                );
                assertEquals(t, tr.createdAt());
            }
        }
    }
}