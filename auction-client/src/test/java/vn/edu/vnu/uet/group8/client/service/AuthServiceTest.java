package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.client.TestFXSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthService")
class AuthServiceTest extends TestFXSetup {

    @BeforeEach
    void clearSession() {
        SessionManager.clearSession();
    }

    @Nested
    @DisplayName("login validation")
    class LoginValidation {

        @Test
        @DisplayName("username null -> onFailure")
        void nullUsernameCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.login(
                    null,
                    "password",
                    () -> {},
                    e -> errors.add(e)
            );

            assertFalse(errors.isEmpty());
            assertEquals(
                    "Vui lòng nhập tên đăng nhập",
                    errors.get(0)
            );
        }

        @Test
        @DisplayName("username blank -> onFailure")
        void blankUsernameCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.login(
                    "   ",
                    "password",
                    () -> {},
                    e -> errors.add(e)
            );

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("password null -> onFailure")
        void nullPasswordCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.login(
                    "alice",
                    null,
                    () -> {},
                    e -> errors.add(e)
            );

            assertFalse(errors.isEmpty());
            assertEquals(
                    "Vui lòng nhập mật khẩu",
                    errors.get(0)
            );
        }

        @Test
        @DisplayName("password empty -> onFailure")
        void emptyPasswordCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.login(
                    "alice",
                    "",
                    () -> {},
                    e -> errors.add(e)
            );

            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("validation fail -> onSuccess NOT called")
        void validationFailDoesNotCallSuccess() {
            AtomicBoolean successCalled = new AtomicBoolean(false);

            AuthService.login(
                    null,
                    null,
                    () -> successCalled.set(true),
                    e -> {}
            );

            assertFalse(successCalled.get());
        }

        @Test
        @DisplayName("không kết nối server -> onFailure")
        void noConnectionCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.login(
                    "alice",
                    "password",
                    () -> {},
                    e -> errors.add(e)
            );

            assertFalse(errors.isEmpty());
            assertEquals(
                    "Không có kết nối đến server",
                    errors.get(0)
            );
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("logout không ném exception")
        void logoutNoException() {
            assertDoesNotThrow(AuthService::logout);
        }
    }
}