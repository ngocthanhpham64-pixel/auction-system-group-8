package vn.edu.vnu.uet.group8.server.network;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.EventType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * Implementation của BroadcastChannel – quản lý client list và gửi broadcast.
 */
public class BroadcastChannelImpl implements BroadcastChannel {
  private static final Logger log = LoggerFactory.getLogger(BroadcastChannelImpl.class);
  private static final Gson GSON = GsonUtil.GSON;

  private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
  private final Map<Integer, Set<ClientHandler>> userSessions = new ConcurrentHashMap<>();

  @Override
  public void addClient(ClientHandler handler) {
    if (handler == null) return;
    clients.add(handler);
    log.debug("Client mới đăng ký - tổng={}", clients.size());
  }

  @Override
  public void removeClient(ClientHandler handler) {
    if (handler == null) return;
    clients.remove(handler);
    
    Integer boundUserId = handler.getUserId();
    if (boundUserId != null) {
      Set<ClientHandler> sessions = userSessions.get(boundUserId);
      if (sessions != null) {
        sessions.remove(handler);
      }
    }
    log.debug("Client rời đi - còn lại={}", clients.size());
  }

  @Override
  public void broadcast(ServerResponse response) {
    pushToAll(response, response.getEventType());
  }

  @Override
  public int getConnectedClientCount() {
    return clients.size();
  }

  @Override
  public void registerUser(int userId, ClientHandler handler) {
    if (handler == null) return;
    Set<ClientHandler> sessions = userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
    for (ClientHandler oldHandler : sessions) {
      if (oldHandler != handler) {
        log.info("Kicking duplicate session cho userId={} (addr={})", userId, oldHandler.getClientAddr());
        try {
          ServerResponse kickResponse = ServerResponse.broadcast(EventType.KICKED)
              .message("Tài khoản của bạn đã được đăng nhập từ một thiết bị khác.")
              .build();
          oldHandler.send(kickResponse);
        } catch (Exception e) {
          log.warn("Không gửi được gói tin KICKED tới userId={}: {}", userId, e.getMessage());
        }
        oldHandler.closeConnection();
      }
    }
    sessions.clear();
    sessions.add(handler);
    log.debug("User {} đã map với một kết nối socket mới", userId);
  }

  @Override
  public void unregisterUser(int userId, ClientHandler handler) {
    if (handler == null) return;
    Set<ClientHandler> sessions = userSessions.get(userId);
    if (sessions != null) {
      sessions.remove(handler);
    }
  }

  @Override
  public void sendToUser(int userId, ServerResponse response) {
    Set<ClientHandler> sessions = userSessions.get(userId);
    if (sessions == null || sessions.isEmpty()) {
      log.debug("User {} không online, bỏ qua gửi tin nhắn cá nhân", userId);
      return;
    }
    int sent = 0;
    for (ClientHandler client : sessions) {
      try {
        client.send(response);
        sent++;
      } catch (Exception e) {
        removeClient(client);
      }
    }
    log.debug("Gửi tới User {}: sent={}/{}", userId, sent, sessions.size());
  }

  private void pushToAll(ServerResponse response, EventType eventType) {
    int sent = 0;
    for (ClientHandler client : clients) {
      try {
        client.send(response);
        sent++;
      } catch (Exception e) {
        clients.remove(client);
        log.warn("Lỗi gửi broadcast tới client - loại khỏi danh sách", e);
      }
    }
    log.debug("Broadcast {}: sent={}/{}", eventType, sent, clients.size());
  }
}