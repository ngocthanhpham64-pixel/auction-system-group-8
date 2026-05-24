package vn.edu.vnu.uet.group8.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;
import vn.edu.vnu.uet.group8.server.service.user.AdminUserService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminUserService adminUserService;

    private AdminController controller;

    /**
     * AdminController tạo AdminUserService bên trong constructor(UserDAO, AuctionService).
     * Dùng spy/inject thủ công để mock adminService.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Tạo controller với DAO mock tạm, sau đó inject adminUserService mock qua reflection
        UserDAO dummyDAO = mock(UserDAO.class);
        AuctionService dummyAuction = mock(AuctionService.class);
        controller = new AdminController(dummyDAO, dummyAuction);

        // Inject mock adminUserService qua reflection
        var field = AdminController.class.getDeclaredField("adminService");
        field.setAccessible(true);
        field.set(controller, adminUserService);
    }

    // ─────────────────────────────────────────────────────────────
    // handleDashboard
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleDashboard()")
    class DashboardTest {

        @Test
        @DisplayName("Success - trả về AdminStatsDTO")
        void success() throws SQLException {
            AdminStatsDTO stats = new AdminStatsDTO(5, 100, new BigDecimal("9000000"), 200);
            when(adminUserService.getDashboardStats(1)).thenReturn(stats);

            ServerResponse resp = controller.handleDashboard("req-dash", 1);

            assertTrue(resp.isSuccess());
            assertEquals("ADMIN_DASHBOARD", resp.getAction());
            assertEquals("OK", resp.getMessage());
            verify(adminUserService).getDashboardStats(1);
        }

        @Test
        @DisplayName("UnauthorizedException → error response")
        void unauthorized() throws SQLException {
            when(adminUserService.getDashboardStats(anyInt()))
                    .thenThrow(new UnauthorizedException("thực hiện thao tác quản trị"));

            ServerResponse resp = controller.handleDashboard("req-unauth", 99);

            assertFalse(resp.isSuccess());
            assertEquals("ADMIN_DASHBOARD", resp.getAction());
        }

        @Test
        @DisplayName("SQLException → error response với message server")
        void sqlException() throws SQLException {
            when(adminUserService.getDashboardStats(anyInt()))
                    .thenThrow(new SQLException("DB lỗi"));

            ServerResponse resp = controller.handleDashboard("req-sql", 1);

            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Lỗi máy chủ"));
        }

        @Test
        @DisplayName("RuntimeException → error response")
        void runtimeError() throws SQLException {
            when(adminUserService.getDashboardStats(anyInt()))
                    .thenThrow(new RuntimeException("unexpected"));

            ServerResponse resp = controller.handleDashboard("req-rt", 1);

            assertFalse(resp.isSuccess());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleGetUsers
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleGetUsers()")
    class GetUsersTest {

        @Test
        @DisplayName("Success - trả về danh sách users")
        void success() throws SQLException {
            List<UserAdminDTO> users = List.of(
                    new UserAdminDTO(1, "admin01", "a@e.com", "ADMIN", "ACTIVE"),
                    new UserAdminDTO(2, "user02", "u@e.com", "MEMBER", "ACTIVE"));
            when(adminUserService.getUsers(1)).thenReturn(users);

            ServerResponse resp = controller.handleGetUsers("req-users", 1);

            assertTrue(resp.isSuccess());
            assertEquals("ADMIN_GET_USERS", resp.getAction());
        }

        @Test
        @DisplayName("Danh sách rỗng vẫn trả success")
        void emptyList() throws SQLException {
            when(adminUserService.getUsers(anyInt())).thenReturn(Collections.emptyList());

            ServerResponse resp = controller.handleGetUsers("req-empty", 1);

            assertTrue(resp.isSuccess());
        }

        @Test
        @DisplayName("UnauthorizedException → error response")
        void unauthorized() throws SQLException {
            when(adminUserService.getUsers(anyInt()))
                    .thenThrow(new UnauthorizedException("không có quyền"));

            ServerResponse resp = controller.handleGetUsers("req-unauth", 99);

            assertFalse(resp.isSuccess());
            assertEquals("ADMIN_GET_USERS", resp.getAction());
        }

        @Test
        @DisplayName("SQLException → error response")
        void sqlException() throws SQLException {
            when(adminUserService.getUsers(anyInt())).thenThrow(new SQLException("DB"));

            ServerResponse resp = controller.handleGetUsers("req-sql", 1);

            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Lỗi máy chủ"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleUpdateUserStatus
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleUpdateUserStatus()")
    class UpdateUserStatusTest {

        private JsonObject buildRequest(int targetId, String status) {
            JsonObject req = new JsonObject();
            JsonObject payload = new JsonObject();
            payload.addProperty("userId", targetId);
            payload.addProperty("status", status);
            req.add("payload", payload);
            return req;
        }

        @Test
        @DisplayName("Success - đổi ACTIVE → SUSPENDED")
        void successSuspend() throws Exception {
            ServerResponse resp = controller.handleUpdateUserStatus(
                    buildRequest(5, "SUSPENDED"), "req-update", 1);

            assertTrue(resp.isSuccess());
            assertEquals("ADMIN_UPDATE_USER_STATUS", resp.getAction());
            verify(adminUserService).updateUserStatus(1, 5, UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("Success - đổi SUSPENDED → ACTIVE")
        void successActivate() throws Exception {
            ServerResponse resp = controller.handleUpdateUserStatus(
                    buildRequest(5, "ACTIVE"), "req-activate", 1);

            assertTrue(resp.isSuccess());
            verify(adminUserService).updateUserStatus(1, 5, UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Success - đổi ACTIVE → BANNED")
        void successBan() throws Exception {
            ServerResponse resp = controller.handleUpdateUserStatus(
                    buildRequest(5, "BANNED"), "req-ban", 1);

            assertTrue(resp.isSuccess());
            verify(adminUserService).updateUserStatus(1, 5, UserStatus.BANNED);
        }

        @Test
        @DisplayName("Thiếu payload → error response")
        void missingPayload() {
            JsonObject req = new JsonObject(); // không có payload

            ServerResponse resp = controller.handleUpdateUserStatus(req, "req-no-payload", 1);

            assertFalse(resp.isSuccess());
            assertEquals("ADMIN_UPDATE_USER_STATUS", resp.getAction());
            assertTrue(resp.getMessage().contains("Thiếu payload"));
        }

        @Test
        @DisplayName("status không hợp lệ (IllegalArgumentException) → error response")
        void invalidStatus() {
            JsonObject req = new JsonObject();
            JsonObject payload = new JsonObject();
            payload.addProperty("userId", 5);
            payload.addProperty("status", "INVALID_STATUS");
            req.add("payload", payload);

            ServerResponse resp = controller.handleUpdateUserStatus(req, "req-bad-status", 1);

            assertFalse(resp.isSuccess());
        }

        @Test
        @DisplayName("UnauthorizedException → error response")
        void unauthorized() throws Exception {
            doThrow(new UnauthorizedException("chỉ Super Admin"))
                    .when(adminUserService).updateUserStatus(anyInt(), anyInt(), any());

            ServerResponse resp = controller.handleUpdateUserStatus(
                    buildRequest(5, "BANNED"), "req-unauth", 2);

            assertFalse(resp.isSuccess());
        }

        @Test
        @DisplayName("UserNotFoundException → error response")
        void userNotFound() throws Exception {
            doThrow(new UserNotFoundException(999))
                    .when(adminUserService).updateUserStatus(anyInt(), anyInt(), any());

            ServerResponse resp = controller.handleUpdateUserStatus(
                    buildRequest(999, "SUSPENDED"), "req-notfound", 1);

            assertFalse(resp.isSuccess());
        }

        @Test
        @DisplayName("SQLException → error response với 'Lỗi máy chủ'")
        void sqlException() throws Exception {
            doThrow(new SQLException("DB"))
                    .when(adminUserService).updateUserStatus(anyInt(), anyInt(), any());

            ServerResponse resp = controller.handleUpdateUserStatus(
                    buildRequest(5, "ACTIVE"), "req-sql", 1);

            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Lỗi máy chủ"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleGetAuctions
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleGetAuctions()")
    class GetAuctionsTest {

        @Test
        @DisplayName("Success - trả về danh sách phiên")
        void success() throws SQLException {
            List<AuctionItemDTO> list = List.of(mock(AuctionItemDTO.class));
            when(adminUserService.getAuctions(1)).thenReturn(list);

            ServerResponse resp = controller.handleGetAuctions("req-auctions", 1);

            assertTrue(resp.isSuccess());
            assertEquals("ADMIN_GET_AUCTIONS", resp.getAction());
        }

        @Test
        @DisplayName("Danh sách rỗng vẫn trả success")
        void emptyList() throws SQLException {
            when(adminUserService.getAuctions(anyInt())).thenReturn(Collections.emptyList());

            assertTrue(controller.handleGetAuctions("req-e", 1).isSuccess());
        }

        @Test
        @DisplayName("UnauthorizedException → error response")
        void unauthorized() throws SQLException {
            when(adminUserService.getAuctions(anyInt()))
                    .thenThrow(new UnauthorizedException("không được"));

            assertFalse(controller.handleGetAuctions("req-ua", 99).isSuccess());
        }

        @Test
        @DisplayName("SQLException → error response")
        void sqlException() throws SQLException {
            when(adminUserService.getAuctions(anyInt())).thenThrow(new SQLException("DB"));

            ServerResponse resp = controller.handleGetAuctions("req-sql", 1);
            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Lỗi máy chủ"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // handleCancelAuction
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleCancelAuction()")
    class CancelAuctionTest {

        @Test
        @DisplayName("Success - huỷ phiên với payload là số int")
        void success() throws Exception {
            JsonObject req = new JsonObject();
            req.addProperty("payload", 10);

            ServerResponse resp = controller.handleCancelAuction(req, "req-cancel", 1);

            assertTrue(resp.isSuccess());
            assertEquals("ADMIN_CANCEL_AUCTION", resp.getAction());
            verify(adminUserService).cancelAuction(1, 10);
        }

        @Test
        @DisplayName("payload không phải int → error response 'sessionId không hợp lệ'")
        void invalidPayload() {
            JsonObject req = new JsonObject();
            req.addProperty("payload", "not-a-number");

            ServerResponse resp = controller.handleCancelAuction(req, "req-bad", 1);

            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("sessionId không hợp lệ"));
        }

        @Test
        @DisplayName("payload null → error response")
        void nullPayload() {
            JsonObject req = new JsonObject(); // không có payload field

            ServerResponse resp = controller.handleCancelAuction(req, "req-null", 1);

            assertFalse(resp.isSuccess());
        }

        @Test
        @DisplayName("UnauthorizedException → error response")
        void unauthorized() throws Exception {
            doThrow(new UnauthorizedException("chỉ admin"))
                    .when(adminUserService).cancelAuction(anyInt(), anyInt());
            JsonObject req = new JsonObject();
            req.addProperty("payload", 5);

            ServerResponse resp = controller.handleCancelAuction(req, "req-unauth", 2);

            assertFalse(resp.isSuccess());
        }

        @Test
        @DisplayName("IllegalArgumentException → error response")
        void illegalArgument() throws Exception {
            doThrow(new IllegalArgumentException("phiên không hợp lệ"))
                    .when(adminUserService).cancelAuction(anyInt(), anyInt());
            JsonObject req = new JsonObject();
            req.addProperty("payload", 99);

            ServerResponse resp = controller.handleCancelAuction(req, "req-illegal", 1);

            assertFalse(resp.isSuccess());
        }

        @Test
        @DisplayName("SQLException → error response với 'Lỗi máy chủ'")
        void sqlException() throws Exception {
            doThrow(new SQLException("DB"))
                    .when(adminUserService).cancelAuction(anyInt(), anyInt());
            JsonObject req = new JsonObject();
            req.addProperty("payload", 3);

            ServerResponse resp = controller.handleCancelAuction(req, "req-sql", 1);

            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Lỗi máy chủ"));
        }
    }
}