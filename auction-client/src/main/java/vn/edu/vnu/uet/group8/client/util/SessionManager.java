package vn.edu.vnu.uet.group8.client.util;

import javafx.application.Platform;
import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;

/**
 * Quản lý token xác thực và thông tin phiên đăng nhập.
 * -Thread-safe: sử dụng volatile fields
 * -Logout tự động chuyển về FX Thread để update UI
 * -Cung cấp tiện ích: isLoggedIn(),isAdmin(),getAvatarText()
 */
public final class SessionManager {
    private static volatile String authToken;
    private static volatile int userId;
    private static volatile  String username ="";
    private static volatile String fullName = "";
    private static volatile String role = "";
    private static volatile boolean adminFlag;
    private static volatile String adminLevel;

    private SessionManager(){}// không cho tạo instance

    // ------------Cập nhật phiên--------------

    /**
     * Lưu toàn bộ thông tin phiên sau khi đăng nhập thành công.
     * @param token auth token
     * @param uid userID
     * @param uname tên đăng nhập(username)
     * @param fname họ và tên(có thể null)
     * @param userRole vai trò(MEMBER, ADMIN,..)
     */
    public static void setSession(String token,int uid,String uname,String fname,String userRole){
        authToken = token;
        userId = uid;
        username = uname != null ? uname :"";
        fullName = fname != null ? fname :"";
        role = userRole != null ? userRole :"";
        // Tự động xác định quyền admin
        updateAdminFlags(userRole);
    }

    /**
     * Dựa vào role string từ server để set adminFlag v adminLevel.
     * "SUPER_ADMIN" -> super admin, "ADMIN" -> admin thường (moderator)
     */
    private static void updateAdminFlags(String roleStr){
        if ("SUPER_ADMIN".equalsIgnoreCase(roleStr)) {
            adminFlag = true;
            adminLevel = "SUPER_ADMIN";
        } else if ("ADMIN".equalsIgnoreCase(roleStr) || "MODERATOR".equalsIgnoreCase(roleStr)) {
            adminFlag = true;
            adminLevel = "MODERATOR"; // admin thường
        } else {
            adminFlag = false;
            adminLevel = null;
        }
    }
    public static void setAdmin(boolean admin, String level) {
        adminFlag = admin;
        adminLevel = level;
    }
    /**
     * Xóa toàn bộ thông tin phiên(không ngắt kết nối).*/
    public static void clearSession() {
        authToken = null;
        userId = 0;
        username = "";
        fullName = "";
        role = "";
        adminFlag = false;
        adminLevel = null;
    }
    //----------Đăng nhập------------
    /** Đăng xuất: xóa session, làm sạch model, ngắt kết nối, quay về màn hình login.*/
    public static void logout(){
        AuctionClient.getInstance().disconnect();
        clearSession();
        ClientModel.getInstance().clearSession();
        Platform.runLater(()->SceneManager.switchTo(SceneManager.VIEW_LOGIN));
    }
    //-----------Getter----------
    public static String getAuthToken(){ return authToken;}
    public static int getUserId(){ return userId;}
    public static String getUsername(){ return username;}
    public static String getFullName(){ return fullName;}
    public static String getRole(){ return role;}

    /** Kiểm tra đã đăng nhập hay chưa (dựa vào token).*/
    public static boolean isLoggedIn(){
        return authToken != null && !authToken.isEmpty();
    }
    /** Kiểm tra người dùng hiện tại có quyền admin hay không.*/
    public static boolean isAdmin(){
        return adminFlag;
    }

    public static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equalsIgnoreCase(adminLevel);
    }

    public static boolean isModerator() {
        return "MODERATOR".equalsIgnoreCase(adminLevel);}
    /**
     * Lấy 1-2 ký tự đầu của tên để hiện thị avatar badge.
     * -Nếu có fullName: lấy chữ cái đầu của từ đầu và từ cuối(vd:Nguyễn Văn A->"NA")
     * -Nếu không: lấy username, lấy 2 ký tự đầu (vd: alice->"AL")
     * -Nếu rỗng: trả về "?"
     */
    public static String getAvatarText(){
        String name = (fullName != null && !fullName.isBlank()) ? fullName : username;
        if(name.isBlank()) return "?";
        String [] parts = name.trim().split("\\s+");
        if(parts.length>=2){
            char firstChar = parts[0].charAt(0);
            char lastChar = parts[parts.length -1].charAt(0);
            return (firstChar + "" + lastChar).toUpperCase();
        } else{
            return name.substring(0,Math.min(2,name.length())).toUpperCase();
        }
    }
}
