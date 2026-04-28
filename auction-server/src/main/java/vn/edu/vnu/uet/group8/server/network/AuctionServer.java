package vn.edu.vnu.uet.group8.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AuctionServer{
    private static final int PORT = 8080;
    // Danh sách lưu trữ các client đang kết nối để có thể broadcast (gửi thông báo hàng loạt)
    public static List<ClientHandler> activeClients = new ArrayList<>();

    public static void main (String[] args) {
        System.out.println("Mở máy chủ. Lắng nghe tại cổng: " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client mới kết nối: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                activeClients.add(handler); // Thêm vào danh sách client đang hoạt động
                new Thread(handler).start(); // Tạo luồng mới để xử lý client này
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi mở server: " + e.getMessage());

        }
    }
}