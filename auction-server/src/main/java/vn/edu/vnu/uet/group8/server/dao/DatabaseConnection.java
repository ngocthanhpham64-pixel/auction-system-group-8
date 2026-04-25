package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
  private static final String URL =
      "jdbc:mysql://localhost:3306/auction_db" +
      "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
  private static final String USER = "root";
  private static final String PASS = "";

  private static volatile DatabaseConnection instance;
  private Connection connection;

  private DatabaseConnection() throws SQLException {
    this.connection = DriverManager.getConnection(URL, USER, PASS);
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
    // Tự reconnect nếu connection bị đứt
    if (connection == null || connection.isClosed()) {
      connection = DriverManager.getConnection(URL, USER, PASS);
    }
    return connection;
  }
}