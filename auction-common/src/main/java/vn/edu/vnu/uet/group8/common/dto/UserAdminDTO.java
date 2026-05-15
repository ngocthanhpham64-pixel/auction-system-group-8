package vn.edu.vnu.uet.group8.common.dto;

public class UserAdminDTO {
    private int id;
    private String userName;
    private String email;
    private String role; // MEMBER, SELLER, ADMIN
    private String status; // ACTIVE, SUSPENDED, BANNED

    public UserAdminDTO(){}

    public UserAdminDTO (int id, String userName, String email, String role, String status){
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.role = role;
        this.status = status;
    }
    public int getId(){ return id;}
    public void setId(int id){ this.id = id;}
    public String getUsername(){ return userName;}
    public void setUsername(String userName) { this.userName = userName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
