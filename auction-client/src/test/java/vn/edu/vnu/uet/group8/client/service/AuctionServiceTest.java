package vn.edu.vnu.uet.group8.client.service;

import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.client.TestFXSetup;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionService (client-side validation)")
class AuctionServiceTest extends TestFXSetup {

    @BeforeEach void clear() { SessionManager.clearSession(); }

    @Nested @DisplayName("loadAll – no connection")
    class LoadAll {
        @Test @DisplayName("không kết nối -> onDone vẫn được gọi")
        void noConnectionCallsOnDone() {
            AtomicBoolean called = new AtomicBoolean(false);
            AuctionService.loadAll(null, () -> called.set(true));
            assertTrue(called.get(), "onDone phải được gọi dù không có kết nối");
        }

        @Test @DisplayName("null onDone không ném ngoại lệ")
        void nullOnDoneNoException() {
            assertDoesNotThrow(() -> AuctionService.loadAll(null, null));
        }
    }

    @Nested @DisplayName("loadDetail – no connection")
    class LoadDetail {
        @Test @DisplayName("không kết nối -> onResult nhận null")
        void noConnectionCallsWithNull() {
            AtomicBoolean called = new AtomicBoolean(false);
            AuctionService.loadDetail(1, item -> {
                called.set(true);
                assertNull(item);
            });
            assertTrue(called.get());
        }

        @Test @DisplayName("null onResult không ném ngoại lệ")
        void nullOnResultNoException() {
            assertDoesNotThrow(() -> AuctionService.loadDetail(1, null));
        }
    }

    @Nested @DisplayName("subscribeAuctionStatus")
    class SubscribeAuctionStatus {
        @Test @DisplayName("null listener -> trả về null")
        void nullListenerReturnsNull() {
            Consumer<ServerResponse> wrapper = AuctionService.subscribeAuctionStatus(null);
            assertNull(wrapper);
        }

        @Test @DisplayName("hợp lệ -> wrapper không null")
        void validListenerReturnsWrapper() {
            Consumer<ServerResponse> wrapper = AuctionService.subscribeAuctionStatus(s -> {});
            assertNotNull(wrapper);
            AuctionService.unsubscribeAuctionStatus(wrapper);
        }

        @Test @DisplayName("unsubscribe null không ném ngoại lệ")
        void unsubscribeNullNoException() {
            assertDoesNotThrow(() -> AuctionService.unsubscribeAuctionStatus(null));
        }
    }

    @Nested @DisplayName("subscribeAuctionEnded")
    class SubscribeAuctionEnded {
        @Test @DisplayName("null listener -> trả về null")
        void nullListenerReturnsNull() {
            Consumer<ServerResponse> wrapper = AuctionService.subscribeAuctionEnded(null);
            assertNull(wrapper);
        }

        @Test @DisplayName("hợp lệ -> wrapper không null")
        void validListenerReturnsWrapper() {
            Consumer<ServerResponse> wrapper = AuctionService.subscribeAuctionEnded(e -> {});
            assertNotNull(wrapper);
            AuctionService.unsubscribeAuctionEnded(wrapper);
        }

        @Test @DisplayName("unsubscribe null không ném ngoại lệ")
        void unsubscribeNullNoException() {
            assertDoesNotThrow(() -> AuctionService.unsubscribeAuctionEnded(null));
        }
    }
}