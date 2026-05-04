package vn.edu.vnu.uet.group8.common.dto;

import com.google.gson.Gson;
<<<<<<< Updated upstream
import java.time.Instant;
=======
>>>>>>> Stashed changes
/**
 * Phản hồi từ server -> client. Hai chế độ:
 * - Reply(requestId != null), thường có action cụ thể
 * - Broadcast:(eventType != null), server push đến nhiều client.
 * Dữ liệu kèm theo (data) được lưu dưới dạng Object Java để tương thích với GSON mà không cần thoogn tin kiểu generic lúc parse
 * Client có thể ép kiểu an toàn qua healper(getData(Class))
 */

public final class ServerResponse {
    private static final Gson GSON = new Gson();// Dùng cho getData, thread-safe
    private final String action;
    private final String eventType;
    private final Instant timestamp;
    private final String requestId; // sao chép lại requestId từ request gốc. CLient gửi request có requestId, server trả về response với cùng resquestId để client biết response này dành chp request nào
    private final boolean success; // kiểm tra thao tác thành công hay không?
    private final String message; // Thông báo mô tả kết quả, dành cho người dùng hoặc log
    private final Object data; // Dữ liệu trả về cụ thể, thay đổi theo từng loại request
    private final String eventType; // thêm mới dùng cho broadcast

    private static final Gson gson = new Gson();

    private ServerResponse(Builder b){
        this.action = b.action;
        this.eventType = b.eventType;
        this.timestamp = b.timestamp != null ? b.timestamp : Instant.now();
        this.requestId = b.requestId;
        this.success = b.success;
        this.message = b.message;
        this.data = b.data;
<<<<<<< Updated upstream

    }
// Getters
    public String getAction(){ return action;}
    public String getEventType(){ return eventType;}
    public Instant getTimestamp(){ return timestamp;}
    public String getResquestId(){ return requestId;}
=======
        this.eventType = b.eventType;
    }

    public String getRequestId(){ return requestId;}
>>>>>>> Stashed changes
    public boolean isSuccess(){ return success;}
    public String getMessage(){ return message;}
    public Object getData(){ return data;}
    public String getEventType(){ return eventType;}

    /**
<<<<<<< Updated upstream
     * Ép kiểu an toàn cho data
     * Nếu data chưa qua serialize( do server trả về trực tiếp) thì dùng type.cast().
     * Nếu data đã qua Gson deserialize( thường LinkedTreeMap) thì convert ngược về đúng kiểu bằng Gson.
     * @param type Class của kiểu mong muốn
     * @return Đối tượng được ép kiểu, hoặc null nếu data là null
     */
    public <T> T getData(Class<T> type){
        if(data == null) return null;
        if(type.isInstance(data)){
            return type.cast(data);
        }
        // Data là dạng Map(LinkedTreeMap) sau khi Gson parse->convert lai
        return GSON.fromJson(GSON.toJson(data),type);
    }
    /** return true nếu đây là server push(broadcast)*/
    public boolean isBroadcast(){
        return requestId == null && eventType != null;
    }
    // Static Factory methods
    /** Tạo Builder cho reply( có requestId).*/
    public static Builder reply(String action,String resquestId){
        return new Builder(action).requestId(resquestId);
    }
    /** Reply lỗi đơn giản, không có data.*/
    public static ServerResponse replyError(String action, String requestId, String errorMessage){
        return reply(action,requestId)
                .success(false)
                .message(errorMessage)
                .build();
    }
    /** Reply lỗi có kèm dữ liệu chi tiết */
    public static ServerResponse replyError(String action,String requestId,String errorMessage,Object errorDetail){
        return reply(action, requestId)
                .success(false)
                .message(errorMessage)
                .data(errorDetail)
                .build();
    }
    /** Tạo Builder cho broadcast. Action mặc định là "BROADCAST"*/
    public static Builder broadcast(String eventType){
        return new Builder("BROADCAST").eventType(eventType);
    }
    // Builder
    public static class Builder {
        private final String action;
        private String eventType;
        private Instant timestamp;
        private String requestId;
        private boolean success = true;
=======
     * Lấy dữ liệu dưới dạng đối tượng của lớp chỉ định.
     * Phương thức này an toàn với kết quả từ Gson( khi data là LinkedTreeMap).
     * @param clazz lớp đích(ví dụ AuctionStatusDTO.class)
     * @param <T> kiểu trả về
     * @return đối tượng đã được chuyển đổi, hoặc null nếu data null
     */
    public <T> T getDataAs(Class<T> clazz){
        if(data == null) return null;
        // Nếu data đã là đúng kiểu, chỉ cần cast
        if(clazz.isInstance(data)) return clazz.cast(data);
        // Nếu không, dùng Gson để chuyển đổi(xử lý LinkedMapTree)
        String json = gson.toJson(data);
        return gson.fromJson(json,clazz);
    }

    /**
     * Tạo ột ServerResponse mô phỏng timeout ( dùng cho callback hết hạn).
     * @param requestId requestId của yêu cầu bị timeout.
     * @return ServerResponse với success = false,message="Request timeout".
     */

    public static ServerResponse timeout(String requestId){
        return ServerResponse.builder(requestId)
                .success(false)
                .message("Request timeout")
                .eventType("Timeout")
                .build();
    }
    public static Builder builder(String requestId){ return new Builder(requestId);}

    public static class Builder{
        private final String requestId;
        private boolean success;
>>>>>>> Stashed changes
        private String message;
        private Object data;
        private String eventType;

<<<<<<< Updated upstream
        private Builder(String action) {
            if (action == null || action.isBlank()) {
                throw new IllegalArgumentException("Action không được trống");
            }
            this.action = action.trim().toUpperCase();
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        public ServerResponse build(){
            // Đảm bảo 1 trong hai chế độ
            if(requestId != null && eventType != null){
                throw new IllegalStateException("Không thể vừa là reply (có requestId) vừa là broadcast (có eventType)");
            }
            if(requestId == null && eventType == null){
                throw new IllegalStateException("Phải chỉ định requestId (reply) hoặc eventType (broadcast)");
            }
            return new ServerResponse(this);
        }
    }
    // Debug
    @Override
    public String toString() {
        return "ServerResponse{" +
                "action='" + action + '\'' +
                ", success=" + success +
                ", requestId='" + requestId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
=======
        private Builder(String requestId){ this.requestId = requestId;}
        public Builder success(boolean s){ this.success = s;return this;}
        public Builder message(String m){ this.message = m; return this;}
        public Builder data( Object d){ this.data = d; return this;}
        public Builder eventType(String e) {this.eventType = e;return this;}
        public ServerResponse build(){ return new ServerResponse(this);}
>>>>>>> Stashed changes
    }
}
