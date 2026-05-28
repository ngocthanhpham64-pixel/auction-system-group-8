package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.response.BidResponse;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boundary & concurrency tests for {@link BidService}.
 * Tập trung: giá trị biên, nhiều thread gọi cùng lúc, callback null-safe.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class BidServiceBoundaryTest {

    @BeforeEach
    void setup() { SessionManager.clearSession(); }

    // ══════════════════════════════════════════════════════
    // Boundary amounts
    // ══════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01", "-999999999"})
    @DisplayName("placeBid: amount <= 0 → thất bại (boundary)")
    void placeBid_nonPositiveAmounts_fail(String amountStr) {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        BigDecimal amount = new BigDecimal(amountStr);
        AtomicReference<BidResponse> result = new AtomicReference<>();
        BidService.placeBid(1, amount, result::set);
        assertNotNull(result.get());
        assertFalse(result.get().isSuccess(),
                "amount=" + amountStr + " phải thất bại");
    }

    @Test
    @DisplayName("placeBid: amount = 0.01 (nhỏ nhất dương) → thất bại khi không kết nối")
    void placeBid_minPositiveDecimal_failsNoConnection() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<BidResponse> result = new AtomicReference<>();
        BidService.placeBid(1, new BigDecimal("0.01"), result::set);
        assertNotNull(result.get());
        assertFalse(result.get().isSuccess());
    }

    @Test
    @DisplayName("placeBid: amount = Long.MAX_VALUE → không crash (không kết nối)")
    void placeBid_veryLargeAmount_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                BidService.placeBid(1, new BigDecimal(Long.MAX_VALUE), resp -> {}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    @DisplayName("setAutoBid: maxAmount <= 0 → onResult(false) (boundary)")
    void setAutoBid_nonPositiveMaxAmount_returnsFalse(String amountStr) {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        BigDecimal amount = new BigDecimal(amountStr);
        AtomicReference<Boolean> result = new AtomicReference<>(true);
        BidService.setAutoBid(1, amount, result::set);
        assertFalse(result.get(), "maxAmount=" + amountStr + " phải false");
    }

    // ══════════════════════════════════════════════════════
    // itemId boundary
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("placeBid: itemId = 0 → không crash")
    void placeBid_zeroItemId_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                BidService.placeBid(0, BigDecimal.valueOf(500_000), resp -> {}));
    }

    @Test
    @DisplayName("placeBid: itemId âm → không crash")
    void placeBid_negativeItemId_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                BidService.placeBid(-1, BigDecimal.valueOf(500_000), resp -> {}));
    }

    @Test
    @DisplayName("loadHistory: itemId âm → không crash")
    void loadHistory_negativeItemId_noCrash() {
        assertDoesNotThrow(() -> BidService.loadHistory(-1, list -> {}));
    }

    // ══════════════════════════════════════════════════════
    // Callback null-safe
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("placeBid: callback null, chưa login → không crash")
    void placeBid_nullCallback_notLoggedIn_noCrash() {
        assertDoesNotThrow(() ->
                BidService.placeBid(1, BigDecimal.valueOf(100_000), null));
    }

    @Test
    @DisplayName("placeBid: callback null, đã login, không kết nối → không crash")
    void placeBid_nullCallback_loggedIn_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                BidService.placeBid(1, BigDecimal.valueOf(100_000), null));
    }

    @Test
    @DisplayName("setAutoBid: callback null → không crash")
    void setAutoBid_nullCallback_noCrash() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        assertDoesNotThrow(() ->
                BidService.setAutoBid(1, BigDecimal.valueOf(1_000_000), null));
    }

    @Test
    @DisplayName("loadHistory: callback null → không crash")
    void loadHistory_nullCallback_noCrash() {
        assertDoesNotThrow(() -> BidService.loadHistory(1, null));
    }

    // ══════════════════════════════════════════════════════
    // Response not null
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("placeBid response: message không null khi thất bại do auth")
    void placeBid_notLoggedIn_responseMessageNotNull() {
        AtomicReference<BidResponse> result = new AtomicReference<>();
        BidService.placeBid(1, BigDecimal.valueOf(500_000), result::set);
        assertNotNull(result.get());
        assertNotNull(result.get().getMessage());
        assertFalse(result.get().getMessage().isBlank());
    }

    @Test
    @DisplayName("placeBid response: message không null khi amount null")
    void placeBid_nullAmount_responseMessageNotNull() {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        AtomicReference<BidResponse> result = new AtomicReference<>();
        BidService.placeBid(1, null, result::set);
        assertNotNull(result.get().getMessage());
    }

    // ══════════════════════════════════════════════════════
    // Concurrency
    // ══════════════════════════════════════════════════════

    @Test
    @DisplayName("placeBid: 10 thread gọi đồng thời → không deadlock, mọi callback được gọi")
    void placeBid_concurrent10Threads_allCallbacksCalled() throws InterruptedException {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger callbackCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> BidService.placeBid(idx + 1, BigDecimal.valueOf(500_000), resp -> {
                callbackCount.incrementAndGet();
                latch.countDown();
            })).start();
        }

        boolean done = latch.await(5, TimeUnit.SECONDS);
        assertTrue(done, "Tất cả " + threadCount + " callback phải được gọi trong 5s");
        assertEquals(threadCount, callbackCount.get());
    }

    @Test
    @DisplayName("setAutoBid: 5 thread đồng thời → không crash")
    void setAutoBid_concurrent5Threads_noCrash() throws InterruptedException {
        SessionManager.setSession("t", 1, "u", "U", "MEMBER");
        CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                BidService.setAutoBid(1, BigDecimal.valueOf(1_000_000), r -> latch.countDown());
            }).start();
        }
        boolean done = latch.await(5, TimeUnit.SECONDS);
        assertTrue(done);
    }
}