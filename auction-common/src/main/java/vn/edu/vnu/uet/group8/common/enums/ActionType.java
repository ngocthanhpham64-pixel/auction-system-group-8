package vn.edu.vnu.uet.group8.common.enums;

import com.google.gson.annotations.SerializedName;

/**
 * Các loại hành động từ client gửi lên server.
 * Dùng GSON @SerializedName để tên JSON ngắn gọn.
 */
public enum ActionType {
    @SerializedName("login") LOGIN,
    @SerializedName("register") REGISTER,
    @SerializedName("logout") LOGOUT,
    @SerializedName("heartbeat") HEARTBEAT,
    @SerializedName("item_get_all") ITEM_GET_ALL,
    @SerializedName("item_get_detail") ITEM_GET_DETAIL,
    @SerializedName("bid_place") BID_PLACE,
    @SerializedName("bid_auto") BID_AUTO,
    @SerializedName("bid_history") BID_HISTORY,
    @SerializedName("user_profile") USER_PROFILE,
    @SerializedName("user_deposit") USER_DEPOSIT,
    @SerializedName("user_bids") USER_BIDS,
    @SerializedName("notif_all") NOTIF_GET_ALL,
    @SerializedName("notif_read") NOTIF_MARK_READ,
    @SerializedName("fav_list") FAVORITE_LIST,
    @SerializedName("fav_add") FAVORITE_ADD,
    @SerializedName("fav_remove") FAVORITE_REMOVE,
    @SerializedName("item_create")      ITEM_CREATE,
    @SerializedName("item_update")      ITEM_UPDATE,
    @SerializedName("item_delete")      ITEM_DELETE,
    @SerializedName("item_my_listings") ITEM_MY_LISTINGS,
    @SerializedName("user_change_password") USER_CHANGE_PASSWORD,
    @SerializedName("user_withdraw")       USER_WITHDRAW,
    @SerializedName("wallet_get_transactions") WALLET_GET_TRANSACTIONS, 
    @SerializedName("notif_delete")    NOTIF_DELETE,
    @SerializedName("admin_dashboard") ADMIN_DASHBOARD,
    @SerializedName("admin_get_users") ADMIN_GET_USERS,
    @SerializedName("admin_update_user_status") ADMIN_UPDATE_USER_STATUS,
    @SerializedName("admin_get_auctions") ADMIN_GET_AUCTIONS,
    @SerializedName("admin_cancel_auction") ADMIN_CANCEL_AUCTION,
    @SerializedName("user_update_profile") USER_UPDATE_PROFILE,
    @SerializedName("auth_request_otp") AUTH_REQUEST_OTP,
    @SerializedName("auth_reset_password") AUTH_RESET_PASSWORD,
    @SerializedName("user_rate_seller") USER_RATE_SELLER,
    @SerializedName("user_get_seller_reviews") USER_GET_SELLER_REVIEWS,
    @SerializedName("item_comment") ITEM_COMMENT,
    @SerializedName("user_get_seller_comments") USER_GET_SELLER_COMMENTS,
    @SerializedName("user_purchase_history") USER_PURCHASE_HISTORY;

    // @SerializedName("chat_send_message") CHAT_SEND_MESSAGE; // Chat (nếu triển khai)
}