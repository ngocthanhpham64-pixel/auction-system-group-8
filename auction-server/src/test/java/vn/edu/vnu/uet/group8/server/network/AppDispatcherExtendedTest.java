package vn.edu.vnu.uet.group8.server.network;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.controller.*;

@ExtendWith(MockitoExtension.class)
class AppDispatcherExtendedTest {

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

  private static final ServerResponse OK =
      ServerResponse.reply("OK", "req-ok")
          .success(true)
          .message("ok")
          .build();
  private static final int VALID_USER_ID = 5;

  @BeforeEach
  void setUp() {
    dispatcher = new AppDispatcher(
        authController, itemController, bidController,
        userController, adminController, notificationController,
        favoriteController, ratingController, sessionManager);

    // Mặc định: token hợp lệ
    lenient().when(sessionManager.validateToken(any())).thenReturn(VALID_USER_ID);
  }

  private JsonObject req(String action) {
    JsonObject r = new JsonObject();
    r.addProperty("action", action);
    r.addProperty("token", "valid-token");
    r.addProperty("requestId", "req-" + action);
    return r;
  }

  // ─────────────────────────────────────────────────────────────
  // Anonymous actions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Anonymous actions - chưa test")
  class AnonymousActionsTest {

    @Test
    @DisplayName("AUTH_REQUEST_OTP → authController.handleRequestOtp()")
    void authRequestOtp() throws Exception {
      when(authController.handleRequestOtp(any(), any())).thenReturn(OK);
      ServerResponse r = dispatcher.dispatch(req("AUTH_REQUEST_OTP"), null);
      assertTrue(r.isSuccess());
      verify(authController).handleRequestOtp(any(), any());
    }

    @Test
    @DisplayName("auth_request_otp lowercase → normalize đúng")
    void authRequestOtpLowercase() throws Exception {
      when(authController.handleRequestOtp(any(), any())).thenReturn(OK);
      dispatcher.dispatch(req("auth_request_otp"), null);
      verify(authController).handleRequestOtp(any(), any());
    }

