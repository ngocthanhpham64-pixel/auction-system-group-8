package vn.edu.vnu.uet.group8.common.dto;


public final class LoginRequest {
    private final String email;
    private final String password;

    public LoginRequest(String email,String password){
        this.email = email.trim().toLowerCase();
        this.password = password;
    }
    public String getEmail(){return email;}
    public String getPassword(){return password;}
}
