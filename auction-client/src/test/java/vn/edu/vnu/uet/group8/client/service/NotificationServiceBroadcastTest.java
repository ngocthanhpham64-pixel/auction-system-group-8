package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Broadcast subscription & concurrency tests for {@link NotificationService}.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class NotificationServiceBroadcastTest {

    @BeforeEach
    void reset() {
        SessionManager.clearSession();
    }

    // ══════════════════════════════════════════════════════
    // loadAll — auth guard
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("loadAll: chưa login → onFailure 'Bạn cần đăng nhập'")
    void loadAll_notLoggedIn_callsOnFailure() {

        AtomicReference<String> error = new AtomicReference<>();

        NotificationService.loadAll(
                list -> fail("Không được gọi onSuccess"),
                error::set
        );

        assertEquals("Bạn cần đăng nhập", error.get());
    }

    @Test
    @DisplayName("loadAll: callbacks null → không crash")
    void loadAll_nullCallbacks_noCrash() {

        assertDoesNotThrow(() ->
                NotificationService.loadAll(null, null));
    }

    // ══════════════════════════════════════════════════════
    // markRead — boundary notificationId
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, -999, Integer.MIN_VALUE})
    @DisplayName("markRead: notificationId <= 0, chưa login → onFailure")
    void markRead_nonPositiveId_notLoggedIn_callsOnFailure(int id) {

        AtomicReference<String> error = new AtomicReference<>();

        NotificationService.markRead(
                id,
                () -> {},
                error::set
        );

        assertNotNull(error.get());
    }

    @Test
    @DisplayName("markRead: notificationId MAX_VALUE, đã login → không crash")
    void markRead_maxIntId_loggedIn_noCrash() {

        SessionManager.setSession("t", 1, "u", "U", "MEMBER");

        assertDoesNotThrow(() ->
                NotificationService.markRead(
                        Integer.MAX_VALUE,
                        () -> {},
                        err -> {}
                ));
    }

    // ══════════════════════════════════════════════════════
    // deleteNotification — boundary
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, Integer.MIN_VALUE})
    @DisplayName("deleteNotification: id <= 0 → onFailure")
    void deleteNotification_nonPositiveId_notLoggedIn_callsOnFailure(int id) {

        AtomicReference<String> error = new AtomicReference<>();

        NotificationService.deleteNotification(
                id,
                () -> {},
                error::set
        );

        assertNotNull(error.get());
    }

    @Test
    @DisplayName("deleteNotification: callbacks null → không crash")
    void deleteNotification_bothNull_noCrash() {

        assertDoesNotThrow(() ->
                NotificationService.deleteNotification(
                        1,
                        null,
                        null
                ));
    }

    // ══════════════════════════════════════════════════════
    // subscribePush
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("subscribePush 5 listeners → wrappers khác nhau")
    void subscribePush_5listeners_allDistinct() {

        List<Consumer<ServerResponse>> wrappers = new ArrayList<>();

        for (int i = 0; i < 5; i++) {

            Consumer<ServerResponse> w =
                    NotificationService.subscribePush(n -> {});

            assertNotNull(w);

            wrappers.add(w);
        }

        long distinct = wrappers.stream().distinct().count();

        assertEquals(5, distinct);

        wrappers.forEach(NotificationService::unsubscribePush);
    }

    // ══════════════════════════════════════════════════════
    // concurrent subscribe/unsubscribe
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("subscribePush/unsubscribePush concurrent → không crash")
    void subscribePush_concurrent10_noCrash()
            throws InterruptedException {

        int n = 10;

        AtomicInteger errors = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {

            new Thread(() -> {

                try {

                    Consumer<ServerResponse> w =
                            NotificationService.subscribePush(notif -> {});

                    NotificationService.unsubscribePush(w);

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

    // ══════════════════════════════════════════════════════
    // concurrent markRead
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("markRead concurrent chưa login → tất cả onFailure được gọi")
    void markRead_concurrent8NotLoggedIn_allFailureCalled()
            throws InterruptedException {

        int n = 8;

        CountDownLatch latch = new CountDownLatch(n);

        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 1; i <= n; i++) {

            final int id = i;

            new Thread(() ->

                    NotificationService.markRead(
                            id,
                            () -> latch.countDown(),
                            err -> {
                                failCount.incrementAndGet();
                                latch.countDown();
                            })

            ).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals(n, failCount.get());
    }

    // ══════════════════════════════════════════════════════
    // auth message consistency
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("loadAll, markRead, deleteNotification cùng auth message")
    void allOperations_authMessage_consistent() {

        String expected = "Bạn cần đăng nhập";

        AtomicReference<String> e1 = new AtomicReference<>();
        AtomicReference<String> e2 = new AtomicReference<>();
        AtomicReference<String> e3 = new AtomicReference<>();

        NotificationService.loadAll(l -> {}, e1::set);

        NotificationService.markRead(
                1,
                () -> {},
                e2::set
        );

        NotificationService.deleteNotification(
                1,
                () -> {},
                e3::set
        );

        assertEquals(expected, e1.get());

        assertEquals(expected, e2.get());

        assertEquals(expected, e3.get());
    }
}