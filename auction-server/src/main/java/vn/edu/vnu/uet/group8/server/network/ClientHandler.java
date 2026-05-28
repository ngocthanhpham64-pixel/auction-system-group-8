package vn.edu.vnu.uet.group8.server.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

public class ClientHandler implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
  private static final Gson GSON = GsonUtil.GSON;

  private final Socket socket;
  private final AppDispatcher dispatcher;
  private final BroadcastChannel broadcastChannel;

  private DataInputStream in;
  private DataOutputStream out;
  private String clientAddr;
  private Integer userId;

  public ClientHandler(Socket socket,
                       AppDispatcher dispatcher,
                       BroadcastChannel broadcastChannel) {
    this.socket = socket;
    this.dispatcher = dispatcher;
    this.broadcastChannel = broadcastChannel;
  }

  public Integer getUserId() {
    return this.userId;
  }

  public void associateUser(int userId) {
    if (this.userId == null || this.userId != userId) {
      this.userId = userId;
      broadcastChannel.registerUser(userId, this);
    }
  }

  @Override
  public void run() {
    try {
      in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
      out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
      clientAddr = socket.getInetAddress().getHostAddress();
      log.info("[{}] Kết nối mới", clientAddr);

      broadcastChannel.addClient(this);

      readLoop();

    } catch (IOException e) {
      log.warn("[{}] IO error: {}", clientAddr, e.getMessage());
      log.warn("[{}] IO error: {}", clientAddr, e.getMessage());
    } finally {
      cleanup();
    }
  }

  private void readLoop() {
    while (!socket.isClosed()) {
      try {
        int length = in.readInt();
        if (length <= 0 || length > 64 * 1024 * 1024) {
          throw new IOException("Độ dài gói tin request không hợp lệ hoặc quá lớn: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        String jsonRequest = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        log.info("[{}] RAW REQUEST RECEIVED: {}", clientAddr, jsonRequest);

        JsonObject request;
        try {
          request = GSON.fromJson(jsonRequest, JsonObject.class);
        } catch (Exception e) {
          log.warn("[{}] JSON không hợp lệ: {}", clientAddr, e.getMessage());
          send(ServerResponse.replyError("UNKNOWN", null,
                  "JSON không hợp lệ: " + e.getMessage()));
          continue;
        }

        if (request == null) {
          log.warn("[{}] Nhận JSON rỗng", clientAddr);
          continue;
        }

        ServerResponse response = dispatcher.dispatch(request, this);
        
        // Tự động map Socket này với User nếu đây là một yêu cầu đăng nhập thành công
        if (response.isSuccess() && "LOGIN".equals(response.getAction())) {
          Object data = response.getData();
          if (data instanceof LoginResultDTO result) {
            this.userId = result.getUserId();
            broadcastChannel.registerUser(this.userId, this);
          }
        } else if (response.isSuccess() && "LOGOUT".equals(response.getAction())) {
          if (this.userId != null) {
            broadcastChannel.unregisterUser(this.userId, this);
            this.userId = null;
          }
        }
        
        send(response);

      } catch (EOFException e) {
        log.info("[{}] Client đóng kết nối", clientAddr);
        break;
      } catch (IOException e) {
        log.warn("[{}] Mất kết nối: {}", clientAddr, e.getMessage());
        break;
      } catch (Throwable t) {
        log.error("[{}] Lỗi hệ thống nghiêm trọng trong readLoop", clientAddr, t);
        break;
      }
    }
  }

  public synchronized void send(ServerResponse response) {
    if (out == null || socket.isClosed()) return;
    try {
      String json = GSON.toJson(response);
      byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      out.writeInt(bytes.length);
      out.write(bytes);
      out.flush();
      log.debug("[{}] Gửi: {}", clientAddr, json);
    } catch (IOException e) {
      log.warn("[{}] Gửi thất bại: {}", clientAddr, e.getMessage());
    }
  }

  public String getClientAddr() {
    return this.clientAddr;
  }

  public void closeConnection() {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      log.error("[{}] Lỗi đóng socket từ closeConnection: {}", clientAddr, e.getMessage());
    }
  }

  private void cleanup() {
    broadcastChannel.removeClient(this);
    try {
      if (in != null) in.close();
      if (out != null) out.close();
      if (socket != null && !socket.isClosed()) socket.close();
    } catch (IOException e) {
      log.error("[{}] Cleanup error", clientAddr, e);
      log.error("[{}] Cleanup error", clientAddr, e);
    }
    log.info("[{}] Đã ngắt kết nối", clientAddr);
    log.info("[{}] Đã ngắt kết nối", clientAddr);
  }
}
