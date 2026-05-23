package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserService (client-side validation)")
class UserServiceTest {

    @BeforeEach
    void setup() {
        SessionManager.clearSession();
    }

    // =========================================================
    // deposit
    // =========================================================
    @Nested
    @DisplayName("deposit – validate")
    class Deposit {

        @Test
        @DisplayName("amount null -> false")
        void nullAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            UserService.deposit(
                    null,
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }

        @Test
        @DisplayName("amount = 0 -> false")
        void zeroAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            UserService.deposit(
                    BigDecimal.ZERO,
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }

        @Test
        @DisplayName("amount âm -> false")
        void negativeAmountReturnsFalse() {

            SessionManager.setSession(
                    "tok",
                    1,
                    "u",
                    "f",
                    "MEMBER"
            );

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            UserService.deposit(
                    new BigDecimal("-500"),
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }

        @Test
        @DisplayName("chưa đăng nhập -> false")
        void notLoggedInReturnsFalse() {

            AtomicReference<Boolean> result =
                    new AtomicReference<>();

            UserService.deposit(
                    new BigDecimal("500000"),
                    result::set
            );

            assertNotNull(result.get());
            assertFalse(result.get());
        }

        @Test
        @DisplayName("null callback, invalid amount -> không ném ngoại lệ")
        void nullCallbackNoException() {

            assertDoesNotThrow(() ->
                    UserService.deposit(null, null)
            );
        }
    }
}