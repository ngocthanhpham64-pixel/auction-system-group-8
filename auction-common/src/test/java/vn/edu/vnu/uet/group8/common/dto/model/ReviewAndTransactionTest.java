package vn.edu.vnu.uet.group8.common.dto.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.vnu.uet.group8.common.enums.TransactionStatus;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

class ReviewAndTransactionTest {

    // ════════════════════════════════════════════════════
    // ReviewDTO
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("ReviewDTO")
    class ReviewDTOTest {

        @Test
        @DisplayName("Constructor đầy đủ - tất cả getter đúng")
        void constructorDayDu() {
            Instant now = Instant.now();
            ReviewDTO dto = new ReviewDTO("nguyen_a", 5, "Rất tốt!", now);

            assertEquals("nguyen_a", dto.getRaterUsername());
            assertEquals(5, dto.getScore());
            assertEquals("Rất tốt!", dto.getComment());
            assertEquals(now, dto.getCreatedAt());
        }

        @Test
        @DisplayName("No-arg constructor - tất cả null/0")
        void noArgConstructor() {
            ReviewDTO dto = new ReviewDTO();
            assertNull(dto.getRaterUsername());
            assertEquals(0, dto.getScore());
            assertNull(dto.getComment());
            assertNull(dto.getCreatedAt());
        }

        @Test
        @DisplayName("score = 1 biên dưới hợp lệ")
        void scoreMin() {
            ReviewDTO dto = new ReviewDTO("u", 1, "tệ", Instant.now());
            assertEquals(1, dto.getScore());
        }

        @Test
        @DisplayName("score = 5 biên trên hợp lệ")
        void scoreMax() {
            ReviewDTO dto = new ReviewDTO("u", 5, "xuất sắc", Instant.now());
            assertEquals(5, dto.getScore());
        }

        @Test
        @DisplayName("score = 0 được phép (DTO không validate)")
        void scoreZero() {
            ReviewDTO dto = new ReviewDTO("u", 0, "c", Instant.now());
            assertEquals(0, dto.getScore());
        }

        @Test
        @DisplayName("score âm được phép (DTO không validate)")
        void scoreNegative() {
            ReviewDTO dto = new ReviewDTO("u", -1, "c", Instant.now());
            assertEquals(-1, dto.getScore());
        }

        @Test
        @DisplayName("raterUsername null được phép")
        void raterUsernameNull() {
            ReviewDTO dto = new ReviewDTO(null, 3, "c", Instant.now());
            assertNull(dto.getRaterUsername());
        }

        @Test
        @DisplayName("comment null được phép")
        void commentNull() {
            ReviewDTO dto = new ReviewDTO("u", 3, null, Instant.now());
            assertNull(dto.getComment());
        }

        @Test
        @DisplayName("comment rỗng được phép")
        void commentEmpty() {
            ReviewDTO dto = new ReviewDTO("u", 3, "", Instant.now());
            assertEquals("", dto.getComment());
        }

        @Test
        @DisplayName("createdAt null được phép")
        void createdAtNull() {
            ReviewDTO dto = new ReviewDTO("u", 3, "c", null);
            assertNull(dto.getCreatedAt());
        }

        @Test
        @DisplayName("comment rất dài hợp lệ")
        void commentVeryLong() {
            String long1000 = "A".repeat(1000);
            ReviewDTO dto = new ReviewDTO("u", 3, long1000, Instant.now());
            assertEquals(1000, dto.getComment().length());
        }

        @Test
        @DisplayName("raterUsername rất dài hợp lệ")
        void raterUsernameLong() {
            String long100 = "x".repeat(100);
            ReviewDTO dto = new ReviewDTO(long100, 4, "c", Instant.now());
            assertEquals(100, dto.getRaterUsername().length());
        }

