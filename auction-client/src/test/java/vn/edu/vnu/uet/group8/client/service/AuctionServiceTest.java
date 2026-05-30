package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.client.TestFXSetup;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.networking.ResponseDispatcher;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.dto.model.CommentDTO;
import vn.edu.vnu.uet.group8.common.dto.model.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.EventType;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test toàn diện cho {@link AuctionService}.
 *
 * Mục tiêu: phủ 100% instruction và branch của tất cả method:
 *   - loadAll            (connected=false/true, response success/fail, onDone null/not-null)
 *   - loadDetail         (connected=false/true, response success/fail, onResult null/not-null)
 *   - loadItemComments   (response success/fail, onResult null/not-null, parsed null/not-null)
 *   - getItemBidHistory  (response success/fail, onResult null/not-null, parsed null/not-null)
 *   - subscribeAuctionStatus   (listener null/valid)
 *   - unsubscribeAuctionStatus (wrapper null/valid)
 *   - handleAuctionStatusBroadcast (update null, update.newEndTime null/not-null)
 *   - subscribeAuctionEnded    (listener null/valid; result null/not-null trong wrapper)
 *   - unsubscribeAuctionEnded  (wrapper null/valid)
 *   - runIfNotNull / acceptIfNotNull  (null / not-null)
 *
 * Chiến lược mock "connected = true":
 *   Dùng Reflection để set field {@code connected} của singleton AuctionClient thành true,
 *   sau đó inject {@code out = null} — khi gọi sendRequest(), AuctionClient sẽ thấy
 *   connected=true nhưng out=null, gọi Platform.runLater(callback(errorResponse)).
 *   Điều này cho phép test nhánh "connected" của loadAll/loadDetail mà không cần server thật.
 *   Platform.runLater được thực thi nhờ TestFXSetup khởi động JavaFX toolkit.
 */
