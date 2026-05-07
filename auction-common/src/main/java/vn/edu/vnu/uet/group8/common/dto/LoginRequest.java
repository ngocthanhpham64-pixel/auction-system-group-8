package vn.edu.vnu.uet.group8.common.dto;

/**
 * Yêu cầu đăng nhập từ client.
 * Password được gửi ở dạng plaintext(chỉ qua socket, không log).
 */
public final class LoginRequest {
    private  final String email;
    private  final String password;

    private LoginRequest(String email,String password){
        if(email == null || email.isBlank())
            throw new IllegalArgumentException("Email không được trống");
        if(password == null || password.isEmpty()){
            throw new IllegalArgumentException("Password không đuợc trống");
        }
        this.email = email.trim().toLowerCase();
        this.password = password;
    }
    public static LoginRequest of(String email, String password){
        return new LoginRequest(email,password);
    }

    public String getEmail(){return email;}
    public String getPassword(){return password;}

    @Override
    public String toString(){
        return "LoginRequest{email='" + email + "',password='[HIDDEN]'}";
    }
}
