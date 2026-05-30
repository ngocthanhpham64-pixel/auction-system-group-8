package vn.edu.vnu.uet.group8.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private final AdminController adminController;
  private final NotificationController notificationController;
  private final FavoriteController favoriteController;
  private final RatingController ratingController;
  private final SessionManager sessionManager;

  public AppDispatcher(AuthController authController,
                       ItemController itemController,
                       BidController bidController,
                       UserController userController,
                       AdminController adminController,
                       NotificationController notificationController,
                       FavoriteController favoriteController,
                       RatingController ratingController,
                       SessionManager sessionManager) {
    this.authController = authController;
    this.itemController = itemController;
    this.bidController = bidController;
    this.userController = userController;
    this.adminController = adminController;
    this.notificationController = notificationController;
    this.favoriteController = favoriteController;
    this.ratingController = ratingController;
    this.sessionManager = sessionManager;
  }

  /**
   * Dispatch một request đến controller phù hợp.
   * KHÔNG bao giờ throw — mọi error đều biến thành ServerResponse error.
   *
   * @param request JSON request gốc từ client
   * @return ServerResponse đã sẵn sàng gửi đi
   */
  public ServerResponse dispatch(JsonObject request, ClientHandler clientHandler) {
    String action = null;
    String requestId = null;
    try {
      action = RequestParser.getAction(request);
      requestId = RequestParser.getRequestId(request);

      // Normalize: ActionType.SerializedName dùng lowercase, nhưng client
      // có thể gửi upper/lower — chuẩn hoá thành UPPER.
      String normalized = action.toUpperCase();

      log.info("Dispatching: action='{}' (normalized='{}'), requestId={}", action, normalized, requestId);

      switch (normalized) {
        // ---- Anonymous actions ----
        case "LOGIN":
          return authController.handleLogin(request, requestId);
        case "REGISTER":
          return authController.handleRegister(request, requestId);
        case "HEARTBEAT":
          return authController.handleHeartbeat(requestId);
        case "AUTH_REQUEST_OTP":
          return authController.handleRequestOtp(request, requestId);
        case "AUTH_RESET_PASSWORD":
          return authController.handleResetPassword(request, requestId);

        // ---- Authenticated actions ----
        default:
          return dispatchAuthenticated(normalized, request, requestId, clientHandler);
      }

    } catch (Throwable e) {
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
                                               String requestId, ClientHandler clientHandler) {
    String token = RequestParser.getToken(request);
    int userId = sessionManager.validateToken(token);
    if (userId == -1) {
      log.warn("Authentication failed for action {}: Token is invalid or expired. Token used: {}", 
               action, (token != null ? "EXISTS" : "NULL"));
      return ServerResponse.replyError(action, requestId, 
          "Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
    }

    if (clientHandler != null) {
      clientHandler.associateUser(userId);
    }

    switch (action) {
      case "LOGOUT":
        return authController.handleLogout(token, requestId);

      // Item
      case "ITEM_GET_ALL":
      case "ITEM_ALL":
        return itemController.handleGetAll(request, requestId);
      case "ITEM_GET_DETAIL":
        return itemController.handleGetDetail(request, requestId);
      case "ITEM_CREATE":
        return itemController.handleCreateItem(request, requestId, userId);
      
      // Bid
      case "BID_PLACE":
        return bidController.handlePlaceBid(request, requestId, userId);
      case "BID_HISTORY":
        return bidController.handleItemBidHistory(request, requestId);
      case "USER_BIDS":
        return bidController.handleUserBidHistory(request, requestId, userId);
      case "ITEM_MY_LISTINGS":
        return itemController.handleGetMyListings(request, requestId, userId);
      case "ITEM_UPDATE":
        return itemController.handleUpdateItem(request, requestId, userId);
      case "ITEM_DELETE":
        return itemController.handleDeleteItem(request, requestId, userId);
      case "BID_AUTO":
        return bidController.handleAutoBid(request, requestId, userId);
      case "GET_AUTO_BID_STATUS":
        return bidController.handleGetAutoBidStatus(request, requestId, userId);
      // case "BID_HISTORY":
      //   return bidController.handleBidHistory(request, requestId, userId);

      // User
      case "USER_PROFILE":
        return userController.handleGetProfile(request, requestId, userId);
      case "USER_PURCHASE_HISTORY":
        return itemController.handleGetPurchaseHistory(request, requestId, userId);
      case "USER_DEPOSIT":
        return userController.handleDeposit(request, requestId, userId);
      case "USER_WITHDRAW":
        return userController.handleWithdraw(request, requestId, userId);
      case "WALLET_GET_TRANSACTIONS":
        return userController.handleGetTransactions(request, requestId, userId);
      case "USER_CHANGE_PASSWORD":
        return userController.handleChangePassword(request, requestId, userId);
      case "USER_UPDATE_PROFILE":
        return userController.handleUpdateProfile(request, requestId, userId);

      // Admin
      case "ADMIN_DASHBOARD":
        return adminController.handleDashboard(requestId, userId);
      case "ADMIN_GET_USERS":
        return adminController.handleGetUsers(requestId, userId);
      case "ADMIN_UPDATE_USER_STATUS":
        return adminController.handleUpdateUserStatus(request, requestId, userId);
      case "ADMIN_GET_AUCTIONS":
        return adminController.handleGetAuctions(requestId, userId);
      case "ADMIN_CANCEL_AUCTION":
        return adminController.handleCancelAuction(request, requestId, userId);

      // Notifications
      case "NOTIF_GET_ALL":
      case "NOTIF_ALL":
        return notificationController.handleGetAll(requestId, userId);
      case "NOTIF_MARK_READ":
      case "NOTIF_READ":
        return notificationController.handleMarkRead(request, requestId, userId);
      case "NOTIF_DELETE":
        return notificationController.handleDelete(request, requestId, userId);

      // Favorites
      case "FAVORITE_LIST":
      case "FAV_LIST":
        return favoriteController.handleGetList(requestId, userId);
      case "FAVORITE_ADD":
      case "FAV_ADD":
        return favoriteController.handleAdd(request, requestId, userId);
      case "FAVORITE_REMOVE":
      case "FAV_REMOVE":
        return favoriteController.handleRemove(request, requestId, userId);

      case "USER_RATE_SELLER":
        return ratingController.handleRateSeller(request, requestId, userId);
      case "USER_GET_SELLER_REVIEWS":
        return ratingController.handleGetSellerReviews(request, requestId);

      default:
        log.warn("Action không xác định: {}", action);
        return ServerResponse.replyError(action, requestId,
            "Hành động không được hỗ trợ: " + action);
    }
  }
}