        @Test
        @DisplayName("createdAt ở Instant.EPOCH hợp lệ")
        void createdAtEpoch() {
            ReviewDTO dto = new ReviewDTO("u", 3, "c", Instant.EPOCH);
            assertEquals(Instant.EPOCH, dto.getCreatedAt());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5})
        @DisplayName("Tất cả score 1-5 đều hợp lệ")
        void allValidScores(int score) {
            ReviewDTO dto = new ReviewDTO("u", score, "c", Instant.now());
            assertEquals(score, dto.getScore());
        }
    }

    // ════════════════════════════════════════════════════
    // TransactionHistoryEntry
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("TransactionHistoryEntry")
    class TransactionHistoryEntryTest {

        @Test
        @DisplayName("Constructor đầy đủ - tất cả getter đúng")
        void constructorDayDu() {
            Instant now = Instant.now();
            TransactionHistoryEntry e = new TransactionHistoryEntry(
                    "tx-001", new BigDecimal("100000"),
                    TransactionType.DEPOSIT, "Nạp tiền", now);

            assertEquals("tx-001", e.transactionId());
            assertEquals(0, e.amount().compareTo(new BigDecimal("100000")));
            assertEquals(TransactionType.DEPOSIT, e.type());
            assertEquals("Nạp tiền", e.description());
            assertEquals(now, e.createdAt());
        }

        @Test
        @DisplayName("No-arg constructor - tất cả null")
        void noArgConstructor() {
            TransactionHistoryEntry e = new TransactionHistoryEntry();
            assertNull(e.transactionId());
            assertNull(e.amount());
            assertNull(e.type());
            assertNull(e.description());
            assertNull(e.createdAt());
        }

        @Test
        @DisplayName("transactionId null được phép")
        void txIdNull() {
            TransactionHistoryEntry e = new TransactionHistoryEntry(
                    null, BigDecimal.ONE, TransactionType.DEPOSIT, "d", Instant.now());
            assertNull(e.transactionId());
        }

        @Test
        @DisplayName("amount = 0 được phép")
        void amountZero() {
            TransactionHistoryEntry e = new TransactionHistoryEntry(
                    "tx", BigDecimal.ZERO, TransactionType.DEPOSIT, "d", Instant.now());
            assertEquals(0, e.amount().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("amount âm được phép (DTO không validate)")
        void amountNegative() {
            TransactionHistoryEntry e = new TransactionHistoryEntry(
                    "tx", new BigDecimal("-1"), TransactionType.WITHDRAW, "d", Instant.now());
            assertTrue(e.amount().compareTo(BigDecimal.ZERO) < 0);
        }

        @Test
        @DisplayName("type null được phép")
        void typeNull() {
            TransactionHistoryEntry e = new TransactionHistoryEntry(
                    "tx", BigDecimal.ONE, null, "d", Instant.now());
            assertNull(e.type());
        }

        @Test
        @DisplayName("description null được phép")
        void descriptionNull() {
            TransactionHistoryEntry e = new TransactionHistoryEntry(
                    "tx", BigDecimal.ONE, TransactionType.DEPOSIT, null, Instant.now());
            assertNull(e.description());
        }

        @Test
        @DisplayName("createdAt null được phép")
        void createdAtNull() {
            TransactionHistoryEntry e = new TransactionHistoryEntry(
                    "tx", BigDecimal.ONE, TransactionType.DEPOSIT, "d", null);
            assertNull(e.createdAt());
        }

        @Test
        @DisplayName("Tất cả TransactionType đều map được")
        void allTransactionTypes() {
            for (TransactionType t : TransactionType.values()) {
                TransactionHistoryEntry e = new TransactionHistoryEntry(
                        "tx", BigDecimal.ONE, t, "d", Instant.now());
                assertEquals(t, e.type());
            }
        }

        @Test
        @DisplayName("amount rất lớn hợp lệ")
        void amountLarge() {
            BigDecimal huge = new BigDecimal("999999999999.99");
            TransactionHistoryEntry e = new TransactionHistoryEntry(
                    "tx", huge, TransactionType.SALE, "Bán được", Instant.now());
            assertEquals(0, e.amount().compareTo(huge));
        }
    }

    // ════════════════════════════════════════════════════
    // TransactionRecord
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("TransactionRecord")
    class TransactionRecordTest {

        @Test
        @DisplayName("Constructor đầy đủ - tất cả getter đúng")
        void constructorDayDu() {
            Instant now = Instant.now();
            TransactionRecord rec = new TransactionRecord(
                    "tx-abc", 5, new BigDecimal("200000"),
                    TransactionType.BID_HOLD, now,
                    TransactionStatus.SUCCESS, "Ký quỹ đặt giá", 10);

            assertEquals("tx-abc", rec.transactionId());
            assertEquals(5, rec.userId());
            assertEquals(0, rec.amount().compareTo(new BigDecimal("200000")));
            assertEquals(TransactionType.BID_HOLD, rec.transactionType());
            assertEquals(now, rec.createdAt());
            assertEquals(TransactionStatus.SUCCESS, rec.status());
            assertEquals("Ký quỹ đặt giá", rec.description());
            assertEquals(10, rec.sessionId());
        }

        @Test
        @DisplayName("No-arg constructor - tất cả null/0")
        void noArgConstructor() {
            TransactionRecord rec = new TransactionRecord();
            assertNull(rec.transactionId());
            assertEquals(0, rec.userId());
            assertNull(rec.amount());
            assertNull(rec.transactionType());
            assertNull(rec.createdAt());
            assertNull(rec.status());
            assertNull(rec.description());
            assertNull(rec.sessionId());
        }

        @Test
        @DisplayName("sessionId null được phép (giao dịch nạp/rút không gắn với phiên)")
        void sessionIdNull() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", 1, BigDecimal.TEN,
                    TransactionType.DEPOSIT, Instant.now(),
                    TransactionStatus.SUCCESS, "Nạp tiền", null);
            assertNull(rec.sessionId());
        }

        @Test
        @DisplayName("sessionId = 0 được phép")
        void sessionIdZero() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", 1, BigDecimal.TEN,
                    TransactionType.DEPOSIT, Instant.now(),
                    TransactionStatus.SUCCESS, "d", 0);
            assertEquals(0, rec.sessionId());
        }

        @Test
        @DisplayName("status = FAILED")
        void statusFailed() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", 1, BigDecimal.ONE,
                    TransactionType.WITHDRAW, Instant.now(),
                    TransactionStatus.FAILED, "Thất bại", null);
            assertEquals(TransactionStatus.FAILED, rec.status());
        }

        @Test
        @DisplayName("status = PENDING")
        void statusPending() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", 1, BigDecimal.ONE,
                    TransactionType.WITHDRAW, Instant.now(),
                    TransactionStatus.PENDING, "Chờ xử lý", null);
            assertEquals(TransactionStatus.PENDING, rec.status());
        }

        @Test
        @DisplayName("status null được phép")
        void statusNull() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", 1, BigDecimal.ONE,
                    TransactionType.DEPOSIT, Instant.now(),
                    null, "d", null);
            assertNull(rec.status());
        }

        @Test
        @DisplayName("transactionType null được phép")
        void txTypeNull() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", 1, BigDecimal.ONE,
                    null, Instant.now(),
                    TransactionStatus.SUCCESS, "d", null);
            assertNull(rec.transactionType());
        }

        @Test
        @DisplayName("Tất cả TransactionType đều map được")
        void allTransactionTypes() {
            for (TransactionType t : TransactionType.values()) {
                TransactionRecord rec = new TransactionRecord(
                        "tx", 1, BigDecimal.ONE, t, Instant.now(),
                        TransactionStatus.SUCCESS, "d", null);
                assertEquals(t, rec.transactionType());
            }
        }

        @Test
        @DisplayName("Tất cả TransactionStatus đều map được")
        void allTransactionStatuses() {
            for (TransactionStatus s : TransactionStatus.values()) {
                TransactionRecord rec = new TransactionRecord(
                        "tx", 1, BigDecimal.ONE,
                        TransactionType.DEPOSIT, Instant.now(),
                        s, "d", null);
                assertEquals(s, rec.status());
            }
        }

        @Test
        @DisplayName("userId âm được phép (DTO không validate)")
        void userIdNegative() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", -1, BigDecimal.ONE,
                    TransactionType.DEPOSIT, Instant.now(),
                    TransactionStatus.SUCCESS, "d", null);
            assertEquals(-1, rec.userId());
        }

        @Test
        @DisplayName("amount = 0 được phép")
        void amountZero() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", 1, BigDecimal.ZERO,
                    TransactionType.DEPOSIT, Instant.now(),
                    TransactionStatus.SUCCESS, "d", null);
            assertEquals(0, rec.amount().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("description rỗng được phép")
        void descriptionEmpty() {
            TransactionRecord rec = new TransactionRecord(
                    "tx", 1, BigDecimal.ONE,
                    TransactionType.DEPOSIT, Instant.now(),
                    TransactionStatus.SUCCESS, "", null);
            assertEquals("", rec.description());
        }
    }
}