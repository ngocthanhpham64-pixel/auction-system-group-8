package vn.edu.vnu.uet.group8.server.network;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.controller.AdminController;
import vn.edu.vnu.uet.group8.server.controller.AuthController;
import vn.edu.vnu.uet.group8.server.controller.BidController;
import vn.edu.vnu.uet.group8.server.controller.FavoriteController;
import vn.edu.vnu.uet.group8.server.controller.ItemController;
import vn.edu.vnu.uet.group8.server.controller.NotificationController;
import vn.edu.vnu.uet.group8.server.controller.RatingController;
import vn.edu.vnu.uet.group8.server.controller.UserController;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.CommentDAO;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;
import vn.edu.vnu.uet.group8.server.dao.FavoriteDAO;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.NotificationDAO;
import vn.edu.vnu.uet.group8.server.dao.RatingDAO;
import vn.edu.vnu.uet.group8.server.dao.TransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.auction.AntiSnipingService;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionClosingService;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionEventBus;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;
import vn.edu.vnu.uet.group8.server.service.auction.AutoBidService;
import vn.edu.vnu.uet.group8.server.service.auction.BidProcessor;
import vn.edu.vnu.uet.group8.server.service.auction.BidValidator;
import vn.edu.vnu.uet.group8.server.service.item.FavoriteService;
import vn.edu.vnu.uet.group8.server.service.item.ItemQueryService;
import vn.edu.vnu.uet.group8.server.service.item.ItemSpecValidator;
import vn.edu.vnu.uet.group8.server.service.item.ItemWriteService;
import vn.edu.vnu.uet.group8.server.service.user.AuthService;
import vn.edu.vnu.uet.group8.server.service.user.BalanceService;
import vn.edu.vnu.uet.group8.server.service.user.NotificationService;
import vn.edu.vnu.uet.group8.server.service.user.PasswordService;
import vn.edu.vnu.uet.group8.server.service.user.ProfileService;
import vn.edu.vnu.uet.group8.server.service.user.RatingService;
import vn.edu.vnu.uet.group8.server.service.user.RegisterService;

public class AuctionServer {
  private static final Logger log = LoggerFactory.getLogger(AuctionServer.class);
  private static final int PORT = 8080;
  private static final int MAX_CONNECTIONS = 200;

  // Mở Mini HTTP Server ở port 8081 chuyên dùng để phục vụ file ảnh
  private static void startImageServer() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
      server.createContext("/uploads/", exchange -> {
        String requestPath = exchange.getRequestURI().getPath(); // VD: /uploads/abc.jpg
        File file = new File("." + requestPath);
        if (file.exists() && file.isFile()) {
          exchange.sendResponseHeaders(200, file.length());
          try (OutputStream os = exchange.getResponseBody()) {
            Files.copy(file.toPath(), os);
          }
        } else {
          exchange.sendResponseHeaders(404, -1);
        }
      });
      server.start();
      log.info("Image Server đang chạy tại http://localhost:8081/uploads/");
    } catch (Exception e) {
      log.error("Không thể khởi động Image Server", e);
    }
  }

  // Khởi động Server
  public static void main(String[] args) {
    try {
      Connection conn = DatabaseConnection.getInstance().getConnection();
      log.info("Kết nối Database thành công");

      // DAO layer – các DAO không nhận Connection (tự quản lý)
      UserDAO userDAO = new UserDAO();
      ItemDAO itemDAO = new ItemDAO();
      AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
      BidTransactionDAO bidDAO = new BidTransactionDAO();
      TransactionDAO transactionDAO = new TransactionDAO();
      NotificationDAO notificationDAO = new NotificationDAO();
      FavoriteDAO favoriteDAO = new FavoriteDAO();
      AutoBidDAO autoBidDAO = new AutoBidDAO();
      RatingDAO ratingDAO = new RatingDAO();
      CommentDAO commentDAO = new CommentDAO();

      // Infrastructure
      SessionManager sessionManager = new SessionManager();
      BroadcastChannel broadcastChannel = new BroadcastChannelImpl();
      AuctionEventBus eventBus = new AuctionEventBus();

      // Service layer
      AuthService authService = new AuthService(userDAO, sessionManager);
      RegisterService registerService = new RegisterService(userDAO);
      ProfileService profileService = new ProfileService(userDAO);
      BalanceService balanceService = new BalanceService(userDAO, transactionDAO);
      NotificationService notificationService = new NotificationService(notificationDAO);
      RatingService ratingService = new RatingService(ratingDAO, userDAO, commentDAO);
      AuctionEventSubscriber auctionEventSubscriber = new AuctionEventSubscriber(broadcastChannel, notificationService);

      PasswordService passwordService = new PasswordService(userDAO);

      ItemSpecValidator specValidator = new ItemSpecValidator();
      ItemQueryService itemQueryService = new ItemQueryService(
          itemDAO, sessionDAO, userDAO, bidDAO, commentDAO);
      ItemWriteService itemWriteService = new ItemWriteService(
          itemDAO, userDAO, sessionDAO, specValidator);
      FavoriteService favoriteService = new FavoriteService(favoriteDAO, itemQueryService);

      // Auction
      // Sửa thứ tự: (itemDAO, userDAO, sessionDAO) theo yêu cầu constructor
      BidValidator bidValidator = new BidValidator(itemDAO, userDAO, sessionDAO);
      BidProcessor bidProcessor = new BidProcessor(bidDAO);
      AutoBidService autoBidService = new AutoBidService(autoBidDAO, bidValidator, bidProcessor, userDAO);
      AntiSnipingService antiSniping = new AntiSnipingService(sessionDAO);
      AuctionService auctionService = new AuctionService(
          sessionDAO, bidValidator, bidProcessor, antiSniping, eventBus, bidDAO, autoBidService);

      // Sửa thứ tự: (sessionDAO, itemDAO, userDAO, bidDAO, auctionEventSubscriber)
      AuctionClosingService closingService = new AuctionClosingService(
          sessionDAO, itemDAO, userDAO, bidDAO, balanceService, eventBus);
      closingService.start();
      log.info("AuctionClosingService đã start");

      // Controller layer
      AuthController authCtrl = new AuthController(authService, registerService, passwordService, sessionManager);
      ItemController itemCtrl = new ItemController(itemQueryService, itemWriteService);
      BidController bidCtrl = new BidController(auctionService);
      UserController userCtrl = new UserController(profileService, balanceService, passwordService);
      AdminController adminCtrl = new AdminController(userDAO, auctionService);
      NotificationController notifCtrl = new NotificationController(notificationService);
      FavoriteController favCtrl = new FavoriteController(favoriteService);
      RatingController ratingCtrl = new RatingController(ratingService);

      // Bật Image Server
      startImageServer();

      // Dispatcher
      AppDispatcher dispatcher = new AppDispatcher(authCtrl, itemCtrl, bidCtrl, userCtrl, adminCtrl,
          notifCtrl, favCtrl, ratingCtrl, sessionManager);

      // Thread pool
      ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CONNECTIONS);
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        log.info("Đang shutdown server...");
        closingService.stop();
        threadPool.shutdown();
      }));

      try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        log.info("Auction Server đang chạy tại port {}", PORT);
        while (!serverSocket.isClosed()) {
          Socket clientSocket = serverSocket.accept();
          log.info("Có kết nối mới từ {}", clientSocket.getInetAddress());
          ClientHandler handler = new ClientHandler(clientSocket, dispatcher, broadcastChannel);
          threadPool.execute(handler);
        }
      }
    } catch (Exception e) {
      log.error("Server gặp lỗi nghiêm trọng", e);
    }
  }
}
