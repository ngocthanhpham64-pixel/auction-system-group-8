package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge-case & concurrency tests for {@link AuthService}.
 * Khác AuthServiceTest: tập trung vào định dạng email đặc biệt,
 * khoảng trắng trong input, concurrent login, logout idempotent.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AuthServiceEdgeCaseTest {

    @BeforeEach
    void reset() { SessionManager.clearSession(); }

    // ══════════════════════════════════════════════════════
    // Email format edge cases
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {"  ", "\t", "\n", "   \t  "})
    @DisplayName("login: email chỉ whitespace → onFailure (không gọi server)")
    void login_whitespaceOnlyEmail_callsOnFailure(String email) {
        AtomicReference<String> error = new AtomicReference<>();
        AuthService.login(email, "password123",
                () -> fail("onSuccess không được gọi"),
                error::set);
        assertNotNull(error.get(), "onFailure phải gọi với email='" + email + "'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("login: password chỉ whitespace → onFailure")
    void login_whitespaceOnlyPassword_callsOnFailure(String pass) {
        AtomicReference<String> error = new AtomicReference<>();
        AuthService.login("user@test.com", pass,
                () -> fail("Không được gọi"), error::set);
        assertNotNull(error.get());
    }

    @Test
    @DisplayName("login: email rất dài (500 ký tự) → onFailure hoặc attempt gửi server (không crash)")
    void login_veryLongEmail_noCrash() {
        String longEmail = "a".repeat(245) + "@" + "b".repeat(245) + ".com";
        assertDoesNotThrow(() ->
                AuthService.login(longEmail, "pass1234", () -> {}, err -> {}));
    }

    @Test
    @DisplayName("login: password rất dài (1000 ký tự) → không crash")
    void login_veryLongPassword_noCrash() {
        String longPass = "P@ss".repeat(250);
        assertDoesNotThrow(() ->
                AuthService.login("user@test.com", longPass, () -> {}, err -> {}));
    }

    // ══════════════════════════════════════════════════════
    // onSuccess / onFailure null-safe
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("login: onSuccess null, email rỗng → không crash (onFailure vẫn safe)")
    void login_nullOnSuccess_emptyEmail_noCrash() {
        assertDoesNotThrow(() ->
                AuthService.login("", "pass", null, err -> {}));
    }

    @Test
    @DisplayName("login: onFailure null, email rỗng → không crash (NPE guard)")
    void login_nullOnFailure_emptyEmail_noCrash() {
        assertDoesNotThrow(() ->
                AuthService.login("", "pass", () -> {}, null));
    }

    @Test
    @DisplayName("login: cả hai callback null → không crash")
    void login_bothCallbacksNull_noCrash() {
        assertDoesNotThrow(() ->
                AuthService.login("u@test.com", "pass", null, null));
    }

    // ══════════════════════════════════════════════════════
    // requestOtp edge cases
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("requestOtp: email null → onFailure hoặc không crash")
    void requestOtp_nullEmail_noCrash() {
        assertDoesNotThrow(() ->
                AuthService.requestOtp(null, otp -> {}, err -> {}));
    }

    @Test
    @DisplayName("requestOtp: email rỗng → không crash")
    void requestOtp_emptyEmail_noCrash() {
        assertDoesNotThrow(() ->
                AuthService.requestOtp("", otp -> {}, err -> {}));
    }

    @Test
    @DisplayName("requestOtp: onSuccess null → không crash")
    void requestOtp_nullOnSuccess_noCrash() {
        assertDoesNotThrow(() ->
                AuthService.requestOtp("user@test.com", null, err -> {}));
    }

    @Test
    @DisplayName("requestOtp: onFailure null → không crash")
    void requestOtp_nullOnFailure_noCrash() {
        assertDoesNotThrow(() ->
                AuthService.requestOtp("user@test.com", otp -> {}, null));
    }

    // ══════════════════════════════════════════════════════
    // resetPassword edge cases
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("resetPassword: tất cả params null → không crash")
    void resetPassword_allNull_noCrash() {
        assertDoesNotThrow(() ->
                AuthService.resetPassword(null, null, null, null, null));
    }

    @Test
    @DisplayName("resetPassword: otp rỗng → onFailure được gọi")
    void resetPassword_emptyOtp_callsOnFailure() {
        AtomicReference<String> error = new AtomicReference<>();
        AuthService.resetPassword("u@test.com", "", "newPass1",
                () -> {}, error::set);
        assertNotNull(error.get());
    }

    // ══════════════════════════════════════════════════════
    // logout idempotent
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("logout 3 lần liên tiếp → idempotent, không crash")
    void logout_calledThreeTimes_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AuthService.logout();
        AuthService.logout();
        assertDoesNotThrow(AuthService::logout);
    }

    @Test
    @DisplayName("logout sau khi clearSession → không crash")
    void logout_afterClearSession_noCrash() {
        SessionManager.clearSession();
        assertDoesNotThrow(AuthService::logout);
    }

    // ══════════════════════════════════════════════════════
    // Concurrency
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("login 8 thread đồng thời (email rỗng) → tất cả onFailure được gọi")
    void login_concurrent8Threads_allOnFailureCalled() throws InterruptedException {
        int threadCount = 8;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() ->
                    AuthService.login("", "pass", () -> latch.countDown(), err -> {
                        failureCount.incrementAndGet();
                        latch.countDown();
                    })
            ).start();
        }

        boolean done = latch.await(5, TimeUnit.SECONDS);
        assertTrue(done, "Tất cả thread phải hoàn thành trong 5s");
        assertEquals(threadCount, failureCount.get(),
                "Tất cả phải nhận onFailure vì email rỗng");
    }

    @Test
    @DisplayName("logout gọi từ nhiều thread đồng thời → không deadlock")
    void logout_concurrent_noDeadlock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                AuthService.logout();
                latch.countDown();
            }).start();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ══════════════════════════════════════════════════════
    // onFailure message quality
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("login failure message: không phải stack trace (không chứa 'at ')")
    void login_failureMessage_notStackTrace() {
        AtomicReference<String> error = new AtomicReference<>();
        AuthService.login("", "pass", () -> {}, error::set);
        if (error.get() != null) {
            assertFalse(error.get().contains("at "),
                    "Message không được là stack trace: " + error.get());
        }
    }

    @Test
    @DisplayName("login failure message: không null, không rỗng")
    void login_failureMessage_notNullOrBlank() {
        AtomicReference<String> error = new AtomicReference<>();
        AuthService.login(null, "pass", () -> {}, error::set);
        assertNotNull(error.get());
        assertFalse(error.get().isBlank());
    }
}