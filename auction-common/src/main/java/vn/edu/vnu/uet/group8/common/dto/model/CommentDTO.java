package vn.edu.vnu.uet.group8.common.dto.model;

import java.time.Instant;

public class CommentDTO {
  private int itemId;
  private int userId;
  private String username;
  private String content;
  private Instant createdAt;
  
  public CommentDTO(int itemId, int userId, String username, String content, Instant createdAt) {
    this.itemId = itemId;
    this.userId = userId;
    this.username = username;
    this.content = content;
    this.createdAt = createdAt;
  }

  public CommentDTO(String username, int userId, String content, Instant createdAt) {
    this.username = username;
    this.userId = userId;
    this.content = content;
    this.createdAt = createdAt;
  }

  public int getItemId() {
    return itemId;
  }

  public int getUserId() {
    return userId; 
  }

  public String getUsername() {
    return username;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
