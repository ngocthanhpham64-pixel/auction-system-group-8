package vn.edu.vnu.uet.group8.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.controller.AuthController;
import vn.edu.vnu.uet.group8.server.controller.BidController;
import vn.edu.vnu.uet.group8.server.controller.ItemController;
import vn.edu.vnu.uet.group8.server.controller.UserController;

/**
 * Dispatcher — router thuần, không chứa logic.
 *
 * <p>Nhiệm vụ duy nhất: nhận JsonObject + action string, route đến đúng
 * controller method. Cũng là nơi enforce authentication (action authenticated
 * phải có token hợp lệ).
 *
 * <p>Đây là điểm tập trung — mọi action mới chỉ cần thêm 1 case ở switch.
 * ClientHandler không bao giờ phải sửa.
 */
public class AppDispatcher {
  private static final Logger log = LoggerFactory.getLogger(AppDispatcher.class);

  private final AuthController authController;
  private final ItemController itemController;
  private final BidController bidController;
  private final UserController userController;
  private final SessionManager sessionManager;

  public AppDispatcher(AuthController authController,
                       ItemController itemController,
                       BidController bidController,
                       UserController userController,
                       SessionManager sessionManager) {
    this.authController = authController;
    this.itemController = itemController;
    this.bidController = bidController;
    this.userController = userController;
    this.sessionManager = sessionManager;
  }

  /**
   * Dispatch một request đến controller phù hợp.
   * KHÔNG bao giờ throw — mọi error đều biến thành ServerResponse error.
   *
   * @param request JSON request gốc từ client
   * @return ServerResponse đã sẵn sàng gửi đi
   */
  public ServerResponse dispatch(JsonObject request) {
    String action = null;
    String requestId = null;
    try {
      action = RequestParser.getAction(request);
      requestId = RequestParser.getRequestId(request);

      // Normalize: ActionType.SerializedName dùng lowercase, nhưng client
      // có thể gửi upper/lower — chuẩn hoá thành UPPER.
      String normalized = action.toUpperCase();

      switch (normalized) {
        // ---- Anonymous actions ----
        case "LOGIN":
          return authController.handleLogin(request, requestId);
        case "REGISTER":
          return authController.handleRegister(request, requestId);
        case "HEARTBEAT":
          return authController.handleHeartbeat(requestId);

        // ---- Authenticated actions ----
        default:
          return dispatchAuthenticated(normalized, request, requestId);
      }

    } catch (Exception e) {
      log.error("Lỗi không mong muốn khi dispatch action={}", action, e);
      return ServerResponse.replyError(
          action != null ? action : "UNKNOWN", requestId,
          "Lỗi máy chủ: " + e.getMessage());
    }
  }

  /**
   * Verify token rồi route các action cần đăng nhập.
   */
  private ServerResponse dispatchAuthenticated(String action, JsonObject request,
                                               String requestId) {
    String token = RequestParser.getToken(request);
    int userId = sessionManager.validateToken(token);
    if (userId == -1) {
      return ServerResponse.replyError(action, requestId,
          "Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
    }

    switch (action) {
      case "LOGOUT":
        return authController.handleLogout(token, requestId);

      // Item
      case "ITEM_GET_ALL":
        return itemController.handleGetAll(request, requestId);
      case "ITEM_GET_DETAIL":
        return itemController.handleGetDetail(request, requestId);
      case "ITEM_CREATE":
        return itemController.handleCreateItem(request, requestId);
      
    
      // Bid
      case "BID_PLACE":
        return bidController.handlePlaceBid(request, requestId, userId);
      case "ITEM_BID_HISTORY":
        return bidController.handleItemBidHistory(request, requestId);
      case "USER_BIDS":
        return bidController.handleUserBidHistory(request, requestId, userId);
      case "ITEM_MY_LISTINGS":
        return itemController.handleGetMyListings(request, requestId, userId);
      case "ITEM_UPDATE":
        return itemController.handleUpdateItem(request, requestId, userId);
      case "ITEM_DELETE":
        return itemController.handleDeleteItem(request, requestId, userId);
      // case "BID_AUTO":
      //   return bidController.handleAutoBid(request, requestId, userId);
      // case "BID_HISTORY":
      //   return bidController.handleBidHistory(request, requestId, userId);

      // User
      case "USER_PROFILE":
        return userController.handleGetProfile(request, requestId, userId);
      case "USER_DEPOSIT":
        return userController.handleDeposit(request, requestId, userId);
      case "USER_WITHDRAW":
        return userController.handleWithdraw(request, requestId, userId);
      case "USER_GET_TRANSACTIONS":
        return userController.handleGetTransactions(request, requestId, userId);
      case "USER_CHANGE_PASSWORD":
        return userController.handleChangePassword(request, requestId, userId);


      default:
        log.warn("Action không xác định: {}", action);
        return ServerResponse.replyError(action, requestId,
            "Hành động không được hỗ trợ: " + action);
    }
  }
}
