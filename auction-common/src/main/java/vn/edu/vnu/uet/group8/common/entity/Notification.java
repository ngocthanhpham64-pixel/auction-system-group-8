package vn.edu.vnu.uet.group8.common.entity;

import java.time.Instant;

import vn.edu.vnu.uet.group8.common.enums.NotificationType;

/**
 * Entity đại diện cho một thông báo lưu trong Database.
 */
public final class Notification extends Entity {

  // ── Immutable sau khi tạo ────────────────────────────
  private final int userId;
  private final String title;
  private final String message;
  private final NotificationType type;
  
  // ── Mutable có kiểm soát ─────────────────────────────
  private boolean isRead;

  /** 
   * Dùng cho việc load dữ liệu từ DB lên Memory 
   */
  private Notification(int id, Instant createdAt, boolean isDeleted,
                       int userId, String title, String message, 
                       NotificationType type, boolean isRead) {
    super(id, createdAt, isDeleted);
    this.userId = userId;
    this.title = title;
    this.message = message;
    this.type = type;
    this.isRead = isRead;
  }

  /** 
   * Dùng cho Builder tạo mới đối tượng 
   */
  private Notification(Builder b) {
    super(0, Instant.now(), false);
    this.userId = b.userId;
    this.title = b.title;
    this.message = b.message;
    this.type = b.type;
    this.isRead = false;
  }

  public int getUserId() { return userId; }
  public String getTitle() { return title; }
  public String getMessage() { return message; }
  public NotificationType getType() { return type; }
  public boolean isRead() { return isRead; }

  public void markAsRead() { this.isRead = true; }

  public static Notification reconstruct(Reconstruct r) {
    return new Notification(r.id, r.createdAt, r.isDeleted, r.userId, r.title, r.message, r.type, r.isRead);
  }

  public Reconstruct reconstructor() {
    return new Reconstruct();
  }

  // ════════════════════════════════════════════════════
  // RECONSTRUCTOR
  // ════════════════════════════════════════════════════
  public static class Reconstruct {
    private int id;
    private Instant createdAt;
    private boolean isDeleted;
    private int userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;

    public Reconstruct id(int id) { this.id = id; return this; }
    public Reconstruct createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Reconstruct isDeleted(boolean isDeleted) { this.isDeleted = isDeleted; return this; }
    public Reconstruct userId(int userId) { this.userId = userId; return this; }
    public Reconstruct title(String title) { this.title = title; return this; }
    public Reconstruct message(String message) { this.message = message; return this; }
    public Reconstruct type(NotificationType type) { this.type = type; return this; }
    public Reconstruct isRead(boolean isRead) { this.isRead = isRead; return this; }

    public Notification build() {
      return Notification.reconstruct(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  // ════════════════════════════════════════════════════
  // BUILDER
  // ════════════════════════════════════════════════════
  public static class Builder {
    private int userId;
    private String title;
    private String message;
    private NotificationType type;

    public Builder userId(int userId) { this.userId = userId; return this; }
    public Builder title(String title) { this.title = title; return this; }
    public Builder message(String message) { this.message = message; return this; }
    public Builder type(NotificationType type) { this.type = type; return this; }

    public Notification build() {
      return new Notification(this);
    } 
  }
}
