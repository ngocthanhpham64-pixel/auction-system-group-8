package vn.edu.vnu.uet.group8.server.network;

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