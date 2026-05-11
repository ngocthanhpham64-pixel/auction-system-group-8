package vn.edu.vnu.uet.group8.common.dto;

/**
 * Yêu cầu đăng nhập từ client.
 * Password được gửi ở dạng plaintext(chỉ qua socket, không log).
 */
public final class LoginRequest {
    private  final String username;
    private  final String password;

    private LoginRequest(String username,String password){
        if(username == null || username.isBlank())
            throw new IllegalArgumentException("Email không được trống");
        if(password == null || password.isEmpty()){
            throw new IllegalArgumentException("Password không đuợc trống");
        }
        this.username = username.trim().toLowerCase();
        this.password = password;
    }
    public static LoginRequest of(String username, String password){
        return new LoginRequest(username,password);
    }

    public String getUsername(){return username;}
    public String getPassword(){return password;}

    @Override
    public String toString(){
        return "LoginRequest{email='" + username + "',password='[HIDDEN]'}";
    }
}
