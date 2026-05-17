package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConnection {
  private static final String URL =
      "jdbc:mysql://localhost:3306/auction_db" +
      "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
  private static final String USER = "root";
  private static final String PASS = "NgocThanh291@";

  private static volatile DatabaseConnection instance;
  private HikariDataSource dataSource;

  private DatabaseConnection() throws SQLException {
    try {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(URL);
      config.setUsername(USER);
      config.setPassword(PASS);
      
      // Optional optimizations for HikariCP + MySQL
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      
      // Pool configuration
      config.setMaximumPoolSize(10);
      config.setMinimumIdle(2);
      
      this.dataSource = new HikariDataSource(config);
    } catch (Exception e) {
      throw new SQLException("Failed to initialize HikariCP connection pool", e);
    }
  }

  public static DatabaseConnection getInstance() throws SQLException {
    if (instance == null) {
      synchronized (DatabaseConnection.class) {
        if (instance == null) {
          instance = new DatabaseConnection();
        }
      }
    }
    return instance;
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }
}