package vn.edu.vnu.uet.group8.server.network;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.util.GsonUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    log.debug("Client mới đăng ký – tổng={}", clients.size());
  }

  @Override
  public void removeClient(ClientHandler handler) {
    if (handler == null) return;
    clients.remove(handler);
    log.debug("Client rời đi – còn lại={}", clients.size());
  }

  public int connectedCount() {
    return clients.size();
  }

  @Override
  public void broadcastPriceUpdate(PriceUpdateBroadcastResponse data) {
    ServerResponse response = ServerResponse.broadcast("PRICE_UPDATE")
            .success(true)
            .data(data)
            .build();
    pushToAll(response, "PRICE_UPDATE");
  }

  @Override
  public void broadcastAuctionEnded(AuctionEndedBroadcastResponse data) {
    ServerResponse response = ServerResponse.broadcast("AUCTION_ENDED")
            .success(true)
            .data(data)
            .build();
    pushToAll(response, "AUCTION_ENDED");
  }

  private void pushToAll(ServerResponse response, String eventType) {
    int sent = 0;
    for (ClientHandler client : clients) {
      try {
        client.send(response);
        sent++;
      } catch (Exception e) {
        clients.remove(client);
        log.warn("Lỗi gửi broadcast tới client – loại khỏi danh sách", e);
      }
    }
    log.debug("Broadcast {}: sent={}/{}", eventType, sent, clients.size());
  }
}