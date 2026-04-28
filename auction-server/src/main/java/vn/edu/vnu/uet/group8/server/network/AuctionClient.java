package vn.edu.vnu.uet.group8.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AuctionClient {
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  public void connectToServer(String serverAddress, int port) {
    try {
      socket = new Socket(serverAddress, port);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      System.out.println("Đã kết nối đến server: " + serverAddress + ":" + port);

      new Thread(new Runnable() {
        @Override
        public void run() {
          listenForMessage();
        }
      }).start();
    } catch (IOException e) {
      System.err.println("Lỗi khi kết nối đến server: " + e.getMessage());
    }
  }

  public void sendData(String message) {
    if (out != null) {
      out.println(message);
    }
  }

  private void listenForMessage() {
    try {
      String serverMessage;
      while ((serverMessage = in.readLine()) != null) {
        System.out.println("Server gửi: " + serverMessage);

        if (serverMessage.startsWith("NEW_BID_UPDATE:")) {
          String data = serverMessage.substring(15);
          // Cập nhật giao diện tại đây
        }
      }
    } catch (IOException e) {
      System.out.println("Kết nối tới Server đã bị ngắt.");
    }
  }
}