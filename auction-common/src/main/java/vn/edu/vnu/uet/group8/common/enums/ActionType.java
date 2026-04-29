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
    @SerializedName("fav_remove") FAVORITE_REMOVE
}