package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for User entity.
 * Used for transferring user data between client and server.
 */
public final class UserDTO {

  @SerializedName("id")
  private Long id;

  @SerializedName("username")
  private String username;

  @SerializedName("email")
  private String email;

  // Password is included only when necessary (e.g., registration, login response)
  // For security, avoid sending password in most responses
  @SerializedName("password")
  private String password;

  @SerializedName("balance")
  private BigDecimal balance;

  @SerializedName("role")
  private String role;  // e.g., "BUYER", "SELLER", "ADMIN"

  @SerializedName("createdAt")
  private LocalDateTime createdAt;

  // Constructors
  public UserDTO() {}

  public UserDTO(Long id, String username, String email, String password,
                  BigDecimal balance, String role, LocalDateTime createdAt) {
      this.id = id;
      this.username = username;
      this.email = email;
      this.password = password;
      this.balance = balance;
      this.role = role;
      this.createdAt = createdAt;
  }

  // Builder pattern (optional but convenient)
  public static Builder builder() {
      return new Builder();
  }

  public static final class Builder {
      private Long id;
      private String username;
      private String email;
      private String password;
      private BigDecimal balance;
      private String role;
      private LocalDateTime createdAt;

      public Builder id(Long id) { this.id = id; return this; }
      public Builder username(String username) { this.username = username; return this; }
      public Builder email(String email) { this.email = email; return this; }
      public Builder password(String password) { this.password = password; return this; }
      public Builder balance(BigDecimal balance) { this.balance = balance; return this; }
      public Builder role(String role) { this.role = role; return this; }
      public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

      public UserDTO build() {
          return new UserDTO(id, username, email, password, balance, role, createdAt);
      }
  }

  // Getters and Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public BigDecimal getBalance() { return balance; }
  public void setBalance(BigDecimal balance) { this.balance = balance; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UserDTO userDTO = (UserDTO) o;
      return Objects.equals(id, userDTO.id) &&
              Objects.equals(username, userDTO.username) &&
              Objects.equals(email, userDTO.email) &&
              Objects.equals(role, userDTO.role);
  }

  @Override
  public int hashCode() {
      return Objects.hash(id, username, email, role);
  }

  @Override
  public String toString() {
      return "UserDTO{id=" + id +
              ", username='" + username + '\'' +
              ", email='" + email + '\'' +
              ", role='" + role + '\'' +
              ", balance=" + balance +
              ", createdAt=" + createdAt +
              '}';
  }
}
