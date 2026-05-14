package vn.edu.vnu.uet.group8.server.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.controller.AuthController;
import vn.edu.vnu.uet.group8.server.controller.BidController;
import vn.edu.vnu.uet.group8.server.controller.ItemController;
import vn.edu.vnu.uet.group8.server.controller.UserController;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.TransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.auction.AntiSnipingService;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionClosingService;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionEventBus;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;
import vn.edu.vnu.uet.group8.server.service.auction.BidProcessor;
import vn.edu.vnu.uet.group8.server.service.auction.BidValidator;
import vn.edu.vnu.uet.group8.server.service.item.ItemQueryService;
import vn.edu.vnu.uet.group8.server.service.item.ItemSpecValidator;
import vn.edu.vnu.uet.group8.server.service.item.ItemWriteService;
import vn.edu.vnu.uet.group8.server.service.user.AuthService;
import vn.edu.vnu.uet.group8.server.service.user.BalanceService;
import vn.edu.vnu.uet.group8.server.service.user.PasswordService;
import vn.edu.vnu.uet.group8.server.service.user.ProfileService;
import vn.edu.vnu.uet.group8.server.service.user.RegisterService;

public class AuctionServer {
  private static final Logger log = LoggerFactory.getLogger(AuctionServer.class);
  private static final int PORT = 8080;
  private static final int MAX_CONNECTIONS = 200;

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

      // Infrastructure
      SessionManager sessionManager = new SessionManager();
      BroadcastChannel broadcastChannel = new BroadcastChannelImpl();
      AuctionEventBus eventBus = new AuctionEventBus();

      // Service layer
      AuthService authService = new AuthService(userDAO, sessionManager);
      RegisterService registerService = new RegisterService(userDAO);
      ProfileService profileService = new ProfileService(userDAO);
      BalanceService balanceService = new BalanceService(userDAO, transactionDAO);
      AuctionEventSubscriber auctionEventSubscriber = 
              new AuctionEventSubscriber(broadcastChannel);
      PasswordService passwordService = new PasswordService(userDAO);


      ItemSpecValidator specValidator = new ItemSpecValidator();
      ItemQueryService itemQueryService = new ItemQueryService(
        itemDAO, sessionDAO, userDAO, bidDAO);
      ItemWriteService itemWriteService = new ItemWriteService(
        itemDAO, userDAO, sessionDAO, specValidator);
      

      // Auction
      // Sửa thứ tự: (itemDAO, userDAO, sessionDAO) theo yêu cầu constructor
      BidValidator bidValidator = new BidValidator(itemDAO, userDAO, sessionDAO);
      BidProcessor bidProcessor = new BidProcessor(bidDAO);
      AntiSnipingService antiSniping = new AntiSnipingService(sessionDAO);
      AuctionService auctionService = new AuctionService(
        sessionDAO, bidValidator, bidProcessor, antiSniping, eventBus, bidDAO);

      // Sửa thứ tự: (sessionDAO, itemDAO, userDAO, bidDAO, auctionEventSubscriber)
      AuctionClosingService closingService = new AuctionClosingService(
        sessionDAO, itemDAO, userDAO, bidDAO, balanceService, eventBus);
      closingService.start();
      log.info("AuctionClosingService đã start");

      // Controller layer
      AuthController authCtrl = new AuthController(authService, registerService, sessionManager);
      ItemController itemCtrl = new ItemController(itemQueryService, itemWriteService);
      BidController bidCtrl = new BidController(auctionService);
      UserController userCtrl = new UserController(profileService, balanceService, passwordService);

      // Dispatcher
      AppDispatcher dispatcher = new AppDispatcher(authCtrl, itemCtrl, bidCtrl, userCtrl, sessionManager);

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
