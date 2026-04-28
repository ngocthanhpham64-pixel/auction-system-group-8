package vn.edu.vnu.uet.group8.common.dto;


import vn.edu.vnu.uet.group8.common.enums.ActionType;

import java.util.Locale;
import java.util.UUID;

public final class ServerRequest {
    private final String requestId;// Mã định danh duy nhất cho mỗi request(dùng UUID tạo tự động)
    private final ActionType action;// Hành động cần thực hiện: LOGIN,BID_PLACE,..
    private final int userId;// Id của người dùng hiện tại(sau khi đăng nhập). Dùng để kiểm tra quyền và biết ai đang gửi
    private final String token;// Mã xác thực(session token). Server có thể kiểm tra token này để đảm bảo request hợp lệ,tránh giả mạo
    private final Object payload;// Dữ liệu nghiệp vụ cụ thể. Ví dụ: khi action = LOGIN, payload là object LoginRequest chứa username/password
    // Private constructor dùng Builder
    private ServerRequest(Builder b) {
        this.requestId = b.requestId;
        this.action = b.action;
        this.userId = b.userId;
        this.token = b.token;
        this.payload = b.payload;
    }
    // Hàm getter (cần thiết để lấy dữ liệu)
    public String getRequestId(){ return requestId;}
    public ActionType getAction(){ return action;}
    public int getUserId(){ return userId;}
    public String getToken(){ return token;}
    public Object getPayload(){ return payload;}

    public static Builder builder(ActionType action){ return new Builder(action);}

    public static class Builder {
        private String requestId = UUID.randomUUID().toString();// tự sinh UUID nếu không set
        private final ActionType action;// bắt buộc từ constructor
        private int userId = 0;
        private String token = "";
        private Object payload = null;

        private Builder(ActionType action){this.action = action;}
        // Mỗi phương thức nhận giá trị, gán vào field của Builder, rồi return this để có thể nối tiếp nhau
        public Builder requestId(String id){ this.requestId = id; return this;}
        public Builder userId (int id){ this.userId = id; return this;}
        public Builder token(String t){ this.token = t;return this;}
        public Builder payload(Object p){ this.payload = p;return this;}
        // Gọi constructor private ServerRequest(Builder b) để tạo object ServerRequest bất biến
        public ServerRequest build(){ return new ServerRequest(this);}
    }
}
// Class ServerRequest: là một DTO immutable, đóng gói toàn bộ thông tin cần thiết cho một request từ client đến server
// - Vai trò: Chuẩn hóa giao tiếp, giúp server biết ai gửi(userId,token) muốn gì(action), và dữ liệu đi kèm theo(payload)