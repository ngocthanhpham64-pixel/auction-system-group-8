package vn.edu.vnu.uet.group8.common.entity;

import java.io.Serializable; //Đây là thư viện để chuẩn hóa việc chuyển dữ liệu thành bit để di chuyển qua mạng
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID; // Thư viện này giúp ta tạo được id với 128 bit và chia làm 32 bit mỗi phần là tổng 4 phần khiến xác suất trùng lặp giữa 2 id là gần bằng 0

public abstract class Entity implements Serializable {
  /**
   * Lớp trừu tượng cơ sở cho tất cả các thực thể trong hệ thống.
   * Cung cấp các thuộc tính định danh và quản lý trạng thái cơ bản.
   */

  private static final long serialVersionUID = 1L; // Đây là mã để giúp sever biết khi gặp xung đột client gửi thông tin khác với sever thì máy chủ sẽ tự xử lý theo dữ liệu trong bản 1 này
  
  private String id;
  private LocalDateTime createdAt;
  private boolean isDeleted = false; // Này để tránh việc sau này khi xóa một vật thể nào đó không gây lỗi cho toàn bộ hệ thống còn lại (như xóa user thì không bị ảnh hưởng lịch sử giao dịch của toàn bộ server, chỉ thay đổi trạng thái không mất dữ liệu)


  /**
   * Khởi tạo thực thể mới với ID ngẫu nhiên và thời gian hiện tại.
   */
  public Entity() { // Constructor khởi tạo với người mới
    this.id = UUID.randomUUID().toString();
    this.createdAt = LocalDateTime.now();
    this.isDeleted = false;
  }

  /**
   * Khởi tạo thực thể với các giá trị cụ thể, thường dùng khi nạp dữ liệu từ database.
   */
  public Entity(String id, LocalDateTime createdAt, boolean isDeleted) { // Dùng cho trường hợp ta muốn khởi động lại toàn bộ hệ thống, toàn bộ dữ liệu cũ sẽ được nạp qua hàm này để lấy lại toàn bộ thông tin người dùng
    this.id = id;
    this.createdAt = createdAt;
    this.isDeleted = isDeleted;
  } 

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean deleted) {
    isDeleted = deleted;
  }


  // Các hàm dưới này Overwrite để sau dễ dùng
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
