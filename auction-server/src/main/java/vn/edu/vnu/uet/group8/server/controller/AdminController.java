package vn.edu.vnu.uet.group8.server.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.UnauthorizedException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.user.AdminUserService;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;

/**
 * AdminController — handle các action ADMIN_* từ client.
 *
 * <p>Controller giờ đây chỉ đảm nhận vai trò tiếp nhận và
 * phản hồi HTTP, mọi nghiệp vụ DB được ủy quyền hoàn toàn cho AdminService.
 */
public class AdminController {
  private static final Logger log = LoggerFactory.getLogger(AdminController.class);
  private final AdminUserService adminService;

  public AdminController(UserDAO userDAO, AuctionService auctionService) {
    this.adminService = new AdminUserService(userDAO, auctionService);
  }

  // ===========================================================================
  // ADMIN_DASHBOARD — tổng quan
  // ===========================================================================
  public ServerResponse handleDashboard(String requestId, int userId) {
    try {
      AdminStatsDTO stats = adminService.getDashboardStats(userId);
      return ServerResponse.reply("ADMIN_DASHBOARD", requestId)
          .success(true).message("OK").data(stats).build();

    } catch (UnauthorizedException e) {
      return ServerResponse.replyError("ADMIN_DASHBOARD", requestId, e.getMessage());
    } catch (Exception e) {
      log.error("ADMIN_DASHBOARD lỗi", e);
      return ServerResponse.replyError("ADMIN_DASHBOARD", requestId, "Lỗi máy chủ: " + e.getMessage());
    }
  }

  // ===========================================================================
  // ADMIN_GET_USERS — danh sách user
  // ===========================================================================
  public ServerResponse handleGetUsers(String requestId, int userId) {
    try {
      List<UserAdminDTO> users = adminService.getUsers(userId);
      log.debug("ADMIN_GET_USERS: trả về {} users", users.size());
      return ServerResponse.reply("ADMIN_GET_USERS", requestId)
          .success(true).message("OK").data(users).build();

    } catch (UnauthorizedException e) {
      return ServerResponse.replyError("ADMIN_GET_USERS", requestId, e.getMessage());
    } catch (Exception e) {
      log.error("ADMIN_GET_USERS lỗi", e);
      return ServerResponse.replyError("ADMIN_GET_USERS", requestId, "Lỗi máy chủ: " + e.getMessage());
    }
  }

  // ===========================================================================
  // ADMIN_UPDATE_USER_STATUS — đổi ACTIVE/SUSPENDED/BANNED
  // ===========================================================================
  public ServerResponse handleUpdateUserStatus(JsonObject request, String requestId, int adminId) {
    try {
      JsonObject payload = request.getAsJsonObject("payload");
      if (payload == null) {
        return ServerResponse.replyError("ADMIN_UPDATE_USER_STATUS", requestId, "Thiếu payload");
      }
      int targetId = payload.get("userId").getAsInt();
      UserStatus newStatus = UserStatus.valueOf(payload.get("status").getAsString());

      adminService.updateUserStatus(adminId, targetId, newStatus);
      log.info("Admin {} đã đổi trạng thái user {} -> {}", adminId, targetId, newStatus);
      return ServerResponse.reply("ADMIN_UPDATE_USER_STATUS", requestId)
          .success(true).message("Cập nhật thành công").build();

    } catch (IllegalArgumentException | UnauthorizedException e) {
      return ServerResponse.replyError("ADMIN_UPDATE_USER_STATUS", requestId, e.getMessage());
    } catch (Exception e) {
      log.error("ADMIN_UPDATE_USER_STATUS lỗi", e);
      return ServerResponse.replyError("ADMIN_UPDATE_USER_STATUS", requestId, "Lỗi máy chủ: " + e.getMessage());
    }
  }

  // ===========================================================================
  // ADMIN_GET_AUCTIONS — danh sách phiên đấu giá để admin quản lý
  // ===========================================================================
  public ServerResponse handleGetAuctions(String requestId, int userId) {
    try {
      List<AuctionItemDTO> list = adminService.getAuctions(userId);
      log.debug("ADMIN_GET_AUCTIONS: trả về {} phiên", list.size());
      return ServerResponse.reply("ADMIN_GET_AUCTIONS", requestId)
          .success(true).message("OK").data(list).build();

    } catch (UnauthorizedException e) {
      return ServerResponse.replyError("ADMIN_GET_AUCTIONS", requestId, e.getMessage());
    } catch (Exception e) {
      log.error("ADMIN_GET_AUCTIONS lỗi", e);
      return ServerResponse.replyError("ADMIN_GET_AUCTIONS", requestId, "Lỗi máy chủ: " + e.getMessage());
    }
  }

  // ===========================================================================
  // ADMIN_CANCEL_AUCTION — đặt status = CANCELLED
  // ===========================================================================
  public ServerResponse handleCancelAuction(JsonObject request, String requestId, int adminId) {
    try {
      int sessionId;
      try {
        sessionId = request.get("payload").getAsInt();
      } catch (Exception ex) {
        return ServerResponse.replyError("ADMIN_CANCEL_AUCTION", requestId, "sessionId không hợp lệ");
      }

      adminService.cancelAuction(adminId, sessionId);
      log.info("Admin {} đã huỷ phiên {}", adminId, sessionId);
      return ServerResponse.reply("ADMIN_CANCEL_AUCTION", requestId)
          .success(true).message("Đã huỷ phiên đấu giá").build();

    } catch (IllegalArgumentException | UnauthorizedException e) {
      return ServerResponse.replyError("ADMIN_CANCEL_AUCTION", requestId, e.getMessage());
    } catch (Exception e) {
      log.error("ADMIN_CANCEL_AUCTION lỗi", e);
      return ServerResponse.replyError("ADMIN_CANCEL_AUCTION", requestId, "Lỗi máy chủ: " + e.getMessage());
    }
  }
}