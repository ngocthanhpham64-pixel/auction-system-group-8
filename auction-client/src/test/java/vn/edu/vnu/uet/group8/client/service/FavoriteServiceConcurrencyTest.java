package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.client.util.SessionManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boundary & concurrency tests for {@link FavoriteService}.
 *
 * Kiểm tra:
 * - Auth guard
 * - Offline guard
 * - itemId boundary
 * - callback null-safe
 * - concurrent add/remove
 * - message consistency
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class FavoriteServiceConcurrencyTest {

    private static final String AUTH_MSG = "Bạn cần đăng nhập";

    @BeforeEach
    void reset() {
        SessionManager.clearSession();
    }

    // =====================================================
    // loadAll
    // =====================================================

    @Test
    @DisplayName("loadAll: chưa login → onFailure auth message")
    void loadAll_notLoggedIn_callsOnFailure() {
        AtomicReference<String> error = new AtomicReference<>();

        FavoriteService.loadAll(
                items -> fail("Không được gọi onSuccess"),
                error::set
        );

        assertEquals(AUTH_MSG, error.get());
    }

    @Test
    @DisplayName("loadAll: đã login nhưng offline → onFailure không null")
    void loadAll_loggedIn_offline_failureNotNull() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");

        AtomicReference<String> error = new AtomicReference<>();

        FavoriteService.loadAll(
                items -> fail("Không được gọi onSuccess"),
                error::set
        );

        assertNotNull(error.get());
        assertFalse(error.get().isBlank());
    }

    @Test
    @DisplayName("loadAll: callbacks null → không crash")
    void loadAll_nullCallbacks_noCrash() {
        assertDoesNotThrow(() ->
                FavoriteService.loadAll(null, null));
    }

    // =====================================================
    // add
    // =====================================================

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -999, Integer.MIN_VALUE})
    @DisplayName("add: invalid itemId + chưa login → auth failure")
    void add_invalidItemId_notLoggedIn_authFailure(int itemId) {
        AtomicReference<String> error = new AtomicReference<>();

        FavoriteService.add(itemId, () -> {}, error::set);

        assertEquals(AUTH_MSG, error.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("add: invalid itemId + loggedIn offline → không crash")
    void add_invalidItemId_loggedIn_offline_noCrash(int itemId) {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");

        assertDoesNotThrow(() ->
                FavoriteService.add(itemId, () -> {}, err -> {}));
    }

    @Test
    @DisplayName("add: MAX_VALUE itemId → không crash")
    void add_maxValue_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");

        assertDoesNotThrow(() ->
                FavoriteService.add(Integer.MAX_VALUE, () -> {}, err -> {}));
    }

    @Test
    @DisplayName("add: callbacks null → không crash")
    void add_nullCallbacks_noCrash() {
        assertDoesNotThrow(() ->
                FavoriteService.add(1, null, null));
    }

    @Test
    @DisplayName("add: failure message không chứa stack trace")
    void add_failureMessage_notStackTrace() {
        AtomicReference<String> error = new AtomicReference<>();

        FavoriteService.add(1, () -> {}, error::set);

        if (error.get() != null) {
            assertFalse(error.get().contains("at "));
        }
    }

    @Test
    @DisplayName("add: chưa login → auth message chuẩn")
    void add_authMessage_consistent() {
        AtomicReference<String> error = new AtomicReference<>();

        FavoriteService.add(1, () -> {}, error::set);

        assertEquals(AUTH_MSG, error.get());
    }

    // =====================================================
    // remove
    // =====================================================

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -999})
    @DisplayName("remove: invalid itemId + chưa login → auth failure")
    void remove_invalidItemId_notLoggedIn_authFailure(int itemId) {
        AtomicReference<String> error = new AtomicReference<>();

        FavoriteService.remove(itemId, () -> {}, error::set);

        assertEquals(AUTH_MSG, error.get());
    }

    @Test
    @DisplayName("remove: loggedIn offline → không crash")
    void remove_loggedIn_offline_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");

        assertDoesNotThrow(() ->
                FavoriteService.remove(99, () -> {}, err -> {}));
    }

    @Test
    @DisplayName("remove: callbacks null → không crash")
    void remove_nullCallbacks_noCrash() {
        assertDoesNotThrow(() ->
                FavoriteService.remove(1, null, null));
    }

    @Test
    @DisplayName("remove: auth message chuẩn")
    void remove_authMessage_consistent() {
        AtomicReference<String> error = new AtomicReference<>();

        FavoriteService.remove(1, () -> {}, error::set);

        assertEquals(AUTH_MSG, error.get());
    }

    @Test
    @DisplayName("add/remove auth message nhất quán")
    void addRemove_authMessage_same() {
        AtomicReference<String> addErr = new AtomicReference<>();
        AtomicReference<String> removeErr = new AtomicReference<>();

        FavoriteService.add(1, () -> {}, addErr::set);
        FavoriteService.remove(1, () -> {}, removeErr::set);

        assertEquals(addErr.get(), removeErr.get());
    }

    // =====================================================
    // concurrency
    // =====================================================

    @Test
    @DisplayName("concurrent add 10 threads → không exception")
    void add_concurrent10_noException() throws InterruptedException {
        int n = 10;

        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            final int itemId = i + 1;

            new Thread(() -> {
                try {
                    FavoriteService.add(
                            itemId,
                            () -> {},
                            err -> {}
                    );
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(0, errors.get());
    }

    @Test
    @DisplayName("concurrent remove 8 threads → không exception")
    void remove_concurrent8_noException() throws InterruptedException {
        int n = 8;

        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            final int itemId = i + 1;

            new Thread(() -> {
                try {
                    FavoriteService.remove(
                            itemId,
                            () -> {},
                            err -> {}
                    );
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(0, errors.get());
    }

    @Test
    @DisplayName("interleaved add/remove nhiều thread → không deadlock")
    void addRemove_interleaved_noDeadlock() throws InterruptedException {
        int n = 12;

        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {

            final boolean add = i % 2 == 0;
            final int itemId = i + 1;

            new Thread(() -> {
                try {

                    if (add) {
                        FavoriteService.add(itemId, () -> {}, err -> {});
                    } else {
                        FavoriteService.remove(itemId, () -> {}, err -> {});
                    }

                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // =====================================================
    // consistency
    // =====================================================

    @Test
    @DisplayName("auth message nhất quán giữa mọi operation")
    void allOperations_authMessage_consistent() {

        AtomicReference<String> e1 = new AtomicReference<>();
        AtomicReference<String> e2 = new AtomicReference<>();
        AtomicReference<String> e3 = new AtomicReference<>();

        FavoriteService.loadAll(l -> {}, e1::set);
        FavoriteService.add(1, () -> {}, e2::set);
        FavoriteService.remove(1, () -> {}, e3::set);

        assertEquals(AUTH_MSG, e1.get());
        assertEquals(AUTH_MSG, e2.get());
        assertEquals(AUTH_MSG, e3.get());
    }
}