package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Detailed validation & concurrency tests for {@link RegisterService}.
 * Tập trung: boundary length, ký tự đặc biệt, concurrent register, message quality.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RegisterServiceValidationTest {

    // ══════════════════════════════════════════════════════
    // username — boundary length
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("register: username 1 ký tự → onFailure (quá ngắn)")
    void register_usernameOneChar_callsOnFailure() {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("a", "e@e.com", "pass123A", "Full Name", "0123456789",
                () -> fail("Không được gọi"), error::set);
        assertNotNull(error.get());
    }

    @Test
    @DisplayName("register: username toàn khoảng trắng → onFailure")
    void register_usernameAllSpaces_callsOnFailure() {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("   ", "e@e.com", "pass123A", "Full", "0123456789",
                () -> fail("Không được gọi"), error::set);
        assertNotNull(error.get());
    }

    @Test
    @DisplayName("register: username rất dài (300 ký tự) → không crash")
    void register_veryLongUsername_noCrash() {
        String longUser = "a".repeat(300);
        assertDoesNotThrow(() ->
                RegisterService.register(longUser, "e@e.com", "pass123A", "Full", "0123456789",
                        () -> {}, err -> {}));
    }

    // ══════════════════════════════════════════════════════
    // email — format validation
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {"notanemail", "missing-at.com", "@nodomain", "two@@domain.com", "  "})
    @DisplayName("register: email không hợp lệ → onFailure")
    void register_invalidEmail_callsOnFailure(String email) {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("validuser", email, "pass123A", "Full Name", "0123456789",
                () -> fail("Không được gọi"), error::set);
        assertNotNull(error.get(), "Email '" + email + "' phải bị từ chối");
    }

    @Test
    @DisplayName("register: email dài 500 ký tự → không crash")
    void register_veryLongEmail_noCrash() {
        String longEmail = "a".repeat(244) + "@" + "b".repeat(244) + ".vn";
        assertDoesNotThrow(() ->
                RegisterService.register("user", longEmail, "pass123A", "Full", "0123456789",
                        () -> {}, err -> {}));
    }

    // ══════════════════════════════════════════════════════
    // password — strength boundary
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {"12345", "abc", "1234567"})
    @DisplayName("register: password < 8 ký tự → onFailure (quá ngắn)")
    void register_shortPassword_callsOnFailure(String pass) {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("validuser", "e@e.com", pass, "Full Name", "0123456789",
                () -> fail("Không được gọi"), error::set);
        assertNotNull(error.get(), "Password '" + pass + "' quá ngắn phải bị từ chối");
    }

    @Test
    @DisplayName("register: password toàn khoảng trắng → onFailure")
    void register_passwordAllSpaces_callsOnFailure() {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("validuser", "e@e.com", "        ", "Full", "0123456789",
                () -> fail("Không được gọi"), error::set);
        assertNotNull(error.get());
    }

    @Test
    @DisplayName("register: password rất dài (1000 ký tự) → không crash")
    void register_veryLongPassword_noCrash() {
        String longPass = "Aa1!".repeat(250);
        assertDoesNotThrow(() ->
                RegisterService.register("user", "e@e.com", longPass, "Full", "0123456789",
                        () -> {}, err -> {}));
    }

    // ══════════════════════════════════════════════════════
    // phone — format boundary
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "01234567890", "abc1234567", "+84123456789", "0123 456 789"})
    @DisplayName("register: phone không hợp lệ → onFailure")
    void register_invalidPhone_callsOnFailure(String phone) {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("validuser", "e@e.com", "pass123A", "Full Name", phone,
                () -> fail("Không được gọi"), error::set);
        assertNotNull(error.get(), "Phone '" + phone + "' phải bị từ chối");
    }

    // ══════════════════════════════════════════════════════
    // fullName boundary
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("register: fullName 1 ký tự → onFailure (quá ngắn)")
    void register_fullNameOneChar_callsOnFailure() {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("validuser", "e@e.com", "pass123A", "A", "0123456789",
                () -> fail("Không được gọi"), error::set);
        assertNotNull(error.get());
    }

    @Test
    @DisplayName("register: fullName rất dài (500 ký tự) → không crash")
    void register_veryLongFullName_noCrash() {
        String longName = "Nguyễn ".repeat(70);
        assertDoesNotThrow(() ->
                RegisterService.register("user", "e@e.com", "pass123A", longName, "0123456789",
                        () -> {}, err -> {}));
    }

    // ══════════════════════════════════════════════════════
    // onFailure message quality
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("register failure message: không null, không rỗng, không stack trace")
    void register_failureMessage_quality() {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("", "", "", "", "",
                () -> {}, error::set);

        assertNotNull(error.get(), "Failure message không được null");
        assertFalse(error.get().isBlank(), "Failure message không được rỗng");
        assertFalse(error.get().contains("at "), "Failure message không được là stack trace");
    }

    @Test
    @DisplayName("register: tất cả field hợp lệ nhưng không kết nối → failure message có 'kết nối'")
    void register_noConnection_failureMessageContainsKetNoi() {
        AtomicReference<String> error = new AtomicReference<>();
        RegisterService.register("validuser", "valid@email.com", "pass123A",
                "Valid Name", "0123456789", () -> {}, error::set);
        assertNotNull(error.get());
        // Message phải đề cập đến kết nối server
        assertTrue(
                error.get().toLowerCase().contains("kết nối")
                        || error.get().toLowerCase().contains("server")
                        || error.get().toLowerCase().contains("connect"),
                "Failure message phải đề cập kết nối: " + error.get()
        );
    }

    // ══════════════════════════════════════════════════════
    // Concurrency
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("register 6 thread cùng lúc (email rỗng) → tất cả onFailure được gọi")
    void register_concurrent6_allOnFailureCalled() throws InterruptedException {
        int n = 6;
        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            new Thread(() ->
                    RegisterService.register("u", "", "pass123A", "Name", "0123456789",
                            () -> latch.countDown(),
                            err -> { failCount.incrementAndGet(); latch.countDown(); })
            ).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS),
                "Tất cả thread phải hoàn thành trong 5s");
        assertEquals(n, failCount.get(), "Tất cả phải nhận onFailure");
    }

    @Test
    @DisplayName("register 5 thread với các input hợp lệ (không kết nối) → tất cả onFailure")
    void register_concurrent5ValidInput_allFailDueToNoConnection() throws InterruptedException {
        int n = 5;
        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            final int idx = i;
            new Thread(() ->
                    RegisterService.register(
                            "user" + idx, "user" + idx + "@email.com",
                            "Pass1234A", "Full Name " + idx, "012345678" + idx,
                            () -> latch.countDown(),
                            err -> { failCount.incrementAndGet(); latch.countDown(); })
            ).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(n, failCount.get(),
                "Tất cả phải thất bại do không có kết nối server");
    }

    // ══════════════════════════════════════════════════════
    // Null combinations
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("register: từng field null → onFailure được gọi (không NPE)")
    void register_eachFieldNull_callsOnFailure(int nullIndex) {
        String[] fields = {"validuser", "e@e.com", "pass123A", "Full Name", "0123456789"};
        fields[nullIndex] = null;

        AtomicReference<String> error = new AtomicReference<>();
        assertDoesNotThrow(() ->
                RegisterService.register(
                        fields[0], fields[1], fields[2], fields[3], fields[4],
                        () -> {}, error::set));
        assertNotNull(error.get(), "onFailure phải gọi khi field[" + nullIndex + "]=null");
    }
}