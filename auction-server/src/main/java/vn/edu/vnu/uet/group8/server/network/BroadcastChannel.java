package vn.edu.vnu.uet.group8.server.network;

import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;

public interface BroadcastChannel {
  void addClient(ClientHandler handler);
  void removeClient(ClientHandler handler);
  void broadcast(ServerResponse response);
  int getConnectedClientCount();
  void registerUser(int userId, ClientHandler handler);
  void unregisterUser(int userId, ClientHandler handler);
  void sendToUser(int userId, ServerResponse response);
}