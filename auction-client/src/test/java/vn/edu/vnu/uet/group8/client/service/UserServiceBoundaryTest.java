package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boundary & concurrency tests for {@link UserService}.
 * Tập trung: deposit/withdraw boundary, changePassword validation,
 * concurrent calls, loadTransactions empty list assertion.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class UserServiceBoundaryTest {

    @BeforeEach
    void reset() { SessionManager.clearSession(); }

    // ══════════════════════════════════════════════════════
    // deposit — boundary values
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "-1", "-999999999"})
    @DisplayName("deposit: amount <= 0 → onResult(false)")
    void deposit_nonPositiveAmounts_returnsFalse(String amount) {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<Boolean> result = new AtomicReference<>(true);
        UserService.deposit(new BigDecimal(amount), result::set);
        assertFalse(result.get(), "amount=" + amount + " phải false");
    }

    @Test
    @DisplayName("deposit: amount = 0.01 (nhỏ nhất dương) → thất bại do không kết nối")
    void deposit_minPositiveDecimal_failsNoConnection() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<Boolean> result = new AtomicReference<>(true);
        UserService.deposit(new BigDecimal("0.01"), result::set);
        assertFalse(result.get());
    }

    @Test
    @DisplayName("deposit: amount = Long.MAX_VALUE → không crash")
    void deposit_maxLongAmount_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                UserService.deposit(new BigDecimal(Long.MAX_VALUE), r -> {}));
    }

    // ══════════════════════════════════════════════════════
    // withdraw — boundary values
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.001"})
    @DisplayName("withdraw: amount <= 0 → onResult(false)")
    void withdraw_nonPositiveAmounts_returnsFalse(String amount) {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<Boolean> result = new AtomicReference<>(true);
        UserService.withdraw(new BigDecimal(amount), result::set);
        assertFalse(result.get(), "withdraw amount=" + amount + " phải false");
    }

    @Test
    @DisplayName("withdraw: amount = Long.MAX_VALUE → không crash")
    void withdraw_maxLongAmount_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                UserService.withdraw(new BigDecimal(Long.MAX_VALUE), r -> {}));
    }

    // ══════════════════════════════════════════════════════
    // deposit vs withdraw — auth message nhất quán
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("deposit và withdraw auth message nhất quán khi chưa login")
    void depositWithdraw_authMessage_consistent() {
        AtomicReference<Boolean> depResult  = new AtomicReference<>();
        AtomicReference<Boolean> withResult = new AtomicReference<>();

        UserService.deposit(BigDecimal.valueOf(100_000), depResult::set);
        UserService.withdraw(BigDecimal.valueOf(100_000), withResult::set);

        assertFalse(depResult.get(), "deposit phải false khi chưa login");
        assertFalse(withResult.get(), "withdraw phải false khi chưa login");
    }

    // ══════════════════════════════════════════════════════
    // changePassword — validation
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("changePassword: oldPassword null → không crash, callback gọi")
    void changePassword_nullOldPass_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<Boolean> result = new AtomicReference<>();
        assertDoesNotThrow(() ->
                UserService.changePassword(null, "newPass123", result::set));
        assertNotNull(result.get());
    }

    @Test
    @DisplayName("changePassword: newPassword null → không crash, callback gọi")
    void changePassword_nullNewPass_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<Boolean> result = new AtomicReference<>();
        assertDoesNotThrow(() ->
                UserService.changePassword("oldPass", null, result::set));
        assertNotNull(result.get());
    }

    @Test
    @DisplayName("changePassword: cả hai null → không crash, callback gọi")
    void changePassword_bothNull_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<Boolean> result = new AtomicReference<>();
        assertDoesNotThrow(() ->
                UserService.changePassword(null, null, result::set));
        assertNotNull(result.get());
    }

    @Test
    @DisplayName("changePassword: password rỗng → callback gọi (không NPE)")
    void changePassword_emptyPasswords_callbackCalled() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<Boolean> result = new AtomicReference<>();
        UserService.changePassword("", "", result::set);
        assertNotNull(result.get());
    }

    @Test
    @DisplayName("changePassword: password dài 1000 ký tự → không crash")
    void changePassword_veryLongPasswords_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        String longPass = "Aa1@".repeat(250);
        assertDoesNotThrow(() ->
                UserService.changePassword(longPass, longPass, r -> {}));
    }

    // ══════════════════════════════════════════════════════
    // loadTransactions — empty list assert
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("loadTransactions: không kết nối → list rỗng (không null)")
    void loadTransactions_noConnection_emptyNotNull() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<java.util.List<?>> result = new AtomicReference<>();
        UserService.loadTransactions(result::set);
        assertNotNull(result.get(), "Callback phải được gọi với list không null");
        assertTrue(result.get().isEmpty(), "List phải rỗng khi không kết nối");
    }

    @Test
    @DisplayName("loadTransactions: callback null → không crash")
    void loadTransactions_nullCallback_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() -> UserService.loadTransactions(null));
    }

    // ══════════════════════════════════════════════════════
    // updateProfile — field boundary
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("updateProfile: tất cả field null → không crash")
    void updateProfile_allNullFields_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                UserService.updateProfile(null, null, null, null, r -> {}));
    }

    @Test
    @DisplayName("updateProfile: fullName rất dài → không crash")
    void updateProfile_veryLongName_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        String longName = "Nguyễn ".repeat(100);
        assertDoesNotThrow(() ->
                UserService.updateProfile(longName, "0123456789", "Hanoi", null, r -> {}));
    }

    @Test
    @DisplayName("updateProfile: phone sai format → callback gọi (server từ chối)")
    void updateProfile_invalidPhone_callbackCalled() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<Boolean> result = new AtomicReference<>();
        UserService.updateProfile("Valid Name", "notaphone", "Hanoi", null, result::set);
        assertNotNull(result.get());
    }

    // ══════════════════════════════════════════════════════
    // Concurrency
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("deposit 8 thread đồng thời (chưa login) → tất cả callback false")
    void deposit_concurrent8NotLoggedIn_allFalse() throws InterruptedException {
        int n = 8;
        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger falseCount = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            new Thread(() ->
                    UserService.deposit(BigDecimal.valueOf(100_000), result -> {
                        if (!result) falseCount.incrementAndGet();
                        latch.countDown();
                    })
            ).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(n, falseCount.get(), "Tất cả phải false khi chưa login");
    }

    @Test
    @DisplayName("withdraw 8 thread đồng thời (chưa login) → tất cả callback false")
    void withdraw_concurrent8NotLoggedIn_allFalse() throws InterruptedException {
        int n = 8;
        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger falseCount = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            new Thread(() ->
                    UserService.withdraw(BigDecimal.valueOf(50_000), result -> {
                        if (!result) falseCount.incrementAndGet();
                        latch.countDown();
                    })
            ).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(n, falseCount.get());
    }

    @Test
    @DisplayName("changePassword 6 thread đồng thời → không deadlock")
    void changePassword_concurrent6_noCrash() throws InterruptedException {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        int n = 6;
        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                try {
                    UserService.changePassword("oldPass", "newPass123", r -> latch.countDown());
                } catch (Exception e) {
                    errors.incrementAndGet();
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(0, errors.get(), "Không được có exception khi concurrent changePassword");
    }
}