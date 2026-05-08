package vn.edu.vnu.uet.group8.common.dto.response;

import java.time.Instant;

import com.google.gson.Gson;

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
    private final String eventType;// Dùng cho broadcast
    private final Instant timestamp;
    private final String requestId; // sao chép lại requestId từ request gốc. CLient gửi request có requestId, server trả về response với cùng resquestId để client biết response này dành chp request nào
    private final boolean success; // kiểm tra thao tác thành công hay không?
    private final String message; // Thông báo mô tả kết quả, dành cho người dùng hoặc log
    private final Object data; // Dữ liệu trả về cụ thể, thay đổi theo từng loại request

    private ServerResponse(Builder b) {
        this.action = b.action;
        this.eventType = b.eventType;
        this.timestamp = (b.timestamp != null) ? b.timestamp : Instant.now();
        this.requestId = b.requestId;
        this.success = b.success;
        this.message = b.message;
        this.data = b.data;
    }

    // Getters
    public String getAction()      { return action; }
    public String getEventType()   { return eventType; }
    public Instant getTimestamp()  { return timestamp; }
    public String getRequestId()   { return requestId; }
    public boolean isSuccess()     { return success; }
    public String getMessage()     { return message; }
    public Object getData()        { return data; }

    /**
     * Ép kiểu an toàn cho data
     * Nếu data chưa qua serialize( do server trả về trực tiếp) thì dùng type.cast().
     * Nếu data đã qua Gson deserialize( thường LinkedTreeMap) thì convert ngược về đúng kiểu bằng Gson.
     *
     * @param type Class của kiểu mong muốn
     * @return Đối tượng được ép kiểu, hoặc null nếu data là null
     */
    public <T> T getData(Class<T> type) {
        if (data == null) return null;
        if (type.isInstance(data)) {
            return type.cast(data);
        }
        // Data là dạng Map(LinkedTreeMap) sau khi Gson parse->convert lai
        return GSON.fromJson(GSON.toJson(data), type);
    }

    /**
     * return true nếu đây là server push(broadcast)
     */
    public boolean isBroadcast() {
        return requestId == null && eventType != null;
    }
    // Static Factory methods

    /**
     * Tạo Builder cho reply( có requestId).
     */
    public static Builder reply(String action, String resquestId) {
        return new Builder(action).requestId(resquestId);
    }

    /**
     * Reply lỗi đơn giản, không có data.
     */
    public static ServerResponse replyError(String action, String requestId, String errorMessage) {
        return reply(action, requestId)
                .success(false)
                .message(errorMessage)
                .build();
    }

    /**
     * Reply lỗi có kèm dữ liệu chi tiết
     */
    public static ServerResponse replyError(String action, String requestId, String errorMessage, Object errorDetail) {
        return reply(action, requestId)
                .success(false)
                .message(errorMessage)
                .data(errorDetail)
                .build();
    }

    /**
     * Tạo Builder cho broadcast. Action mặc định là "BROADCAST"
     */
    public static Builder broadcast(String eventType) {
        return new Builder("BROADCAST").eventType(eventType);
    }

    // Builder
    public static class Builder {
        private final String action;
        private String eventType;
        private Instant timestamp;
        private String requestId;
        private boolean success = true;
        private String message;
        private Object data;

        private Builder(String action) {
                if (action == null || action.isBlank()) {
                    throw new IllegalArgumentException("Action không được trống");
                }
                this.action = action.trim().toUpperCase();
            }
        public Builder success(boolean success)   { this.success = success; return this; }
        public Builder message(String message)    { this.message = message; return this; }
        public Builder data(Object data)          { this.data = data; return this; }
        public Builder requestId(String requestId){ this.requestId = requestId; return this; }
        public Builder eventType(String eventType){ this.eventType = eventType; return this; }
        public Builder timestamp(Instant timestamp){ this.timestamp = timestamp; return this; }


        public ServerResponse build() {
                // Đảm bảo 1 trong hai chế độ
                if (requestId != null && eventType != null) {
                    throw new IllegalStateException("Không thể vừa là reply (có requestId) vừa là broadcast (có eventType)");
                }
                if (requestId == null && eventType == null) {
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
    }
}
