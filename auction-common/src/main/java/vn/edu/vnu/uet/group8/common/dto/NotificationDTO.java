package vn.edu.vnu.uet.group8.common.dto;

import java.time.Instant;

/**
 * DTO (Data Transfer Object) cho thông báo trong hệ thống đấu giá.
 * Dùng để truyền dữ liệu thông báo giữa Client và Server.
 */
public final class NotificationDTO {
    private final int id; // Mã định danh duy nhất của thông báo(từ database)
    private final int userId;// Id của người dùng nhận thông báo. Dùng để biết thông báo này dành cho bạn
    private final String title; // Tiêu đề thông báo
    private final String message; // Nội dung chi tiết của thông báo
    private final boolean isRead; // Đánh dấu đã đọc hay chưa(true = đã đọc và ngược lại)
    private final Instant createdAt; // Thời điểm tạo thông báo(UTC). So sánh, hiển thị dễ dàng
    private final String type;// Loại thông báo giúp client xử lý hiển thị khác nhau

    private NotificationDTO(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.title = builder.title;
        this.message = builder.message;
        this.isRead = builder.isRead;
        this.createdAt = builder.createdAt;
        this.type = builder.type;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }
    public Instant getCreatedAt() { return createdAt; }
    public String getType() { return type; }
    // Client sau khi nhận NotificationDTO sẽ gọi các getter này để lấy dữ liệu hiển thị lên giao diện
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int id;
        private int userId;
        private String title;
        private String message;
        private boolean isRead;
        private Instant createdAt;
        private String type;

        private Builder() {} // chỉ được gọi từ bên trong, ngăn chặn việc tạo builder 1 cách tùy tiện

        public Builder id(int id) { this.id = id; return this; }
        public Builder userId(int userId) { this.userId = userId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder isRead(boolean isRead) { this.isRead = isRead; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder type(String type) { this.type = type; return this; }

        public NotificationDTO build() {
            return new NotificationDTO(this);
        }
    }
}