package vn.edu.vnu.uet.group8.common.dto.model;

import java.time.Instant;

public class ReviewDTO {
  private String raterUsername;
  private int score;
  private String comment;
  private Instant createdAt;

  public ReviewDTO() {}

  public ReviewDTO(String raterUsername, int score, String comment, Instant createdAt) {
    this.raterUsername = raterUsername;
    this.score = score;
    this.comment = comment;
    this.createdAt = createdAt;
  }

  public String getRaterUsername() {
    return raterUsername;
  }

  public int getScore() {
    return score;
  }

  public String getComment() {
    return comment;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
