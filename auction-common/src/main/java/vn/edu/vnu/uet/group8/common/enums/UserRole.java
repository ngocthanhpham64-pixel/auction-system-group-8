package vn.edu.vnu.uet.group8.common.enums;
import com.google.gson.annotations.SerializedName;
public enum UserRole {
    @SerializedName("member")   MEMBER,
    @SerializedName("seller")   SELLER,
    @SerializedName("admin")    ADMIN;
}
