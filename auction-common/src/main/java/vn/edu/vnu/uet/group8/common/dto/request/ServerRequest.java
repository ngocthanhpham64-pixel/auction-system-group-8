package vn.edu.vnu.uet.group8.common.dto.request;

import java.time.Instant;
import java.util.UUID;

import vn.edu.vnu.uet.group8.common.enums.ActionType;

/**
 * Yêu cầu từ client-> server
 * Immutable, dùng Builder với Generic payload để GSON serialize an toàn
 * -Hai loại request:
 *  Anonymous:userId=null, token = null-dùng cho LOGIN, REGISTER
 *  Authenticated: userId >0, token != null-mọi action sau khi đăng nhập
 * @ param <T> Kiểu dữ liệu của payload
 */

public final class ServerRequest <T>{
    private final String requestId;// Mã định danh duy nhất cho mỗi request(dùng UUID tạo tự động)
    private final ActionType action;// Hành động cần thực hiện: LOGIN,BID_PLACE,..
    private final Integer userId;// Id của người dùng hiện tại(sau khi đăng nhập). Dùng để kiểm tra quyền và biết ai đang gửi
    private final String token;// Mã xác thực(session token). Server có thể kiểm tra token này để đảm bảo request hợp lệ,tránh giả mạo
    private final T payload;// Dữ liệu nghiệp vụ cụ thể. Ví dụ: khi action = LOGIN, payload là object LoginRequest chứa username/password
    private final Instant timestamp;
    // Private constructor dùng Builder
    private ServerRequest(Builder<T> b) {
        this.requestId = b.requestId;
        this.action = b.action;
        this.userId = b.userId;
        this.token = b.token;
        this.payload = b.payload;
        this.timestamp = b.timestamp;
    }
    // Hàm getter (cần thiết để lấy dữ liệu)
    public String getRequestId(){ return requestId;}
    public ActionType getAction(){ return action;}
    public Integer getUserId(){ return userId;}
    public String getToken(){ return token;}
    public T getPayload(){ return payload;}
    public Instant getTimestamp(){ return timestamp;}
    //Helper methods

    /**
     *
     * @return true nếu request đến từ user đã đăng nhập(có userId và regist token hợp lệ)
     */
    public boolean isAuthenticated(){
        return userId != null && userId>0 && token != null;
    }
    /**
     * @return true nếu request không có token(LOGIN,REGISTER)
     */
    public boolean isAnonymous(){
        return userId == null && token == null;
    }
    // Static Factory Methods
    /** Builder với action bắt buộc.*/
    public static <T> Builder <T> builder(ActionType action){ return new Builder<>(action);}

    /**
     * Request đã xác thực, không cần payload
     * Dùng cho: GET_PROFILE, LOGOUT,....
     */
    public static ServerRequest<Void> authenticated(
            ActionType action, int userId, String token){
        return new Builder<Void> (action)
                .userId(userId)
                .token(token)
                .build();
    }

    /**
     * Request chưa đăng nhập, không cần payload.
     * Dùng cho: PING, HEALTH_CHECK..
     */
    public static ServerRequest<Void> anonymous(ActionType action){
        return new Builder<Void>(action).build();
    }
    public static class Builder<T> {
        private String requestId = UUID.randomUUID().toString();// tự sinh UUID nếu không set
        private final ActionType action;// bắt buộc từ constructor
        private Integer userId = null;
        private String token = null;
        private T payload;
        private Instant timestamp;

        private Builder(ActionType action){
            if(action == null){
                throw new IllegalArgumentException("ActionType không được null");
            }
            this.action = action;}
        // Mỗi phương thức nhận giá trị, gán vào field của Builder, rồi return this để có thể nối tiếp nhau
        public Builder<T> requestId(String id) {
            if(id != null && !id.isBlank()){
                this.requestId = id.trim();
            }
            return this;
        }
        public Builder<T> userId (int id){ this.userId = id; return this;}
        public Builder<T> token(String token){
            if(token != null && !token.isBlank()){
                this.token = token.trim();
            } else{
                this.token = null;
            }
            return this;
        }
        public Builder<T> payload(T payload){ this.payload = payload;return this;}
        // Gọi constructor private ServerRequest(Builder b) để tạo object ServerRequest bất biến
        public ServerRequest<T> build(){
            if (requestId == null){
                requestId = UUID.randomUUID().toString();
            }
            if(timestamp == null){
                timestamp = Instant.now();
            }
            return new ServerRequest<>(this);
        }
    }
}
// Class ServerRequest: là một DTO immutable, đóng gói toàn bộ thông tin cần thiết cho một request từ client đến server
// - Vai trò: Chuẩn hóa giao tiếp, giúp server biết ai gửi(userId,token) muốn gì(action), và dữ liệu đi kèm theo(payload)