    @Test
    @DisplayName("AUTH_RESET_PASSWORD → authController.handleResetPassword()")
    void authResetPassword() throws Exception {
      when(authController.handleResetPassword(any(), any())).thenReturn(OK);
      ServerResponse r = dispatcher.dispatch(req("AUTH_RESET_PASSWORD"), null);
      assertTrue(r.isSuccess());
      verify(authController).handleResetPassword(any(), any());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Item actions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Item actions - chưa test")
  class ItemActionsTest {

    @Test
    @DisplayName("ITEM_ALL → handleGetAll()")
    void itemAll() throws Exception {
      when(itemController.handleGetAll(any(), any())).thenReturn(OK);
      dispatcher.dispatch(req("ITEM_ALL"), null);
      verify(itemController).handleGetAll(any(), any());
    }

    @Test
    @DisplayName("ITEM_GET_DETAIL → handleGetDetail()")
    void itemGetDetail() throws Exception {
      when(itemController.handleGetDetail(any(), any())).thenReturn(OK);
      dispatcher.dispatch(req("ITEM_GET_DETAIL"), null);
      verify(itemController).handleGetDetail(any(), any());
    }

    @Test
    @DisplayName("ITEM_CREATE → handleCreateItem()")
    void itemCreate() throws Exception {
      when(itemController.handleCreateItem(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ITEM_CREATE"), null);
      verify(itemController).handleCreateItem(any(), any(), eq(VALID_USER_ID));
    }
    @Test
    @DisplayName("ITEM_MY_LISTINGS → handleGetMyListings()")
    void itemMyListings() throws Exception {
      when(itemController.handleGetMyListings(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ITEM_MY_LISTINGS"), null);
      verify(itemController).handleGetMyListings(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("ITEM_UPDATE → handleUpdateItem()")
    void itemUpdate() throws Exception {
      when(itemController.handleUpdateItem(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ITEM_UPDATE"), null);
      verify(itemController).handleUpdateItem(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("ITEM_DELETE → handleDeleteItem()")
    void itemDelete() throws Exception {
      when(itemController.handleDeleteItem(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ITEM_DELETE"), null);
      verify(itemController).handleDeleteItem(any(), any(), eq(VALID_USER_ID));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Bid actions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Bid actions - chưa test")
  class BidActionsTest {

    @Test
    @DisplayName("BID_HISTORY → handleItemBidHistory()")
    void bidHistory() throws Exception {
      when(bidController.handleItemBidHistory(any(), any())).thenReturn(OK);
      dispatcher.dispatch(req("BID_HISTORY"), null);
      verify(bidController).handleItemBidHistory(any(), any());
    }

    @Test
    @DisplayName("USER_BIDS → handleUserBidHistory()")
    void userBids() throws Exception {
      when(bidController.handleUserBidHistory(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("USER_BIDS"), null);
      verify(bidController).handleUserBidHistory(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("BID_AUTO → handleAutoBid()")
    void bidAuto() throws Exception {
      when(bidController.handleAutoBid(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("BID_AUTO"), null);
      verify(bidController).handleAutoBid(any(), any(), eq(VALID_USER_ID));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // User actions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("User actions - chưa test")
  class UserActionsTest {

    @Test
    @DisplayName("USER_PURCHASE_HISTORY → handleGetPurchaseHistory()")
    void userPurchaseHistory() throws Exception {
      when(itemController.handleGetPurchaseHistory(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("USER_PURCHASE_HISTORY"), null);
      verify(itemController).handleGetPurchaseHistory(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("USER_DEPOSIT → handleDeposit()")
    void userDeposit() throws Exception {
      when(userController.handleDeposit(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("USER_DEPOSIT"), null);
      verify(userController).handleDeposit(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("USER_WITHDRAW → handleWithdraw()")
    void userWithdraw() throws Exception {
      when(userController.handleWithdraw(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("USER_WITHDRAW"), null);
      verify(userController).handleWithdraw(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("WALLET_GET_TRANSACTIONS → handleGetTransactions()")
    void walletGetTransactions() throws Exception {
      when(userController.handleGetTransactions(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("WALLET_GET_TRANSACTIONS"), null);
      verify(userController).handleGetTransactions(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("USER_CHANGE_PASSWORD → handleChangePassword()")
    void userChangePassword() throws Exception {
      when(userController.handleChangePassword(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("USER_CHANGE_PASSWORD"), null);
      verify(userController).handleChangePassword(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("USER_UPDATE_PROFILE → handleUpdateProfile()")
    void userUpdateProfile() throws Exception {
      when(userController.handleUpdateProfile(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("USER_UPDATE_PROFILE"), null);
      verify(userController).handleUpdateProfile(any(), any(), eq(VALID_USER_ID));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Admin actions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Admin actions - chưa test")
  class AdminActionsTest {

    @Test
    @DisplayName("ADMIN_DASHBOARD → handleDashboard()")
    void adminDashboard() throws Exception {
      when(adminController.handleDashboard(any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ADMIN_DASHBOARD"), null);
      verify(adminController).handleDashboard(any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("ADMIN_GET_USERS → handleGetUsers()")
    void adminGetUsers() throws Exception {
      when(adminController.handleGetUsers(any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ADMIN_GET_USERS"), null);
      verify(adminController).handleGetUsers(any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("ADMIN_UPDATE_USER_STATUS → handleUpdateUserStatus()")
    void adminUpdateUserStatus() throws Exception {
      when(adminController.handleUpdateUserStatus(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ADMIN_UPDATE_USER_STATUS"), null);
      verify(adminController).handleUpdateUserStatus(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("ADMIN_GET_AUCTIONS → handleGetAuctions()")
    void adminGetAuctions() throws Exception {
      when(adminController.handleGetAuctions(any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ADMIN_GET_AUCTIONS"), null);
      verify(adminController).handleGetAuctions(any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("ADMIN_CANCEL_AUCTION → handleCancelAuction()")
    void adminCancelAuction() throws Exception {
      when(adminController.handleCancelAuction(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("ADMIN_CANCEL_AUCTION"), null);
      verify(adminController).handleCancelAuction(any(), any(), eq(VALID_USER_ID));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Notification actions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Notification actions - chưa test")
  class NotifActionsTest {

    @Test
    @DisplayName("NOTIF_GET_ALL → handleGetAll()")
    void notifGetAll() throws Exception {
      when(notificationController.handleGetAll(any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("NOTIF_GET_ALL"), null);
      verify(notificationController).handleGetAll(any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("NOTIF_ALL alias → handleGetAll()")
    void notifAllAlias() throws Exception {
      when(notificationController.handleGetAll(any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("NOTIF_ALL"), null);
      verify(notificationController).handleGetAll(any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("NOTIF_MARK_READ → handleMarkRead()")
    void notifMarkRead() throws Exception {
      when(notificationController.handleMarkRead(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("NOTIF_MARK_READ"), null);
      verify(notificationController).handleMarkRead(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("NOTIF_DELETE → handleDelete()")
    void notifDelete() throws Exception {
      when(notificationController.handleDelete(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("NOTIF_DELETE"), null);
      verify(notificationController).handleDelete(any(), any(), eq(VALID_USER_ID));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Favorite actions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Favorite actions - chưa test")
  class FavoriteActionsTest {

    @Test
    @DisplayName("FAVORITE_LIST → handleGetList()")
    void favoriteList() throws Exception {
      when(favoriteController.handleGetList(any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("FAVORITE_LIST"), null);
      verify(favoriteController).handleGetList(any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("FAVORITE_ADD → handleAdd()")
    void favoriteAdd() throws Exception {
      when(favoriteController.handleAdd(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("FAVORITE_ADD"), null);
      verify(favoriteController).handleAdd(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("FAVORITE_REMOVE → handleRemove()")
    void favoriteRemove() throws Exception {
      when(favoriteController.handleRemove(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("FAVORITE_REMOVE"), null);
      verify(favoriteController).handleRemove(any(), any(), eq(VALID_USER_ID));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Rating actions
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Rating actions - chưa test")
  class RatingActionsTest {

    @Test
    @DisplayName("USER_RATE_SELLER → handleRateSeller()")
    void userRateSeller() throws Exception {
      when(ratingController.handleRateSeller(any(), any(), eq(VALID_USER_ID))).thenReturn(OK);
      dispatcher.dispatch(req("USER_RATE_SELLER"), null);
      verify(ratingController).handleRateSeller(any(), any(), eq(VALID_USER_ID));
    }

    @Test
    @DisplayName("USER_GET_SELLER_REVIEWS → handleGetSellerReviews()")
    void userGetSellerReviews() throws Exception {
      when(ratingController.handleGetSellerReviews(any(), any())).thenReturn(OK);
      dispatcher.dispatch(req("USER_GET_SELLER_REVIEWS"), null);
      verify(ratingController).handleGetSellerReviews(any(), any());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Token invalid → tất cả authenticated actions đều từ chối
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Token invalid - nhiều action")
  class TokenInvalidTest {

    @BeforeEach
    void override() {
      when(sessionManager.validateToken(any())).thenReturn(-1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ITEM_CREATE", "BID_PLACE", "USER_DEPOSIT", "ADMIN_DASHBOARD",
        "NOTIF_GET_ALL", "FAVORITE_ADD", "USER_RATE_SELLER",
        "ITEM_DELETE", "ITEM_UPDATE", "BID_AUTO", "USER_WITHDRAW",
        "WALLET_GET_TRANSACTIONS", "ADMIN_CANCEL_AUCTION"
    })
    @DisplayName("Token invalid → error 'Phiên đăng nhập không hợp lệ'")
    void tokenInvalidBlock(String action) {
      ServerResponse r = dispatcher.dispatch(req(action), null);
      assertFalse(r.isSuccess());
      assertTrue(r.getMessage().contains("Phiên đăng nhập")
          || r.getMessage().contains("hết hạn")
          || r.getMessage().contains("không hợp lệ"));
      verifyNoInteractions(itemController, bidController, userController,
          adminController, notificationController, favoriteController, ratingController);
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Controller ném exception → dispatcher bắt, trả error
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Controller exception → dispatcher bắt")
  class ControllerExceptionTest {

    @Test
    @DisplayName("ITEM_CREATE ném RuntimeException → trả error response")
    void itemCreateThrow() throws Exception {
      when(itemController.handleCreateItem(any(), any(), anyInt()))
          .thenThrow(new RuntimeException("DB sập"));
      ServerResponse r = dispatcher.dispatch(req("ITEM_CREATE"), null);
      assertFalse(r.isSuccess());
      assertTrue(r.getMessage().contains("Lỗi máy chủ") || r.getMessage().contains("DB sập"));
    }

    @Test
    @DisplayName("ADMIN_DASHBOARD ném Exception → trả error response")
    void adminDashboardThrow() throws Exception {
      when(adminController.handleDashboard(any(), anyInt()))
          .thenThrow(new RuntimeException("lỗi server"));
      ServerResponse r = dispatcher.dispatch(req("ADMIN_DASHBOARD"), null);
      assertFalse(r.isSuccess());
    }

    @Test
    @DisplayName("BID_PLACE ném Exception → action trong response = BID_PLACE")
    void bidPlaceThrowPreservesAction() throws Exception {
      when(bidController.handlePlaceBid(any(), any(), anyInt()))
          .thenThrow(new RuntimeException("timeout"));
      ServerResponse r = dispatcher.dispatch(req("BID_PLACE"), null);
      assertFalse(r.isSuccess());
      assertEquals("BID_PLACE", r.getAction());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // RequestParser.getAction ném → dispatcher bắt
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("Không có field action → throw exception")
  void missingActionField() {
    JsonObject r = new JsonObject();
    r.addProperty("token", "tok");
    r.addProperty("requestId", "req-missing-action");

    assertThrows(Exception.class, () -> dispatcher.dispatch(r, null));
  }
}