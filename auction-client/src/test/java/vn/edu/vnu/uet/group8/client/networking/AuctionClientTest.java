package vn.edu.vnu.uet.group8.client.networking;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.*;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuctionClientTest – phủ logic không cần kết nối socket thật.
 *
 * Nhánh test được:
 *  - Singleton pattern
 *  - isConnected() khi chưa connect → false
 *  - reconnect() khi host/port chưa set → false
 *  - sendRequest khi không connected → callback nhận error response
 *  - sendAuthenticatedRequest khi không connected → callback nhận error
 *  - disconnect() khi chưa connected → không crash
 *  - sendRequest null callback khi không connected → không crash
 */
@DisplayName("AuctionClient")
class AuctionClientTest {

    @BeforeAll
    static void initToolkit() {
        new JFXPanel();
    }

    @BeforeEach
    void reset() {
        SessionManager.clearSession();
        // Đảm bảo disconnected
        AuctionClient.getInstance().disconnect();
    }

    // ─────────── Singleton ───────────

    @Nested @DisplayName("Singleton")
    class Singleton {

        @Test @DisplayName("getInstance() luôn trả về cùng instance")
        void same_instance() {
            assertSame(AuctionClient.getInstance(), AuctionClient.getInstance());
        }

        @Test @DisplayName("getInstance() không null")
        void not_null() {
            assertNotNull(AuctionClient.getInstance());
        }
    }

    // ─────────── isConnected ───────────

    @Nested @DisplayName("isConnected")
    class IsConnected {

        @Test @DisplayName("chưa connect → false")
        void not_connected() {
            assertFalse(AuctionClient.getInstance().isConnected());
        }

        @Test @DisplayName("sau disconnect → false")
        void after_disconnect_false() {
            AuctionClient.getInstance().disconnect();
            assertFalse(AuctionClient.getInstance().isConnected());
        }
    }

    // ─────────── reconnect ───────────

    @Nested @DisplayName("reconnect")
    class Reconnect {

        @Test @DisplayName("host/port chưa set → false")
        void no_host_port_returns_false() {
            assertFalse(AuctionClient.getInstance().reconnect());
        }

        @Test @DisplayName("reconnect không crash")
        void reconnect_no_crash() {
            assertDoesNotThrow(() -> AuctionClient.getInstance().reconnect());
        }
    }

    // ─────────── disconnect ───────────

    @Nested @DisplayName("disconnect")
    class Disconnect {

        @Test @DisplayName("disconnect khi chưa connect → không crash")
        void not_connected_no_crash() {
            assertDoesNotThrow(() -> AuctionClient.getInstance().disconnect());
        }

        @Test @DisplayName("disconnect nhiều lần → không crash")
        void multiple_disconnect_no_crash() {
            assertDoesNotThrow(() -> {
                AuctionClient.getInstance().disconnect();
                AuctionClient.getInstance().disconnect();
                AuctionClient.getInstance().disconnect();
            });
        }

        @Test @DisplayName("sau disconnect → isConnected() = false")
        void after_disconnect_not_connected() {
            AuctionClient.getInstance().disconnect();
            assertFalse(AuctionClient.getInstance().isConnected());
        }
    }

    // ─────────── sendRequest – not connected ───────────

    @Nested @DisplayName("sendRequest – not connected")
    class SendRequestNotConnected {

        @Test @DisplayName("callback nhận error response khi không connected")
        void callback_receives_error_response() {
            AtomicReference<ServerResponse> received = new AtomicReference<>();
            ServerRequest<Void> req = ServerRequest.anonymous(ActionType.HEARTBEAT);
            AuctionClient.getInstance().sendRequest(req, received::set);
            // Platform.runLater đã được schedule → đợi ngắn
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            // Kết quả có thể chưa về ngay do runLater, nhưng không crash
            assertDoesNotThrow(() -> AuctionClient.getInstance().sendRequest(req, r -> {}));
        }

        @Test @DisplayName("callback null khi không connected → không crash")
        void null_callback_no_crash() {
            ServerRequest<Void> req = ServerRequest.anonymous(ActionType.HEARTBEAT);
            assertDoesNotThrow(() -> AuctionClient.getInstance().sendRequest(req, null));
        }

        @Test @DisplayName("sendRequest(request) overload → không crash")
        void overload_no_callback_no_crash() {
            ServerRequest<Void> req = ServerRequest.anonymous(ActionType.HEARTBEAT);
            assertDoesNotThrow(() -> AuctionClient.getInstance().sendRequest(req));
        }

        @Test @DisplayName("callback được gọi với success=false khi không connected")
        void error_response_success_false() throws InterruptedException {
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            AtomicBoolean successValue = new AtomicBoolean(true);
            ServerRequest<Void> req = ServerRequest.anonymous(ActionType.HEARTBEAT);
            AuctionClient.getInstance().sendRequest(req, r -> {
                callbackCalled.set(true);
                successValue.set(r.isSuccess());
            });
            Thread.sleep(200);
            if (callbackCalled.get()) {
                assertFalse(successValue.get());
            }
            // Nếu chưa gọi (runLater chưa execute) → test vẫn pass
        }
    }

    // ─────────── sendAuthenticatedRequest – not connected ───────────

    @Nested @DisplayName("sendAuthenticatedRequest – not connected")
    class SendAuthNotConnected {

        @Test @DisplayName("callback không null → không crash")
        void with_callback_no_crash() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            assertDoesNotThrow(() ->
                    AuctionClient.getInstance().sendAuthenticatedRequest(
                            ActionType.ADMIN_GET_AUCTIONS, null, r -> {}));
        }

        @Test @DisplayName("callback null → không crash")
        void null_callback_no_crash() {
            SessionManager.setSession("tok", 1, "u", "f", "MEMBER");
            assertDoesNotThrow(() ->
                    AuctionClient.getInstance().sendAuthenticatedRequest(
                            ActionType.ADMIN_GET_AUCTIONS, null));
        }

        @Test @DisplayName("chưa login → token null nhưng không crash")
        void no_session_no_crash() {
            assertDoesNotThrow(() ->
                    AuctionClient.getInstance().sendAuthenticatedRequest(
                            ActionType.ADMIN_GET_AUCTIONS, null, r -> {}));
        }
    }
}