package vn.edu.vnu.uet.group8.server.network;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.controller.AdminController;
import vn.edu.vnu.uet.group8.server.controller.AuthController;
import vn.edu.vnu.uet.group8.server.controller.BidController;
import vn.edu.vnu.uet.group8.server.controller.FavoriteController;
import vn.edu.vnu.uet.group8.server.controller.ItemController;
import vn.edu.vnu.uet.group8.server.controller.NotificationController;
import vn.edu.vnu.uet.group8.server.controller.RatingController;
import vn.edu.vnu.uet.group8.server.controller.UserController;

/**

 * Test cho AppDispatcher.
 */
@ExtendWith(MockitoExtension.class)
class AppDispatcherTest {

    @Mock private AuthController authController;
    @Mock private ItemController itemController;
    @Mock private BidController bidController;
    @Mock private UserController userController;

    @Mock private AdminController adminController;
    @Mock private NotificationController notificationController;
    @Mock private FavoriteController favoriteController;
    @Mock private RatingController ratingController;

    @Mock private SessionManager sessionManager;

    private AppDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new AppDispatcher(
                authController,
                itemController,
                bidController,
                userController,
                adminController,
                notificationController,
                favoriteController,
                ratingController,
                sessionManager
        );
    }

    /** Helper - tạo request với action. */
    private JsonObject taoReq(String action, String requestId) {
        JsonObject req = new JsonObject();
        req.addProperty("requestId", "req-1");

        req.addProperty("action", action);


        if (requestId != null) {
            req.addProperty("requestId", requestId);
        }

        return req;

    }

    /** Helper - tạo request authenticated. */
    private JsonObject taoReqAuth(String action, String requestId, String token) {
        JsonObject req = taoReq(action, requestId);
        req.addProperty("token", token);
        return req;
    }

    /** Helper - response stub. */
    private ServerResponse stubResponse(String action) {
        return ServerResponse.reply(action, "req-1")
                .success(true)
                .build();
    }

    @Nested
    @DisplayName("Anonymous actions")
    class AnonymousTest {


        @Test
        @DisplayName("LOGIN → authController.handleLogin")
        void login() {
            JsonObject req = taoReq("LOGIN", "req-1");

            when(authController.handleLogin(any(), any()))
                    .thenReturn(stubResponse("LOGIN"));

            ServerResponse res = dispatcher.dispatch(req);

            assertTrue(res.isSuccess());

            verify(authController)
                    .handleLogin(req, "req-1");
        }

        @Test
        @DisplayName("REGISTER → authController.handleRegister")
        void register() {
            JsonObject req = taoReq("REGISTER", "req-1");

            when(authController.handleRegister(any(), any()))
                    .thenReturn(stubResponse("REGISTER"));

            dispatcher.dispatch(req);

            verify(authController)
                    .handleRegister(req, "req-1");
        }

        @Test
        @DisplayName("HEARTBEAT → authController.handleHeartbeat")
        void heartbeat() {
            JsonObject req = taoReq("HEARTBEAT", "req-1");

            when(authController.handleHeartbeat(any()))
                    .thenReturn(stubResponse("HEARTBEAT"));

            dispatcher.dispatch(req);

            verify(authController)
                    .handleHeartbeat("req-1");
        }

        @Test
        @DisplayName("Action lowercase → normalize uppercase")
        void lowercaseNormalize() {
            JsonObject req = taoReq("login", "req-1");

            when(authController.handleLogin(any(), any()))
                    .thenReturn(stubResponse("LOGIN"));

            dispatcher.dispatch(req);

            verify(authController)
                    .handleLogin(req, "req-1");
        }


    }

    @Nested
    @DisplayName("Authenticated actions")
    class AuthenticatedTest {


        @Test
        @DisplayName("LOGOUT")
        void logout() {
            JsonObject req = taoReqAuth("LOGOUT", "req-1", "tok-1");

            when(sessionManager.validateToken("tok-1"))
                    .thenReturn(100);

            when(authController.handleLogout(any(), any()))
                    .thenReturn(stubResponse("LOGOUT"));

            dispatcher.dispatch(req);

            verify(authController)
                    .handleLogout("tok-1", "req-1");
        }

        @Test
        @DisplayName("BID_PLACE")
        void bidPlace() {
            JsonObject req = taoReqAuth("BID_PLACE", "req-1", "tok-1");

            when(sessionManager.validateToken("tok-1"))
                    .thenReturn(100);

            when(bidController.handlePlaceBid(any(), any(), anyInt()))
                    .thenReturn(stubResponse("BID_PLACE"));

            dispatcher.dispatch(req);

            verify(bidController)
                    .handlePlaceBid(req, "req-1", 100);
        }

        @Test
        @DisplayName("USER_PROFILE")
        void userProfile() {
            JsonObject req = taoReqAuth("USER_PROFILE", "req-1", "tok-1");

            when(sessionManager.validateToken("tok-1"))
                    .thenReturn(50);

            when(userController.handleGetProfile(any(), any(), anyInt()))
                    .thenReturn(stubResponse("USER_PROFILE"));

            dispatcher.dispatch(req);

            verify(userController)
                    .handleGetProfile(req, "req-1", 50);
        }

        @Test
        @DisplayName("ITEM_GET_ALL")
        void itemGetAll() {
            JsonObject req = taoReqAuth("ITEM_GET_ALL", "req-1", "tok-1");

            when(sessionManager.validateToken("tok-1"))
                    .thenReturn(100);

            when(itemController.handleGetAll(any(), any()))
                    .thenReturn(stubResponse("ITEM_GET_ALL"));

            dispatcher.dispatch(req);

            verify(itemController)
                    .handleGetAll(req, "req-1");
        }


    }

    @Nested
    @DisplayName("Authentication failure")
    class AuthFailureTest {


        @Test
        @DisplayName("Token invalid")
        void tokenInvalid() {
            JsonObject req = taoReqAuth("USER_PROFILE", "req-1", "fake-token");

            when(sessionManager.validateToken("fake-token"))
                    .thenReturn(-1);

            ServerResponse res = dispatcher.dispatch(req);

            assertFalse(res.isSuccess());

            verifyNoInteractions(userController);
        }

        @Test
        @DisplayName("Missing token")
        void thieuToken() {
            JsonObject req = taoReq("USER_PROFILE", "req-1");

            when(sessionManager.validateToken(null))
                    .thenReturn(-1);

            ServerResponse res = dispatcher.dispatch(req);

            assertFalse(res.isSuccess());
        }

    }

    @Nested
    @DisplayName("Error handling")
    class ErrorTest {
        @Test
        @DisplayName("Unknown action")
        void actionKhongXacDinh() {
            JsonObject req = taoReqAuth("UNKNOWN_ACTION", "req-1", "tok-1");

            when(sessionManager.validateToken("tok-1"))
                    .thenReturn(100);

            ServerResponse res = dispatcher.dispatch(req);

            assertFalse(res.isSuccess());
        }


        @Test
        @DisplayName("Request thiếu field action")
        void thieuActionField() {

            JsonObject req = new JsonObject();
            req.addProperty("requestId", "req-1");

            assertThrows(
                    IllegalStateException.class,
                    () -> dispatcher.dispatch(req)
            );

        }


        @Test
        @DisplayName("Controller throw RuntimeException")
        void controllerThrow() {
            JsonObject req = taoReq("LOGIN", "req-1");

            when(authController.handleLogin(any(), any()))
                    .thenThrow(new RuntimeException("boom"));

            assertDoesNotThrow(() -> dispatcher.dispatch(req));
        }
    }
}
