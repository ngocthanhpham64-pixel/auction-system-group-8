package vn.edu.vnu.uet.group8.server.network;

import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;

public interface BroadcastChannel {
  void addClient(ClientHandler handler);
  void removeClient(ClientHandler handler);
  void broadcastPriceUpdate(PriceUpdateBroadcastResponse data);
  void broadcastAuctionEnded(AuctionEndedBroadcastResponse data);
}