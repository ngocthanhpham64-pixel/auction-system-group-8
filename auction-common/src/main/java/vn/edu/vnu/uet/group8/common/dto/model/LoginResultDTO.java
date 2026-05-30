package vn.edu.vnu.uet.group8.common.dto.model;

public class LoginResultDTO {
    private UserSummaryDTO user;
    private String token;

    public LoginResultDTO() {}

    public LoginResultDTO(UserSummaryDTO user, String token) {
        this.user = user;
        this.token = token;
    }

    public static LoginResultDTO from(UserSummaryDTO user, String token) {
        return new LoginResultDTO(user, token);
    }

    public UserSummaryDTO user() { return user; }
    public String token() { return token; }

    public int getUserId() {
        return user != null ? user.getUserId() : 0;
    }
}
