package vn.edu.vnu.uet.group8.client.networking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.EventType;
import vn.edu.vnu.uet.group8.client.TestFXSetup;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResponseDispatcherTest
 *
 * Test các chức năng:
 * - register / unregister callback theo requestId
 * - subscribe / unsubscribe broadcast listener
 * - dispatch null-safe
 * - dispatch khi không có listener
 *
 * Không cần JavaFX toolkit.
 */
@DisplayName("ResponseDispatcher")
class ResponseDispatcherTest extends TestFXSetup {

    // =========================================================
    // Helper methods
    // =========================================================

    private static ServerResponse replyResponse(String requestId) {
        return ServerResponse.reply("TEST_ACTION", requestId)
                .success(true)
                .build();
    }

    private static ServerResponse broadcastResponse(EventType eventType) {
        return ServerResponse.broadcast(eventType)
                .success(true)
                .build();
    }

    // =========================================================
    // register / unregister
    // =========================================================

    @Nested
    @DisplayName("register / unregister")
    class RegisterUnregister {

        @Test
        @DisplayName("register null requestId không ném exception")
        void registerNullRequestId() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.register(null, r -> {})
            );
        }

        @Test
        @DisplayName("register null callback không ném exception")
        void registerNullCallback() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.register("req-1", null)
            );
        }

        @Test
        @DisplayName("unregister null không ném exception")
        void unregisterNull() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.unregister(null)
            );
        }

        @Test
        @DisplayName("unregister requestId không tồn tại không ném exception")
        void unregisterUnknownId() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.unregister("unknown-id")
            );
        }

        @Test
        @DisplayName("register rồi unregister thành công")
        void registerThenUnregister() {
            assertDoesNotThrow(() -> {
                ResponseDispatcher.register("req-123", r -> {});
                ResponseDispatcher.unregister("req-123");
            });
        }
    }

    // =========================================================
    // subscribe / unsubscribe
    // =========================================================

    @Nested
    @DisplayName("subscribe / unsubscribe")
    class SubscribeUnsubscribe {

        @Test
        @DisplayName("subscribe null eventType không ném exception")
        void subscribeNullEventType() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.subscribe(null, r -> {})
            );
        }

        @Test
        @DisplayName("subscribe null listener không ném exception")
        void subscribeNullListener() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.subscribe(EventType.NOTIFICATION, null)
            );
        }

        @Test
        @DisplayName("unsubscribe null eventType không ném exception")
        void unsubscribeNullEventType() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.unsubscribe(null, r -> {})
            );
        }

        @Test
        @DisplayName("unsubscribe null listener không ném exception")
        void unsubscribeNullListener() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.unsubscribe(EventType.NOTIFICATION, null)
            );
        }

        @Test
        @DisplayName("subscribe rồi unsubscribe listener thành công")
        void subscribeThenUnsubscribe() {

            Consumer<ServerResponse> listener = r -> {};

            assertDoesNotThrow(() -> {
                ResponseDispatcher.subscribe(EventType.PRICE_UPDATE, listener);
                ResponseDispatcher.unsubscribe(EventType.PRICE_UPDATE, listener);
            });
        }

        @Test
        @DisplayName("unsubscribe listener chưa subscribe không ném exception")
        void unsubscribeUnknownListener() {

            Consumer<ServerResponse> listener = r -> {};

            assertDoesNotThrow(() ->
                    ResponseDispatcher.unsubscribe(EventType.AUCTION_ENDED, listener)
            );
        }
    }

    // =========================================================
    // dispatch null-safe
    // =========================================================

    @Nested
    @DisplayName("dispatch null-safe")
    class DispatchNullSafe {

        @Test
        @DisplayName("dispatch null không ném exception")
        void dispatchNull() {
            assertDoesNotThrow(() ->
                    ResponseDispatcher.dispatch(null)
            );
        }

        @Test
        @DisplayName("dispatch response không có requestId và eventType")
        void dispatchInvalidResponse() {

            ServerResponse invalid =
                    ServerResponse.reply("TEST", "req-1")
                            .success(true)
                            .build();

            assertDoesNotThrow(() ->
                    ResponseDispatcher.dispatch(invalid)
            );
        }
    }

    // =========================================================
    // dispatch no listener
    // =========================================================

    @Nested
    @DisplayName("dispatch no listener")
    class DispatchNoListener {

        @Test
        @DisplayName("dispatch broadcast không listener không crash")
        void dispatchBroadcastNoListener() {

            ServerResponse response =
                    broadcastResponse(EventType.PRICE_UPDATE);

            assertDoesNotThrow(() ->
                    ResponseDispatcher.dispatch(response)
            );
        }

        @Test
        @DisplayName("dispatch reply không callback không crash")
        void dispatchReplyNoCallback() {

            ServerResponse response =
                    replyResponse("unknown-request-id");

            assertDoesNotThrow(() ->
                    ResponseDispatcher.dispatch(response)
            );
        }
    }

    // =========================================================
    // multiple subscribers
    // =========================================================

    @Nested
    @DisplayName("multiple subscribers")
    class MultipleSubscribers {

        @Test
        @DisplayName("2 listener khác nhau subscribe cùng event")
        void multipleListeners() {

            Consumer<ServerResponse> l1 = r -> {};
            Consumer<ServerResponse> l2 = r -> {};

            assertDoesNotThrow(() -> {
                ResponseDispatcher.subscribe(EventType.NOTIFICATION, l1);
                ResponseDispatcher.subscribe(EventType.NOTIFICATION, l2);

                ResponseDispatcher.unsubscribe(EventType.NOTIFICATION, l1);
                ResponseDispatcher.unsubscribe(EventType.NOTIFICATION, l2);
            });
        }

        @Test
        @DisplayName("unsubscribe 1 listener không ảnh hưởng listener còn lại")
        void unsubscribeOneKeepOther() {

            AtomicInteger counter = new AtomicInteger(0);

            Consumer<ServerResponse> l1 =
                    r -> counter.incrementAndGet();

            Consumer<ServerResponse> l2 =
                    r -> counter.incrementAndGet();

            ResponseDispatcher.subscribe(EventType.AUCTION_ENDED, l1);
            ResponseDispatcher.subscribe(EventType.AUCTION_ENDED, l2);

            ResponseDispatcher.unsubscribe(EventType.AUCTION_ENDED, l1);

            ServerResponse response =
                    broadcastResponse(EventType.AUCTION_ENDED);

            assertDoesNotThrow(() ->
                    ResponseDispatcher.dispatch(response)
            );

            ResponseDispatcher.unsubscribe(EventType.AUCTION_ENDED, l2);
        }
    }
}