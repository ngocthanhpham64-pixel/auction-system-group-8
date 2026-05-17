package vn.edu.vnu.uet.group8.common.dto.model;

public record LoginResultDTO(
  UserSummaryDTO user,
  String token
) {
  // Factory method để tạo DTO cho tiện
  public static LoginResultDTO from(UserSummaryDTO user, String token) {
      return new LoginResultDTO(user, token);
  }

  public int getUserId() {
    return user.getUserId();
  }
}