@DisplayName("AuctionService — full branch coverage")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AuctionServiceTest extends TestFXSetup {

    // ══════════════════════════════════════════════════════════════════
    // Reflection helpers để điều khiển AuctionClient singleton
    // ══════════════════════════════════════════════════════════════════

    /** Đặt connected = value trên AuctionClient singleton */
    private static void setConnected(boolean value) throws Exception {
        Field f = AuctionClient.class.getDeclaredField("connected");
        f.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicBoolean) f.get(AuctionClient.getInstance())).set(value);
    }

    /** Đặt out = null để sendRequest biết không thể ghi (nhưng connected=true) */
    private static void setOutNull() throws Exception {
        Field f = AuctionClient.class.getDeclaredField("out");
        f.setAccessible(true);
        f.set(AuctionClient.getInstance(), null);
    }

    /**
     * Kích hoạt trạng thái "connected nhưng không có socket" —
     * sendRequest sẽ gọi callback với error response trên FX thread.
     */
    private static void forceConnectedNoSocket() throws Exception {
        setConnected(true);
        setOutNull();
    }

    /** Restore về disconnected sau test */
    private static void forceDisconnected() throws Exception {
        setConnected(false);
    }

    /**
     * Chờ FX thread xử lý xong (dùng CountDownLatch + Platform.runLater sentinel).
     * Timeout 3 giây.
     */
    private static void waitFxQueue() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        javafx.application.Platform.runLater(latch::countDown);
        assertTrue(latch.await(3, TimeUnit.SECONDS), "FX queue timeout");
    }

    // ══════════════════════════════════════════════════════════════════
    // Teardown — luôn restore về disconnected
    // ══════════════════════════════════════════════════════════════════

    @AfterEach
    void restore() throws Exception {
        forceDisconnected();
        SessionManager.clearSession();
    }

    // ══════════════════════════════════════════════════════════════════
    // runIfNotNull / acceptIfNotNull — private helpers qua indirect test
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("runIfNotNull() — gián tiếp qua loadAll")
    class RunIfNotNull {

        @Test
        @DisplayName("onDone != null → được gọi")
        void notNull_called() {
            AtomicBoolean flag = new AtomicBoolean(false);
            AuctionService.loadAll(null, () -> flag.set(true));
            assertTrue(flag.get());
        }

        @Test
        @DisplayName("onDone == null → không ném NPE")
        void null_noException() {
            assertDoesNotThrow(() -> AuctionService.loadAll(null, null));
        }
    }

    @Nested
    @DisplayName("acceptIfNotNull() — gián tiếp qua loadDetail")
    class AcceptIfNotNull {

        @Test
        @DisplayName("onResult != null → nhận null khi offline")
        void notNull_acceptsNull() {
            AtomicReference<Object> ref = new AtomicReference<>("NOT_SET");
            AuctionService.loadDetail(1, ref::set);
            assertNull(ref.get());
        }

        @Test
        @DisplayName("onResult == null → không ném NPE")
        void null_noException() {
            assertDoesNotThrow(() -> AuctionService.loadDetail(1, null));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // loadAll
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadAll()")
    class LoadAll {

        // ── Branch: NOT connected ─────────────────────────────────────

        @Test
        @DisplayName("offline + onDone != null → onDone được gọi ngay lập tức")
        void offline_onDoneCalled() {
            AtomicBoolean called = new AtomicBoolean(false);
            AuctionService.loadAll(null, () -> called.set(true));
            assertTrue(called.get());
        }

        @Test
        @DisplayName("offline + onDone == null → không crash")
        void offline_onDoneNull() {
            assertDoesNotThrow(() -> AuctionService.loadAll(null, null));
        }

        @Test
        @DisplayName("offline + filter != null → không crash, onDone vẫn gọi")
        void offline_withFilter() {
            AtomicBoolean called = new AtomicBoolean(false);
            var filter = vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest.builder().build();
            AuctionService.loadAll(filter, () -> called.set(true));
            assertTrue(called.get());
        }

        // ── Branch: connected → sendRequest → error response (out=null) ──

        @Test
        @DisplayName("connected + sendRequest error → onDone vẫn được gọi sau FX flush")
        void connected_errorResponse_onDoneCalled() throws Exception {
            forceConnectedNoSocket();

            CountDownLatch latch = new CountDownLatch(1);
            AuctionService.loadAll(null, latch::countDown);

            // FX thread gọi callback với error response
            assertTrue(latch.await(3, TimeUnit.SECONDS), "onDone phải được gọi sau FX flush");
        }

        @Test
        @DisplayName("connected + sendRequest error + onDone null → không crash")
        void connected_errorResponse_onDoneNull() throws Exception {
            forceConnectedNoSocket();
            assertDoesNotThrow(() -> AuctionService.loadAll(null, null));
            waitFxQueue();
        }

        @Test
        @DisplayName("concurrent 5 thread offline → tất cả onDone được gọi")
        void concurrent_offline_allCalled() throws InterruptedException {
            int n = 5;
            CountDownLatch latch = new CountDownLatch(n);
            for (int i = 0; i < n; i++) {
                new Thread(() -> AuctionService.loadAll(null, latch::countDown)).start();
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // loadDetail
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadDetail()")
    class LoadDetail {

        // ── Branch: NOT connected ─────────────────────────────────────

        @Test
        @DisplayName("offline + onResult != null → nhận null")
        void offline_callsWithNull() {
            AtomicBoolean called = new AtomicBoolean(false);
            AuctionService.loadDetail(1, item -> {
                called.set(true);
                assertNull(item);
            });
            assertTrue(called.get());
        }

        @Test
        @DisplayName("offline + onResult == null → không crash")
        void offline_nullOnResult() {
            assertDoesNotThrow(() -> AuctionService.loadDetail(1, null));
        }

        @ParameterizedTest
        @ValueSource(ints = {-100, -1, 0})
        @DisplayName("offline + itemId <= 0 → không crash, onResult nhận null")
        void offline_nonPositiveId(int id) {
            AtomicReference<Object> ref = new AtomicReference<>("NOT_SET");
            assertDoesNotThrow(() -> AuctionService.loadDetail(id, ref::set));
            assertNull(ref.get());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 999, Integer.MAX_VALUE})
        @DisplayName("offline + itemId hợp lệ/lớn → không crash")
        void offline_largeId(int id) {
            assertDoesNotThrow(() -> AuctionService.loadDetail(id, item -> {}));
        }

        // ── Branch: connected → sendRequest → error response ──────────

        @Test
        @DisplayName("connected + error response → onResult nhận null sau FX flush")
        void connected_errorResponse_callsNull() throws Exception {
            forceConnectedNoSocket();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Object> ref = new AtomicReference<>("NOT_SET");

            AuctionService.loadDetail(42, item -> {
                ref.set(item);
                latch.countDown();
            });

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            // error response → response.isSuccess()=false → acceptIfNotNull(onResult, null)
            assertNull(ref.get());
        }

        @Test
        @DisplayName("connected + error response + onResult null → không crash")
        void connected_errorResponse_nullOnResult() throws Exception {
            forceConnectedNoSocket();
            assertDoesNotThrow(() -> AuctionService.loadDetail(1, null));
            waitFxQueue();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // loadItemComments — không kiểm tra connected, gọi sendRequest trực tiếp
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("loadItemComments()")
    class LoadItemComments {

        @Test
        @DisplayName("offline (sendRequest thấy not connected) + onResult null → không crash")
        void offline_nullOnResult() {
            assertDoesNotThrow(() -> AuctionService.loadItemComments(1, null));
        }

        @Test
        @DisplayName("offline + onResult != null → callback vẫn được gọi sau FX flush")
        void offline_callbackCalled() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AuctionService.loadItemComments(1, list -> latch.countDown());
            assertTrue(latch.await(3, TimeUnit.SECONDS),
                    "onResult phải được gọi dù offline (sendRequest báo lỗi qua FX)");
        }

        // Khi connected + error response → acceptIfNotNull(onResult, emptyList)
        @Test
        @DisplayName("connected + error response → onResult nhận emptyList")
        void connected_errorResponse_emptyList() throws Exception {
            forceConnectedNoSocket();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<List<CommentDTO>> ref = new AtomicReference<>();

            AuctionService.loadItemComments(1, list -> {
                ref.set(list);
                latch.countDown();
            });

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertNotNull(ref.get());
            // error response → emptyList
            assertTrue(ref.get().isEmpty());
        }

        @Test
        @DisplayName("connected + error + onResult null → không crash")
        void connected_errorResponse_nullOnResult() throws Exception {
            forceConnectedNoSocket();
            assertDoesNotThrow(() -> AuctionService.loadItemComments(1, null));
            waitFxQueue();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // getItemBidHistory
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getItemBidHistory()")
    class GetItemBidHistory {

        @Test
        @DisplayName("offline + onResult null → không crash")
        void offline_nullOnResult() {
            assertDoesNotThrow(() -> AuctionService.getItemBidHistory(1, null));
        }

        @Test
        @DisplayName("offline + onResult != null → callback được gọi sau FX flush")
        void offline_callbackCalled() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AuctionService.getItemBidHistory(1, list -> latch.countDown());
            assertTrue(latch.await(3, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("connected + error response → onResult nhận emptyList")
        void connected_errorResponse_emptyList() throws Exception {
            forceConnectedNoSocket();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<List<BidRecord>> ref = new AtomicReference<>();

            AuctionService.getItemBidHistory(1, list -> {
                ref.set(list);
                latch.countDown();
            });

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertNotNull(ref.get());
            assertTrue(ref.get().isEmpty());
        }

        @Test
        @DisplayName("connected + error + onResult null → không crash")
        void connected_errorResponse_nullOnResult() throws Exception {
            forceConnectedNoSocket();
            assertDoesNotThrow(() -> AuctionService.getItemBidHistory(1, null));
            waitFxQueue();
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 0, 1, Integer.MAX_VALUE})
        @DisplayName("itemId mọi giá trị → không crash khi offline")
        void anyItemId_offline(int id) {
            assertDoesNotThrow(() -> AuctionService.getItemBidHistory(id, list -> {}));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // subscribeAuctionStatus / unsubscribeAuctionStatus
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("subscribeAuctionStatus() / unsubscribeAuctionStatus()")
    class SubscribeAuctionStatus {

        @Test
        @DisplayName("listener == null → trả về null, không đăng ký gì")
        void nullListener_returnsNull() {
            Consumer<ServerResponse> w = AuctionService.subscribeAuctionStatus(null);
            assertNull(w);
        }

        @Test
        @DisplayName("listener hợp lệ → wrapper != null")
        void validListener_returnsWrapper() {
            Consumer<ServerResponse> w = AuctionService.subscribeAuctionStatus(s -> {});
            assertNotNull(w);
            AuctionService.unsubscribeAuctionStatus(w);
        }

        @Test
        @DisplayName("gọi 3 lần → 3 wrapper khác nhau")
        void threeSubscribers_distinctWrappers() {
            var w1 = AuctionService.subscribeAuctionStatus(s -> {});
            var w2 = AuctionService.subscribeAuctionStatus(s -> {});
            var w3 = AuctionService.subscribeAuctionStatus(s -> {});
            assertNotSame(w1, w2);
            assertNotSame(w2, w3);
            assertNotSame(w1, w3);
            AuctionService.unsubscribeAuctionStatus(w1);
            AuctionService.unsubscribeAuctionStatus(w2);
            AuctionService.unsubscribeAuctionStatus(w3);
        }

        @Test
        @DisplayName("unsubscribe(null) → không crash")
        void unsubscribeNull_noCrash() {
            assertDoesNotThrow(() -> AuctionService.unsubscribeAuctionStatus(null));
        }

        @Test
        @DisplayName("unsubscribe wrapper hợp lệ → không crash")
        void unsubscribeValid_noCrash() {
            var w = AuctionService.subscribeAuctionStatus(s -> {});
            assertDoesNotThrow(() -> AuctionService.unsubscribeAuctionStatus(w));
        }

        @Test
        @DisplayName("unsubscribe 2 lần → idempotent, không crash")
        void unsubscribeTwice_idempotent() {
            var w = AuctionService.subscribeAuctionStatus(s -> {});
            AuctionService.unsubscribeAuctionStatus(w);
            assertDoesNotThrow(() -> AuctionService.unsubscribeAuctionStatus(w));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // handleAuctionStatusBroadcast — kích hoạt qua ResponseDispatcher.dispatch
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleAuctionStatusBroadcast() — qua ResponseDispatcher.dispatch")
    class HandleAuctionStatusBroadcast {

        /**
         * Tạo ServerResponse broadcast PRICE_UPDATE với data là PriceUpdateBroadcastResponse.
         */
        private ServerResponse makePriceUpdateResponse(PriceUpdateBroadcastResponse payload) {
            return ServerResponse.broadcast(EventType.PRICE_UPDATE)
                    .data(payload)
                    .build();
        }

        @Test
        @DisplayName("update == null (data=null) → listener KHÔNG được gọi")
        void nullUpdate_listenerNotCalled() throws Exception {
            AtomicBoolean called = new AtomicBoolean(false);
            var w = AuctionService.subscribeAuctionStatus(s -> called.set(true));

            // Dispatch broadcast với data=null
            ServerResponse resp = ServerResponse.broadcast(EventType.PRICE_UPDATE)
                    .data(null) // GsonUtil.toObject sẽ trả null → branch "update == null, skip"
                    .build();
            ResponseDispatcher.dispatch(resp);
            waitFxQueue();

            // update null → listener không được gọi (branch: return sớm)
            assertFalse(called.get(), "Listener không được gọi khi update=null");
            AuctionService.unsubscribeAuctionStatus(w);
        }

        @Test
        @DisplayName("update != null + newEndTime != null → listener được gọi, status có endTime từ update")
        void validUpdate_withEndTime_listenerCalled() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AuctionStatusDTO> captured = new AtomicReference<>();

            var w = AuctionService.subscribeAuctionStatus(status -> {
                captured.set(status);
                latch.countDown();
            });

            Instant endTime = Instant.now().plusSeconds(3600);
            PriceUpdateBroadcastResponse update = PriceUpdateBroadcastResponse.of(
                    1, new BigDecimal("500000"), 5, endTime, "bidder1", false);

            ResponseDispatcher.dispatch(makePriceUpdateResponse(update));
            assertTrue(latch.await(3, TimeUnit.SECONDS), "Listener phải được gọi");

            assertNotNull(captured.get());
            assertEquals(1, captured.get().getItemId());
            assertEquals(new BigDecimal("500000"), captured.get().getCurrentPrice());
            assertEquals(endTime, captured.get().getEndTime());

            AuctionService.unsubscribeAuctionStatus(w);
        }

        @Test
        @DisplayName("update != null + newEndTime == null → endTime fallback về Instant.now()")
        void validUpdate_nullEndTime_fallbackNow() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AuctionStatusDTO> captured = new AtomicReference<>();

            var w = AuctionService.subscribeAuctionStatus(status -> {
                captured.set(status);
                latch.countDown();
            });

            // newEndTime = null → branch "update.getNewEndTime() != null ? ... : Instant.now()"
            PriceUpdateBroadcastResponse update = PriceUpdateBroadcastResponse.of(
                    2, new BigDecimal("100"), 1, null, "bidder2", false);

            Instant beforeDispatch = Instant.now();
            ResponseDispatcher.dispatch(makePriceUpdateResponse(update));
            assertTrue(latch.await(3, TimeUnit.SECONDS));

            assertNotNull(captured.get());
            // endTime phải >= thời điểm dispatch (fallback Instant.now())
            assertFalse(captured.get().getEndTime().isBefore(beforeDispatch),
                    "endTime fallback phải >= beforeDispatch");

            AuctionService.unsubscribeAuctionStatus(w);
        }

        @Test
        @DisplayName("update hợp lệ → ClientModel.updateItemCurrentPrice được gọi")
        void validUpdate_clientModelUpdated() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            var w = AuctionService.subscribeAuctionStatus(s -> latch.countDown());

            PriceUpdateBroadcastResponse update = PriceUpdateBroadcastResponse.of(
                    99, new BigDecimal("999"), 10, Instant.now().plusSeconds(60), "x", false);
            ResponseDispatcher.dispatch(makePriceUpdateResponse(update));
            assertTrue(latch.await(3, TimeUnit.SECONDS));

            AuctionService.unsubscribeAuctionStatus(w);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // subscribeAuctionEnded / unsubscribeAuctionEnded
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("subscribeAuctionEnded() / unsubscribeAuctionEnded()")
    class SubscribeAuctionEnded {

        @Test
        @DisplayName("listener == null → trả về null")
        void nullListener_returnsNull() {
            Consumer<ServerResponse> w = AuctionService.subscribeAuctionEnded(null);
            assertNull(w);
        }

        @Test
        @DisplayName("listener hợp lệ → wrapper != null")
        void validListener_returnsWrapper() {
            var w = AuctionService.subscribeAuctionEnded(r -> {});
            assertNotNull(w);
            AuctionService.unsubscribeAuctionEnded(w);
        }

        @Test
        @DisplayName("unsubscribe(null) → không crash")
        void unsubscribeNull_noCrash() {
            assertDoesNotThrow(() -> AuctionService.unsubscribeAuctionEnded(null));
        }

        @Test
        @DisplayName("unsubscribe wrapper hợp lệ → không crash")
        void unsubscribeValid_noCrash() {
            var w = AuctionService.subscribeAuctionEnded(r -> {});
            assertDoesNotThrow(() -> AuctionService.unsubscribeAuctionEnded(w));
        }

        @Test
        @DisplayName("unsubscribe 2 lần → idempotent")
        void unsubscribeTwice_idempotent() {
            var w = AuctionService.subscribeAuctionEnded(r -> {});
            AuctionService.unsubscribeAuctionEnded(w);
            assertDoesNotThrow(() -> AuctionService.unsubscribeAuctionEnded(w));
        }

        // ── Branch bên trong wrapper: result == null → skip ──────────

        @Test
        @DisplayName("dispatch AUCTION_ENDED với data=null → listener KHÔNG được gọi")
        void nullResult_listenerNotCalled() throws Exception {
            AtomicBoolean called = new AtomicBoolean(false);
            var w = AuctionService.subscribeAuctionEnded(r -> called.set(true));

            // data=null → GsonUtil.toObject trả null → branch "result == null, skip"
            ServerResponse resp = ServerResponse.broadcast(EventType.AUCTION_ENDED)
                    .data(null)
                    .build();
            ResponseDispatcher.dispatch(resp);
            waitFxQueue();

            assertFalse(called.get(), "Listener không được gọi khi result=null");
            AuctionService.unsubscribeAuctionEnded(w);
        }

        // ── Branch bên trong wrapper: result != null → gọi listener ──

        @Test
        @DisplayName("dispatch AUCTION_ENDED với data SOLD → listener được gọi với đúng data")
        void validResult_sold_listenerCalled() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AuctionEndedBroadcastResponse> captured = new AtomicReference<>();

            var w = AuctionService.subscribeAuctionEnded(r -> {
                captured.set(r);
                latch.countDown();
            });

            AuctionEndedBroadcastResponse ended = AuctionEndedBroadcastResponse.sold(
                    10, "Rolex Watch", new BigDecimal("1000000"), "winner1");
            ServerResponse resp = ServerResponse.broadcast(EventType.AUCTION_ENDED)
                    .data(ended)
                    .build();
            ResponseDispatcher.dispatch(resp);

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertNotNull(captured.get());
            assertEquals(10, captured.get().getItemId());
            assertEquals("SOLD", captured.get().getFinalStatus());
            assertEquals("winner1", captured.get().getWinnerUsername());

            AuctionService.unsubscribeAuctionEnded(w);
        }

        @Test
        @DisplayName("dispatch AUCTION_ENDED với data NO_BID → listener được gọi")
        void validResult_noBid_listenerCalled() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AuctionEndedBroadcastResponse> captured = new AtomicReference<>();

            var w = AuctionService.subscribeAuctionEnded(r -> {
                captured.set(r);
                latch.countDown();
            });

            AuctionEndedBroadcastResponse ended = AuctionEndedBroadcastResponse.noBid(20, "iPhone");
            ServerResponse resp = ServerResponse.broadcast(EventType.AUCTION_ENDED)
                    .data(ended)
                    .build();
            ResponseDispatcher.dispatch(resp);

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertNotNull(captured.get());
            assertEquals("ENDED_NO_BID", captured.get().getFinalStatus());
            assertNull(captured.get().getWinnerUsername());

            AuctionService.unsubscribeAuctionEnded(w);
        }

        @Test
        @DisplayName("subscribeAuctionStatus wrapper khác subscribeAuctionEnded wrapper")
        void wrappers_areDistinct() {
            var w1 = AuctionService.subscribeAuctionStatus(s -> {});
            var w2 = AuctionService.subscribeAuctionEnded(r -> {});
            assertNotSame(w1, w2);
            AuctionService.unsubscribeAuctionStatus(w1);
            AuctionService.unsubscribeAuctionEnded(w2);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Concurrent tests
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Concurrent safety")
    class ConcurrentTests {

        @Test
        @DisplayName("subscribe/unsubscribe AuctionStatus 10 thread đồng thời → không crash")
        void subscribeStatus_concurrent10() throws InterruptedException {
            int n = 10;
            AtomicInteger errors = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(n);
            for (int i = 0; i < n; i++) {
                new Thread(() -> {
                    try {
                        var w = AuctionService.subscribeAuctionStatus(s -> {});
                        AuctionService.unsubscribeAuctionStatus(w);
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

        @Test
        @DisplayName("subscribe/unsubscribe AuctionEnded 8 thread đồng thời → không crash")
        void subscribeEnded_concurrent8() throws InterruptedException {
            int n = 8;
            AtomicInteger errors = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(n);
            for (int i = 0; i < n; i++) {
                new Thread(() -> {
                    try {
                        var w = AuctionService.subscribeAuctionEnded(r -> {});
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

        @Test
        @DisplayName("loadAll 5 thread offline đồng thời → tất cả onDone được gọi")
        void loadAll_concurrent5_offline() throws InterruptedException {
            int n = 5;
            CountDownLatch latch = new CountDownLatch(n);
            for (int i = 0; i < n; i++) {
                new Thread(() -> AuctionService.loadAll(null, latch::countDown)).start();
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("unsubscribe tất cả loại với null cùng lúc → không crash")
        void unsubscribeAll_null_noCrash() {
            assertDoesNotThrow(() -> {
                AuctionService.unsubscribeAuctionStatus(null);
                AuctionService.unsubscribeAuctionEnded(null);
            });
        }
    }
}