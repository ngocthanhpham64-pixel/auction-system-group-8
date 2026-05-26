package vn.edu.vnu.uet.group8.common.dto;

/**
 * DTO phản hồi đăng nhập - immutable, an toàn, đủ thông tin cho client.
 * Không chứa User entity, không lộ encryptedPassword.
 */
public final class LoginResponse {

    private final boolean success;
    private final String message;

    // Chỉ có khi success = true
    private final int userId;
    private final String username;
    private final String fullName;
    private final String email;
    private final String role; //"Member","Admin"
    private final String authToken;

    private LoginResponse(boolean success,String message,
                          int userId,String username,String fullName,String email,String role,String authToken){
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role      = role;
        this.authToken = authToken;
    }
    // Factory methods
    public static LoginResponse success(int userId,String username,
                                        String fullName,String email,String role,String authToken){
        return new LoginResponse(true,"Đăng nhập thành công",userId,username,fullName,email,role,authToken);
    }
    public static LoginResponse failure (String reason){
        return new LoginResponse(false,reason,0,null,null,null,null,null);
    }
    // Getters
    public boolean isSuccess()   { return success;   }
    public String  getMessage()  { return message;   }
    public int     getUserId()   { return userId;    }
    public String  getUsername() { return username;  }
    public String  getFullName() { return fullName;  }
    public String  getEmail()    { return email;     }
    public String  getRole()     { return role;      }
    public String  getAuthToken(){ return authToken; }
    @Override
    public String toString(){
        return "LoginResponse{success=" + success
                + ", message='" + message + '\''
                + ", userId=" + userId
                + ", username='" + username + '\''
                + ", role='" + role + '\'' + '}';
        // Chú ý: không in authToken ra log
    }
}