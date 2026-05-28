package vn.edu.vnu.uet.group8.client.service;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthService – Complete Test")
class AuthServiceTest {

    @BeforeAll
    static void initToolkit() {
        new JFXPanel();
    }

    @BeforeEach
    void clearSession() {
        SessionManager.clearSession();
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Nested
    @DisplayName("login validation")
    class LoginValidation {

        @Test
        @DisplayName("email null -> onFailure")
        void nullEmailCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.login(
                    null,
                    "password",
                    () -> {},
                    errors::add
            );

            assertFalse(errors.isEmpty());
            assertEquals(
                    "Vui lòng nhập tên đăng nhập",
                    errors.get(0)
            );
        }

        @Test
        @DisplayName("email blank -> onFailure")
        void blankEmailCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.login(
                    "   ",
                    "password",
                    () -> {},
                    errors::add
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
                    errors::add
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
                    errors::add
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
        @DisplayName("null email -> onSuccess NOT called")
        void nullEmailDoesNotCallSuccess() {
            AtomicBoolean successCalled = new AtomicBoolean(false);

            AuthService.login(
                    null,
                    "password",
                    () -> successCalled.set(true),
                    e -> {}
            );

            assertFalse(successCalled.get());
        }

        @Test
        @DisplayName("null password -> onSuccess NOT called")
        void nullPasswordDoesNotCallSuccess() {
            AtomicBoolean successCalled = new AtomicBoolean(false);

            AuthService.login(
                    "alice",
                    null,
                    () -> successCalled.set(true),
                    e -> {}
            );

            assertFalse(successCalled.get());
        }

        @Test
        @DisplayName("no server connection -> onFailure")
        void noConnectionCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.login(
                    "alice",
                    "password",
                    () -> {},
                    errors::add
            );

            assertFalse(errors.isEmpty());
            assertTrue(
                    errors.get(0).contains("kết nối")
            );
        }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("logout does not throw exception")
        void logoutNoException() {
            assertDoesNotThrow(AuthService::logout);
        }

        @Test
        @DisplayName("logout when not connected -> no crash")
        void logoutWhenNotConnected() {
            SessionManager.setSession(
                    "token",
                    1,
                    "user",
                    "fullname",
                    "MEMBER"
            );

            assertDoesNotThrow(AuthService::logout);
        }

        @Test
        @DisplayName("logout clears session")
        void logoutClearsSession() {
            SessionManager.setSession(
                    "token",
                    1,
                    "user",
                    "fullname",
                    "MEMBER"
            );

            AuthService.logout();

            assertFalse(SessionManager.isLoggedIn());
        }

        @Test
        @DisplayName("logout when not logged in -> no crash")
        void logoutWhenNotLoggedIn() {
            assertDoesNotThrow(AuthService::logout);
        }
    }

    // =========================================================
    // REQUEST OTP
    // =========================================================

    @Nested
    @DisplayName("requestOtp")
    class RequestOtp {

        @Test
        @DisplayName("no connection -> onFailure")
        void noConnectionCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.requestOtp(
                    "a@b.com",
                    s -> {},
                    errors::add
            );

            assertFalse(errors.isEmpty());
            assertTrue(
                    errors.get(0).contains("kết nối")
            );
        }

        @Test
        @DisplayName("no connection -> onSuccess NOT called")
        void noConnectionDoesNotCallSuccess() {
            AtomicBoolean successCalled = new AtomicBoolean(false);

            AuthService.requestOtp(
                    "a@b.com",
                    s -> successCalled.set(true),
                    e -> {}
            );

            assertFalse(successCalled.get());
        }
    }

    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("no connection -> onFailure")
        void noConnectionCallsFailure() {
            List<String> errors = new ArrayList<>();

            AuthService.resetPassword(
                    "a@b.com",
                    "123456",
                    "newPassword",
                    () -> {},
                    errors::add
            );

            assertFalse(errors.isEmpty());
            assertTrue(
                    errors.get(0).contains("kết nối")
            );
        }

        @Test
        @DisplayName("no connection -> onSuccess NOT called")
        void noConnectionDoesNotCallSuccess() {
            AtomicBoolean successCalled = new AtomicBoolean(false);

            AuthService.resetPassword(
                    "a@b.com",
                    "123456",
                    "newPassword",
                    () -> successCalled.set(true),
                    e -> {}
            );

            assertFalse(successCalled.get());
        }
    }
}