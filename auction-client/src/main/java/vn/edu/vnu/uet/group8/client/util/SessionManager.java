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
    }

    /**
     * Xóa toàn bộ thông tin phiên(không ngắt kết nối).*/
    public static void clearSession() {
        authToken = null;
        userId = 0;
        username = "";
        fullName = "";
        role = "";
    }
    //----------Đăng nhập------------
    /** Đăng xuất: xóa session, làm sạch model, ngắt kết nối, quay về màn hình login.*/
    public static void logout(){
        clearSession();
        ClientModel.getInstance().clearSession();
        AuctionClient.getInstance().disconnect();
        Platform.runLater(()->SceneManager.switchTo("login.fxml"));
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
        return "ADMIN".equalsIgnoreCase(role);
    }
    /**
     * Lấy 1-2 ký tự đầu của tên để hiện thị avatar badge.
     * -Nếu có fullName: lấy chữ cái đầu của từ đầu và từ cuối(vd:Nguyễn Văn A->"NA")
     * -Nếu không: lấy username, lấy 2 ký tự đầu (vd: alice->"AL")
     * -Nếu rỗng: trả về "?"
     */
    public static String getAvatarText(){
        String name = fullName.isBlank() ? username : fullName;
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
