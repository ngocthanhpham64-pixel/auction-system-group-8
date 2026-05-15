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
    @SerializedName("item_all") ITEM_GET_ALL,
    @SerializedName("item_detail") ITEM_GET_DETAIL,
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
    @SerializedName("admin_dashboard") ADMIN_DASHBOARD,
    @SerializedName("admin_get_users") ADMIN_GET_USERS,
    @SerializedName("admin_update_user_status") ADMIN_UPDATE_USER_STATUS,
    @SerializedName("admin_get_auctions") ADMIN_GET_AUCTIONS,
    @SerializedName("admin_cancel_auction") ADMIN_CANCEL_AUCTION,
}