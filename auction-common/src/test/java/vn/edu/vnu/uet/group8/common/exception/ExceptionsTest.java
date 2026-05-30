package vn.edu.vnu.uet.group8.common.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.edu.vnu.uet.group8.common.enums.UserStatus;

/**
 * Test cho 15 exception class trong package {@code common.exception}.
 *
 * <p>Đa số là kiểm tra constructor + getMessage(). Một số class
 * có getter riêng (AccountLockedException.getStatus, UserNotFoundException.getUserId).
 */
class ExceptionsTest {

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AccountLockedException")
    class AccountLockedTest {
        @Test
        @DisplayName("Constructor lưu status, getStatus trả đúng")
        void getter() {
            AccountLockedException ex = new AccountLockedException(UserStatus.BANNED);
            assertEquals(UserStatus.BANNED, ex.getStatus());
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Status SUSPENDED")
        void suspended() {
            AccountLockedException ex = new AccountLockedException(UserStatus.SUSPENDED);
            assertEquals(UserStatus.SUSPENDED, ex.getStatus());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AuctionClosedException")
    class AuctionClosedTest {
        @Test
        @DisplayName("Message lưu đúng")
        void message() {
            AuctionClosedException ex = new AuctionClosedException("Phiên đã đóng");
            assertEquals("Phiên đã đóng", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AuctionException")
    class AuctionExceptionTest {
        @Test
        @DisplayName("Message lưu đúng")
        void message() {
            AuctionException ex = new AuctionException("Lỗi phiên đấu giá");
            assertEquals("Lỗi phiên đấu giá", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AuthenticationException")
    class AuthenticationExceptionTest {
        @Test
        @DisplayName("Message lưu đúng")
        void message() {
            AuthenticationException ex = new AuthenticationException("Auth fail");
            assertEquals("Auth fail", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DuplicateTransactionException")
    class DuplicateTransactionTest {
        @Test
        @DisplayName("Constructor lưu transactionId trong message")
        void message() {
            DuplicateTransactionException ex = new DuplicateTransactionException("tx-123");
            assertTrue(ex.getMessage().contains("tx-123"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DuplicateUserException")
    class DuplicateUserTest {
        @Test
        @DisplayName("Constructor 2 tham số (field, value) → message chứa cả 2")
        void messageDayDu() {
            DuplicateUserException ex = new DuplicateUserException("email", "a@e.com");
            assertTrue(ex.getMessage().contains("email"));
            assertTrue(ex.getMessage().contains("a@e.com"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("InsufficientBalanceException")
    class InsufficientBalanceTest {
        @Test
        @DisplayName("Message lưu đúng")
        void message() {
            InsufficientBalanceException ex = new InsufficientBalanceException("Không đủ tiền");
            assertEquals("Không đủ tiền", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("InvalidBidException")
    class InvalidBidTest {
        @Test
        @DisplayName("Message lưu đúng")
        void message() {
            InvalidBidException ex = new InvalidBidException("Giá thấp hơn giá hiện tại");
            assertEquals("Giá thấp hơn giá hiện tại", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("InvalidCredentialsException")
    class InvalidCredentialsTest {
        @Test
        @DisplayName("Constructor no-arg → vẫn có message default")
        void noArg() {
            InvalidCredentialsException ex = new InvalidCredentialsException();
            assertNotNull(ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("InvalidSpecException")
    class InvalidSpecTest {
        @Test
        @DisplayName("Constructor 2 tham số (specKey, reason)")
        void messageDayDu() {
            InvalidSpecException ex = new InvalidSpecException("BRAND", "không hợp lệ");
            assertTrue(ex.getMessage().contains("BRAND"));
            assertTrue(ex.getMessage().contains("không hợp lệ"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("ItemNotFoundException")
    class ItemNotFoundTest {
        @Test
        @DisplayName("Constructor int itemId → message chứa id")
        void chunNumber() {
            ItemNotFoundException ex = new ItemNotFoundException(42);
            assertTrue(ex.getMessage().contains("42"));
        }

        @Test
        @DisplayName("Constructor String message")
        void chunString() {
            ItemNotFoundException ex = new ItemNotFoundException("Item not found");
            assertEquals("Item not found", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("UnauthorizedException")
    class UnauthorizedTest {
        @Test
        @DisplayName("Constructor (action) → message chứa action")
        void chuaAction() {
            UnauthorizedException ex = new UnauthorizedException("xóa item của người khác");
            assertTrue(ex.getMessage().contains("xóa item"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("UserNotFoundException")
    class UserNotFoundTest {
        @Test
        @DisplayName("Constructor (int userId) → message chứa id, getUserId trả đúng")
        void chunUserId() {
            UserNotFoundException ex = new UserNotFoundException(99);
            assertEquals(99, ex.getUserId());
            assertTrue(ex.getMessage().contains("99"));
        }

        @Test
        @DisplayName("Constructor (String)")
        void chunString() {
            UserNotFoundException ex = new UserNotFoundException("User missing");
            assertEquals("User missing", ex.getMessage());
        }

        @Test
        @DisplayName("Constructor (String, Throwable cause)")
        void chunCause() {
            RuntimeException cause = new RuntimeException("DB lỗi");
            UserNotFoundException ex = new UserNotFoundException("User missing", cause);
            assertEquals("User missing", ex.getMessage());
            assertSame(cause, ex.getCause());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("UserServiceException")
    class UserServiceTest {
        @Test
        @DisplayName("Message lưu đúng")
        void message() {
            UserServiceException ex = new UserServiceException("Lỗi service");
            assertEquals("Lỗi service", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("ValidationException")
    class ValidationTest {
        @Test
        @DisplayName("Message lưu đúng")
        void message() {
            ValidationException ex = new ValidationException("Invalid input");
            assertEquals("Invalid input", ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Tất cả exception đều extends RuntimeException")
    void deuLaRuntimeException() {
        assertTrue(new AccountLockedException(UserStatus.BANNED) instanceof RuntimeException);
        assertTrue(new AuctionClosedException("x") instanceof RuntimeException);
        assertTrue(new AuctionException("x") instanceof RuntimeException);
        assertTrue(new AuthenticationException("x") instanceof RuntimeException);
        assertTrue(new DuplicateTransactionException("tx") instanceof RuntimeException);
        assertTrue(new DuplicateUserException("f", "v") instanceof RuntimeException);
        assertTrue(new InsufficientBalanceException("x") instanceof RuntimeException);
        assertTrue(new InvalidBidException("x") instanceof RuntimeException);
        assertTrue(new InvalidCredentialsException() instanceof RuntimeException);
        assertTrue(new InvalidSpecException("k", "r") instanceof RuntimeException);
        assertTrue(new ItemNotFoundException(1) instanceof RuntimeException);
        assertTrue(new UnauthorizedException("x") instanceof RuntimeException);
        assertTrue(new UserNotFoundException(1) instanceof RuntimeException);
        assertTrue(new UserServiceException("x") instanceof RuntimeException);
        assertTrue(new ValidationException("x") instanceof RuntimeException);
    }
}