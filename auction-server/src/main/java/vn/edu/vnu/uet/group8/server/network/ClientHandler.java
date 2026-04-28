package vn.edu.vnu.uet.group8.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import vn.edu.vnu.uet.group8.server.service.BidObserver;

public class ClientHandler implements Runnale {
  private final Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  public ClientHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      String clienMessage;
      while ((clientMessage = in.readLine()) != null) {
        System.out.println("Nhận từ client: " + clientMessage);

        if (clienMessage.startsWith("LOGIN")) {
          out.println("RESPONE: LOGGIN_SUCCESS");
        } else if (clienMessage.equals("QUIT")) {
          break;
        }
      }
    } catch (IOException e) {
      System.err.println("Lỗi khi xử lý client: " + e.getMessage());
    } finally {
      closeEverything();
    }
  }
  private void closeEverything(){
    try {
      AuctionServer.activeClients.remove(this);
      if (in != null) in.close();
      if (out != null) out.close();
      if (socket != null) socket.close();
      System.out.println("Đã dọn dẹp kết nối của Client.");
    } catch (IOException e){
      e.printStackTrace();
    }
  }

  public void updateBid(){
    if (out != null){
      out.println("NEW_BID_UPDATE: " + auctionData);
    }
  }
}