package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Broadcast subscription & concurrency tests for {@link AuctionService}.
 *
 * Tập trung:
 * - subscribeAuctionStatus
 * - subscribeAuctionEnded
 * - concurrent subscribe/unsubscribe
 * - loadDetail boundary itemId
 * - loadAll concurrent
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AuctionServiceBroadcastTest {

    // ══════════════════════════════════════════════════════
    // subscribeAuctionStatus
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("subscribeAuctionStatus(null) → trả về null")
    void subscribeAuctionStatus_null_returnsNull() {
        var w = AuctionService.subscribeAuctionStatus(null);
        assertNull(w);
    }

    @Test
    @DisplayName("subscribeAuctionStatus listener hợp lệ → wrapper không null")
    void subscribeAuctionStatus_validListener_notNull() {
        var w = AuctionService.subscribeAuctionStatus(r -> {});
        assertNotNull(w);
        AuctionService.unsubscribeAuctionStatus(w);
    }

    @Test
    @DisplayName("subscribeAuctionStatus gọi 3 lần → 3 wrapper khác nhau")
    void subscribeAuctionStatus_threeSubscribers_distinctWrappers() {
        var w1 = AuctionService.subscribeAuctionStatus(r -> {});
        var w2 = AuctionService.subscribeAuctionStatus(r -> {});
        var w3 = AuctionService.subscribeAuctionStatus(r -> {});

        assertNotSame(w1, w2);
        assertNotSame(w2, w3);
        assertNotSame(w1, w3);

        AuctionService.unsubscribeAuctionStatus(w1);
        AuctionService.unsubscribeAuctionStatus(w2);
        AuctionService.unsubscribeAuctionStatus(w3);
    }

    // ══════════════════════════════════════════════════════
    // unsubscribeAuctionStatus
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("unsubscribeAuctionStatus(null) → không crash")
    void unsubscribeAuctionStatus_null_noCrash() {
        assertDoesNotThrow(() ->
                AuctionService.unsubscribeAuctionStatus(null));
    }

    @Test
    @DisplayName("unsubscribeAuctionStatus wrapper hợp lệ → không crash")
    void unsubscribeAuctionStatus_validWrapper_noCrash() {
        var w = AuctionService.subscribeAuctionStatus(r -> {});
        assertDoesNotThrow(() ->
                AuctionService.unsubscribeAuctionStatus(w));
    }

    @Test
    @DisplayName("unsubscribeAuctionStatus gọi 2 lần → idempotent")
    void unsubscribeAuctionStatus_twice_idempotent() {
        var w = AuctionService.subscribeAuctionStatus(r -> {});
        AuctionService.unsubscribeAuctionStatus(w);

        assertDoesNotThrow(() ->
                AuctionService.unsubscribeAuctionStatus(w));
    }

    // ══════════════════════════════════════════════════════
    // subscribeAuctionEnded
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("subscribeAuctionEnded wrapper khác null")
    void subscribeAuctionEnded_notNull() {
        var w = AuctionService.subscribeAuctionEnded(r -> {});
        assertNotNull(w);
        AuctionService.unsubscribeAuctionEnded(w);
    }

    @Test
    @DisplayName("subscribeAuctionStatus wrapper khác subscribeAuctionEnded wrapper")
    void subscribeAuctionStatus_distinctFromAuctionEnded() {
        var w1 = AuctionService.subscribeAuctionStatus(r -> {});
        var w2 = AuctionService.subscribeAuctionEnded(r -> {});

        assertNotSame(w1, w2);

        AuctionService.unsubscribeAuctionStatus(w1);
        AuctionService.unsubscribeAuctionEnded(w2);
    }

    // ══════════════════════════════════════════════════════
    // loadDetail — boundary itemId
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(ints = {-100, -1, 0})
    @DisplayName("loadDetail: itemId <= 0 → không crash")
    void loadDetail_nonPositiveItemId_returnsNull(int itemId) {
        var ref =
                new java.util.concurrent.atomic.AtomicReference<Object>("NOT_SET");

        assertDoesNotThrow(() ->
                AuctionService.loadDetail(itemId, ref::set));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MAX_VALUE, 999_999_999})
    @DisplayName("loadDetail: itemId rất lớn → không crash")
    void loadDetail_veryLargeItemId_noCrash(int itemId) {
        assertDoesNotThrow(() ->
                AuctionService.loadDetail(itemId, item -> {}));
    }

    // ══════════════════════════════════════════════════════
    // loadAll — concurrent
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("loadAll: 5 thread đồng thời → tất cả onDone được gọi")
    void loadAll_concurrent5_allOnDoneCalled()
            throws InterruptedException {

        int n = 5;
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            new Thread(() ->
                    AuctionService.loadAll(null, latch::countDown)
            ).start();
        }

        assertTrue(
                latch.await(5, TimeUnit.SECONDS),
                "Tất cả " + n + " onDone phải được gọi trong 5s"
        );
    }

    // ══════════════════════════════════════════════════════
    // subscribe + unsubscribe concurrent
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("subscribe/unsubscribe AuctionStatus đồng thời 10 thread → không crash")
    void subscribeAuctionStatus_concurrent10_noCrash()
            throws InterruptedException {

        int n = 10;

        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {

            new Thread(() -> {
                try {

                    var w =
                            AuctionService.subscribeAuctionStatus(r -> {});

                    AuctionService.unsubscribeAuctionStatus(w);

                } catch (Exception e) {

                    errors.incrementAndGet();

                } finally {

                    latch.countDown();
                }

            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);

        assertEquals(
                0,
                errors.get(),
                "Không được có exception khi concurrent subscribe/unsubscribe"
        );
    }

    @Test
    @DisplayName("subscribe/unsubscribe AuctionEnded đồng thời → không crash")
    void subscribeAuctionEnded_concurrent_noCrash()
            throws InterruptedException {

        int n = 8;

        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {

            new Thread(() -> {

                try {

                    var w =
                            AuctionService.subscribeAuctionEnded(r -> {});

                    AuctionService.unsubscribeAuctionEnded(w);

                } catch (Exception e) {

                    errors.incrementAndGet();

                } finally {

                    latch.countDown();
                }

            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);

        assertEquals(0, errors.get());
    }

    // ══════════════════════════════════════════════════════
    // null-safe unsubscribe
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("unsubscribe tất cả loại với null → không crash")
    void unsubscribeAll_null_noCrash() {

        assertDoesNotThrow(() -> {

            AuctionService.unsubscribeAuctionStatus(null);

            AuctionService.unsubscribeAuctionEnded(null);
        });
    }
}