package vn.edu.vnu.uet.group8.common.entity;

import  java.time.Instant;

public class Comment extends Entity {
  private int item_id;
  private int user_id;
  private String username;
  private String content;
  
  public Comment(int item_id, int user_id, String username, String content) {
    super(0, Instant.now(), false);
    this.item_id = item_id;
    this.user_id = user_id;
    this.username = username;
    this.content = content;
  }

  public int getItemId() {
    return item_id;
  }

  public int getUserId() {
    return user_id; 
  }

  public String getUsername() {
    return username;
  }

  public String getContent() {
    return content;
  }
}
