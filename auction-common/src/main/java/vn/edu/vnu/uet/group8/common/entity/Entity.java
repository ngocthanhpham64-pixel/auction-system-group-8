package vn.edu.vnu.uet.group8.common.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * Lớp trừu tượng cơ sở — Passive Entity.
 *
 * Không tự sinh ID, không tự gán thời gian.
 * Mọi giá trị đều đến từ bên ngoài:
 *   - Tạo mới  → Service/Factory truyền vào (id = 0, chờ DB)
 *   - Load DB  → DAO.mapRow() truyền vào (id = giá trị thật từ DB)
 */
public abstract class Entity {
  // int thay vì String UUID
  // 0 = chưa được lưu vào DB (transient state), nghĩa là trong DB tự tạo id theo int
  private int id;
  private Instant createdAt;
  private boolean isDeleted = false; // Này để tránh việc sau này khi xóa một vật thể nào đó không gây lỗi cho toàn bộ hệ thống còn lại (như xóa user thì không bị ảnh hưởng lịch sử giao dịch của toàn bộ server, chỉ thay đổi trạng thái không mất dữ liệu)

  // ════════════════════════════════════════════════════
  // Chỉ có MỘT constructor — nhận tất cả từ bên ngoài
  // Không có constructor rỗng, không tự sinh UUID
  // ════════════════════════════════════════════════════

  /**
   * Dùng cho cả hai trường hợp:
   *
   * TẠO MỚI (chưa có trong DB):
   *   id        = 0          ← quy ước "chưa có ID"
   *   createdAt = Instant.now() ← Service truyền vào
   *   isDeleted = false
   *
   * LOAD TỪ DB (đã có trong DB):
   *   id        = giá trị thật từ ResultSet
   *   createdAt = giá trị thật từ ResultSet
   *   isDeleted = giá trị thật từ ResultSet
   */
  protected Entity(int id, Instant createdAt, boolean isDeleted) {
    this.id        = id;
    this.createdAt = Objects.requireNonNull(createdAt,
        "createdAt không được null");
    this.isDeleted = isDeleted;
  }

  // ════════════════════════════════════════════════════
  // ID MANAGEMENT
  // ════════════════════════════════════════════════════
  
  /**
   * Gọi MỘT LẦN DUY NHẤT sau khi INSERT thành công.
   * DAO lấy generated key từ DB rồi gọi method này.
   *
   * Sau khi set xong, không cho phép đổi nữa.
   */
  public void assignId(int generatedId) {
    if (this.id != 0)
      throw new IllegalStateException(
        "ID đã được gán, không thể gán lại. "
        + "id hiện tại = " + this.id);
    if (generatedId <= 0)
      throw new IllegalArgumentException(
        "ID từ DB phải là số nguyên dương");
    this.id = generatedId;
  }

  /** Kiểm tra xem đã được lưu chưa */
  public boolean isPersisted() {
    return id > 0;
  }

  // ════════════════════════════════════════════════════
  // GETTERS + SOFT DELETE
  // ════════════════════════════════════════════════════

  public int getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  // 
  public void markAsDeleted() {
    this.isDeleted = true;
  }

  public void restore() {
    this.isDeleted = false;
  }
  
  // ════════════════════════════════════════════════════
  // OVERRIDE
  // ════════════════════════════════════════════════════

  @Override 
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    else if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    else {
        Entity entity = (Entity) o;
        if (Objects.equals(this.getId(), entity.getId())) {
          return true;
        }
    }
    return false;
  }

  @Override
  public int hashCode() { // Dùng để sau này dễ tìm kiếm từ UUID, tốc độ gần như O(1) trong điều kiện thuận lợi nhất
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Entity{" +
            "id='" + id + '\'' +
            ", createdAt=" + createdAt +
            ", isDeleted=" + isDeleted +
            '}';
  }
}
