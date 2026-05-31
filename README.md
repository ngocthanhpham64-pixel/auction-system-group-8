# Auctiva — Hệ thống Đấu giá Trực tuyến

> Bài Tập Lớn **Lập trình Nâng cao**
> Trường Đại học Công nghệ — Đại học Quốc gia Hà Nội (UET-VNU)

**Auctiva** là một hệ thống đấu giá trực tuyến (online auction) xây dựng theo kiến trúc **client–server** với giao diện JavaFX và giao thức truyền tin TCP socket. Người dùng có thể đăng bán sản phẩm, tham gia các phiên đấu giá thời gian thực, đặt giá tự động (auto-bid) và quản lý ví điện tử trong nền tảng.

---

## Mục lục

- [1. Tính năng nổi bật](#1-tính-năng-nổi-bật)
- [2. Kiến trúc hệ thống](#2-kiến-trúc-hệ-thống)
- [3. Công nghệ sử dụng](#3-công-nghệ-sử-dụng)
- [4. Cấu trúc dự án](#4-cấu-trúc-dự-án)
- [5. Yêu cầu môi trường](#5-yêu-cầu-môi-trường)
- [6. Hướng dẫn cài đặt và chạy](#6-hướng-dẫn-cài-đặt-và-chạy)
- [7. Cấu hình kết nối](#7-cấu-hình-kết-nối)
- [8. Mô-đun chi tiết](#8-mô-đun-chi-tiết)
- [9. Giao thức truyền tin](#9-giao-thức-truyền-tin)
- [10. Thiết kế giao diện](#10-thiết-kế-giao-diện)
- [11. Kiểm thử (Unit Test)](#11-kiểm-thử-unit-test)
- [12. CI/CD](#12-cicd)
- [13. Quy ước phát triển](#13-quy-ước-phát-triển)
- [14. PDF và Video](#14-PDF-và-Video)
- [15. Nhóm phát triển](#15-nhóm-phát-triển)

---

## 1. Tính năng nổi bật

### Cho người dùng (Bidder & Seller)

- **Đăng ký / Đăng nhập** với xác thực mật khẩu BCrypt + OTP qua email khi quên mật khẩu.
- **Khám phá sản phẩm** với bộ lọc theo danh mục, tình trạng, khoảng giá, thời gian.
- **Phòng đấu giá trực tiếp (Live Auction)** — cập nhật giá theo thời gian thực qua broadcast từ server.
- **Đặt giá tự động (Auto-bid)** — đặt mức giá tối đa, hệ thống tự đấu giúp đến khi đạt ngưỡng.
- **Cơ chế Anti-sniping** — phiên đấu giá tự gia hạn nếu có bid trong những giây cuối.
- **Đăng bán sản phẩm** với hình ảnh (Base64), thông số kỹ thuật theo từng danh mục.
- **Quản lý đơn hàng**, lịch sử đặt giá, sản phẩm yêu thích, thông báo realtime.
- **Ví điện tử** — nạp tiền, rút tiền, xem lịch sử giao dịch.
- **Đánh giá người bán** sau khi giao dịch thành công.
- **Giao diện sáng / tối** — hỗ trợ Light Theme và Dark Theme có thể chuyển đổi trong Settings.

### Cho Admin

- **Dashboard tổng quan** — 4 chỉ số chính: tổng user, tổng phiên đấu giá, doanh thu, số phiên đang diễn ra.
- **Quản lý người dùng** — xem danh sách, khoá / mở khoá tài khoản.
- **Quản lý phiên đấu giá** — xem danh sách, huỷ phiên nếu cần.

---

## 2. Kiến trúc hệ thống

Hệ thống được thiết kế theo mô hình **3 mô-đun Maven độc lập**, giao tiếp qua một module dùng chung:

```
┌──────────────────┐         TCP Socket         ┌──────────────────┐
│  auction-client  │ ◄─────────────────────────►│  auction-server  │
│   (JavaFX UI)    │      JSON / Gson           │  (Business)      │
└──────────────────┘                            └────────┬─────────┘
         │                                               │
         │                                               ▼
         │                                      ┌──────────────────┐
         │                                      │     MySQL        │
         │                                      │   (HikariCP)     │
         │                                      └──────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│                       auction-common                              │
│   Entity • Enum • DTO (Request / Response / Model) — dùng chung   │
└──────────────────────────────────────────────────────────────────┘
```

**Nguyên tắc thiết kế:**

- **Tách biệt rõ ràng** giữa tầng giao diện (Controller JavaFX), tầng dịch vụ (Service), tầng mạng (`AuctionClient` + `ResponseDispatcher`) ở phía client.
- **Bất đồng bộ (asynchronous)** — mọi request đều dùng `Consumer<ServerResponse>` callback, không block UI thread.
- **Sự kiện đẩy (push event)** — server broadcast giá mới, kết thúc phiên, thông báo qua `EventType` mà client subscribe.
- **Event-driven phía server** — `AuctionEventBus` phát sự kiện (`BidPlacedEvent`, `AuctionEndedEvent`, ...) để tách rời nghiệp vụ và broadcast.
- **Session management** — `SessionManager` phía server quản lý phiên đăng nhập của từng `ClientHandler`.

---

## 3. Công nghệ sử dụng

| Lớp | Công nghệ | Phiên bản |
|---|---|---|
| Ngôn ngữ | Java | **17** |
| Build & Quản lý phụ thuộc | Maven | 3.8+ |
| Giao diện | JavaFX | 17.0.6 |
| Layout | FXML + CSS | — |
| Network | Java Socket API (TCP) | JDK 17 |
| Serialization | Google Gson | 2.10.1 |
| Bảo mật mật khẩu | jBCrypt | 0.4 |
| Cơ sở dữ liệu | MySQL | 8.x |
| Connection Pool | HikariCP | 5.1.0 |
| Logging | SLF4J + Logback | 2.0.16 / 1.5.12 |
| Kiểm thử | JUnit Jupiter | 5.10.0 |
| Mock | Mockito | 5.8.0 |
| Coverage | JaCoCo | 0.8.11 |
| Coding Style | Checkstyle (Google Style) | maven-checkstyle-plugin 3.3.1 |
| CI/CD | GitHub Actions | — |

---

## 4. Cấu trúc dự án

```
auction-system-group-8/
├── .github/
│   └── workflows/
│       └── java-ci.yaml             # GitHub Actions: checkstyle + test + package
├── .gitignore
├── docs/
│   ├── schema.sql                   # Schema MySQL đầy đủ
│   ├── auction_common_api_doc.docx  # Tài liệu API auction-common
│   └── client_api_doc.docx          # Tài liệu API client
├── output/
│   ├── auction-common.puml          # PlantUML sơ đồ lớp common
│   └── auction-server.puml          # PlantUML sơ đồ lớp server
├── out/output/                      # SVG render từ PlantUML
├── pom.xml                          # POM cha — quản lý 3 module + Checkstyle + JaCoCo
│
├── auction-common/                  # Module dùng chung (không phụ thuộc client/server)
│   ├── pom.xml
│   └── src/
│       ├── main/java/vn/edu/vnu/uet/group8/common/
│       │   ├── entity/              # 8 Entity nghiệp vụ
│       │   ├── enums/               # 15 Enum dùng chung
│       │   └── dto/
│       │       ├── model/           # DTO trả về (UserDTO, AuctionItemDTO...)
│       │       ├── request/         # DTO yêu cầu (LoginRequest, BidRequest...)
│       │       └── response/        # DTO phản hồi (ServerResponse, BidResponse...)
│       └── test/                    # 39 file test cho common
│
├── auction-server/                  # Module server (nghiệp vụ + DB)
│   ├── pom.xml
│   └── src/
│       ├── main/java/vn/edu/vnu/uet/group8/server/
│       │   ├── network/             # AuctionServer, ClientHandler, AppDispatcher,
│       │   │                        #   BroadcastChannel, BroadcastChannelImpl,
│       │   │                        #   RequestParser, AuctionEventSubscriber
│       │   ├── auth/                # SessionManager
│       │   ├── controller/          # 8 Controller (Auth, User, Item, Bid,
│       │   │                        #   Admin, Favorite, Notification, Rating)
│       │   ├── service/
│       │   │   ├── auction/         # AuctionService, AuctionClosingService,
│       │   │   │                    #   AntiSnipingService, AutoBidService,
│       │   │   │                    #   HybridBidExecutor, BidValidator,
│       │   │   │                    #   BidProcessor, AuctionEventBus
│       │   │   │   └── event/       # BidPlacedEvent, AuctionEndedEvent,
│       │   │   │                    #   AuctionOpenedEvent, AuctionCancelledEvent
│       │   │   ├── item/            # ItemWriteService, ItemQueryService,
│       │   │   │                    #   FavoriteService, ItemSpecValidator
│       │   │   └── user/            # AuthService, RegisterService, ProfileService,
│       │   │                        #   BalanceService, PasswordService, RatingService,
│       │   │                        #   AdminUserService, NotificationService, UserPolicy
│       │   ├── dao/                 # UserDAO, ItemDAO, AuctionSessionDAO,
│       │   │                        #   BidTransactionDAO, AutoBidDAO, FavoriteDAO,
│       │   │                        #   NotificationDAO, RatingDAO, TransactionDAO,
│       │   │                        #   DatabaseConnection (HikariCP)
│       │   └── util/                # FileUtil, PasswordUtil
│       ├── main/resources/
│       │   └── logback.xml          # Logging: console + rolling file (30 ngày)
│       └── test/                    # 51 file test cho server
│
└── auction-client/                  # Module client (JavaFX UI)
    ├── pom.xml
    └── src/main/
        ├── java/vn/edu/vnu/uet/group8/client/
        │   ├── AuctionClientApp.java    # Entry point JavaFX
        │   ├── controller/              # 46 Controller cho các màn hình
        │   ├── service/                 # 22 Service gọi server (tất cả static)
        │   ├── networking/              # AuctionClient (TCP socket) + ResponseDispatcher
        │   ├── model/                   # ClientModel (cache local)
        │   └── util/                    # AlertUtil, ModalUtil, PopupUtil,
        │                                #   SceneManager, SessionManager, UIFormatter
        └── resources/
            ├── fxml/                    # 40 file FXML — bố cục giao diện
            ├── css/                     # tokens.css, layout.css, components.css,
            │                            #   light-theme.css, dark-theme.css, login.css
            └── image/                   # logo.png + ảnh tĩnh
```

---

## 5. Yêu cầu môi trường

Trước khi cài đặt, máy của bạn cần có:

- **JDK 17** trở lên (khuyến nghị OpenJDK 17 hoặc Temurin 17).
- **Apache Maven 3.8+**.
- **MySQL 8.x** đang chạy ở `localhost:3306` (hoặc tuỳ cấu hình).
- **Git** để clone repository.
- Hệ điều hành: Windows 10/11, macOS, hoặc Linux đều OK.

Kiểm tra phiên bản:

```bash
java -version       # phải hiện "17.x"
mvn -v              # Maven 3.8+
mysql --version
```

---

## 6. Hướng dẫn cài đặt và chạy

### Bước 1. Clone repository

```bash
git clone https://github.com/ngocthanhpham64-pixel/auction-system-group-8.git
cd auction-system-group-8
```

### Bước 2. Cài đặt cơ sở dữ liệu

1. Đăng nhập MySQL và tạo database:

   ```sql
   CREATE DATABASE auctiva CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Import schema từ thư mục `docs/`:

   ```bash
   mysql -u root -p auctiva < docs/schema.sql
   ```

3. Cập nhật thông tin kết nối DB trong `DatabaseConnection.java` của `auction-server` (xem phần [7. Cấu hình kết nối](#7-cấu-hình-kết-nối)).

### Bước 3. Build toàn bộ project

Tại thư mục gốc:

```bash
mvn clean install
```

Maven sẽ build lần lượt: `auction-common` → `auction-server` → `auction-client`.

> Nếu chỉ muốn build mà bỏ qua test: `mvn clean install -DskipTests`

### Bước 4. Chạy Server

Mở terminal mới, tại thư mục gốc:

```bash
cd auction-server
mvn exec:java -Dexec.mainClass="vn.edu.vnu.uet.group8.server.network.AuctionServer"
```

Server sẽ lắng nghe tại cổng **8080** (mặc định). Log được ghi ra console và file `logs/auction-server.log`.

### Bước 5. Chạy Client (JavaFX)

Mở terminal khác:

```bash
cd auction-client
mvn javafx:run
```

Cửa sổ ứng dụng **Auctiva — Live Online Auction** sẽ mở ra với màn hình đăng nhập.

> **Tip:** Có thể chạy nhiều client cùng lúc bằng cách mở nhiều terminal và lặp lại bước 5 — rất hữu ích để test phiên đấu giá nhiều người.

---

## 7. Cấu hình kết nối

### Client → Server

Cấu hình mặc định trong `AuctionClientApp.java`:

```java
private static final String SERVER_HOST = "localhost";
private static final int    SERVER_PORT = 8080;
```

Sửa hai hằng số trên nếu server chạy ở máy khác.

### Cửa sổ ứng dụng

```java
primaryStage.setWidth(1280);
primaryStage.setHeight(800);
primaryStage.setMinWidth(1024);
primaryStage.setMinHeight(700);
primaryStage.setMaximized(true);   // Mặc định full-screen khi mở
```

### Server → Database

Thông tin kết nối MySQL được khai báo trong `DatabaseConnection.java` (HikariCP). Cập nhật URL, username và password tương ứng với môi trường của bạn:

```java
config.setJdbcUrl("jdbc:mysql://localhost:3306/auctiva?useSSL=false&serverTimezone=UTC");
config.setUsername("root");
config.setPassword("yourpassword");
```

---

## 8. Mô-đun chi tiết

### 8.1. `auction-common`

Module **không có dependency** vào client hoặc server — đảm bảo tính độc lập. Gồm:

#### Entity (8 class)

| Class | Mô tả |
|---|---|
| `Entity` | Lớp cha trừu tượng — `id`, `createdAt`, `isDeleted` |
| `User` (abstract) | Lớp cha của `UserMember` và `UserAdmin` |
| `UserMember` | Người dùng thường — có ví, vai trò `BIDDER`/`SELLER` |
| `UserAdmin` | Quản trị viên — có `AdminLevel` |
| `Item` | Sản phẩm đấu giá — title, mô tả, ảnh, specs |
| `AuctionSession` | Phiên đấu giá — giá khởi điểm, hiện tại, người dẫn đầu |
| `AutoBidConfig` | Cấu hình đấu giá tự động của một user trên một phiên |
| `Notification` | Thông báo realtime |

#### Enum (15 enum)

`ActionType` (40+ giá trị), `EventType`, `UserRole`, `UserStatus`, `AdminLevel`, `ItemCategory`, `ItemCondition`, `ItemStatus`, `SessionStatus`, `AuctionStatus`, `NotificationType`, `PaymentMethod`, `TransactionType`, `TransactionStatus`, `SpecKey`.

#### DTO (25+ class)

Chia thành 3 thư mục:

- **`model/`** — DTO biểu diễn dữ liệu trả về cho client: `UserDTO`, `UserProfileDTO`, `UserBidHistoryDTO`, `AuctionItemDTO`, `AuctionStatusDTO`, `BidRecord`, `NotificationDTO`, `AdminStatsDTO`, `UserAdminDTO`, `TransactionHistoryEntry`, `TransactionRecord`, `LoginResultDTO`, `UserSummaryDTO`.
- **`request/`** — DTO client gửi lên: `LoginRequest`, `RegisterRequest`, `BidRequest`, `AutoBidRequest`, `DepositRequest`, `GetAuctionsRequest`, `ServerRequest<T>` (envelope).
- **`response/`** — DTO server trả về: `ServerResponse`, `LoginResponse`, `BidResponse`, `PriceUpdateBroadcastResponse`, `AuctionEndedBroadcastResponse`.

### 8.2. `auction-client`

Cấu trúc theo mô hình **MVC + Service Layer**:

#### Controller (46 class)

Một số Controller chính:

| Controller | Màn hình |
|---|---|
| `LoginController` | Đăng nhập |
| `ForgetPasswordController` | Quên mật khẩu + OTP |
| `RegisterController` | Đăng ký tài khoản |
| `MainController` | Layout chính (sidebar + content area) |
| `HomeController` | Trang chủ |
| `ExploreController` | Duyệt phiên đấu giá + bộ lọc |
| `ProductCardController` | Card sản phẩm (component tái sử dụng) |
| `AuctionDetailController` | Chi tiết sản phẩm + đặt giá |
| `CreateItemsController` | Form đăng sản phẩm mới |
| `MyProductsController` | Quản lý sản phẩm (Seller) |
| `SalesManagementController` | Quản lý doanh số |
| `UserDashboardController` | Hồ sơ người dùng |
| `ProfileEditController` | Chỉnh sửa hồ sơ |
| `ChangePasswordController` | Đổi mật khẩu |
| `DepositController` | Nạp tiền ví |
| `WithdrawController` | Rút tiền ví |
| `TransactionHistoryController` | Lịch sử giao dịch |
| `FavoriteController` | Danh sách yêu thích |
| `NotificationController` | Trung tâm thông báo |
| `AuctionHistoryController` | Lịch sử đấu giá |
| `PurchaseHistoryController` | Lịch sử mua hàng |
| `ReviewController` | Đánh giá người bán |
| `BuyerReviewsController` | Xem đánh giá từ người mua |
| `SettingsController` | Cài đặt (theme, tài khoản) |
| `AdminController` | Layout khu vực Admin |
| `AdminDashboardController` | 4 thẻ chỉ số tổng quan |
| `AdminUserListController` | Quản lý người dùng |
| `AdminAuctionListController` | Quản lý phiên đấu giá |

#### Service (22 class — tất cả method `static`)

| Service | Trách nhiệm |
|---|---|
| `AuthService` | `login`, `logout`, `requestOtp`, `resetPassword` |
| `RegisterService` | `register` |
| `UserService` | `loadProfile`, `updateProfile`, `changePassword`, `deposit`, `withdraw`, `loadMyBids`, `loadTransactions` |
| `AuctionService` | `loadAll`, `loadDetail`, subscribe / unsubscribe sự kiện đấu giá |
| `BidService` | `placeBid`, `setAutoBid`, `loadHistory` |
| `SellerService` | `createItem`, `updateItem`, `deleteItem`, `getMyListings` |
| `FavoriteService` | `loadAll`, `add`, `remove` |
| `NotificationService` | `loadAll`, `markRead`, `deleteNotification`, `subscribePush` |
| `RatingService` | `rateSeller` |
| `AdminService` | `getStats`, `getUsers`, `updateUserStatus`, `getAuctions`, `cancelAuction` |

#### Networking

- **`AuctionClient`** (singleton) — quản lý socket TCP, hai luồng `PrintWriter` (ghi) và `BufferedReader` (đọc), cung cấp `sendRequest()`, `sendAuthenticatedRequest()` và cơ chế `reconnect()`.
- **`ResponseDispatcher`** — định tuyến response từ server về đúng callback:
  - Nếu response có `requestId` → tra cứu map callback (đăng ký qua `register()`).
  - Nếu response là broadcast (`eventType != null`, `requestId == null`) → gọi tất cả listener đã `subscribe()` cho `EventType` đó.

#### Util

- **`SceneManager`** — quản lý chuyển scene giữa các màn hình.
- **`SessionManager`** — lưu thông tin user đã đăng nhập (singleton, phía client).
- **`AlertUtil`** — wrapper cho `Alert` của JavaFX: `showInfo`, `showWarning`, `showError`, `showConfirm`.
- **`ModalUtil`** — hiển thị màn hình modal (popup overlay).
- **`PopupUtil`** — hiển thị popup nhỏ (tooltip-style).
- **`UIFormatter`** — format tiền tệ VND, ngày tháng, thời gian đếm ngược.

### 8.3. `auction-server`

Module phía server — đã hoàn chỉnh với đầy đủ nghiệp vụ, networking và persistence:

#### Network

- **`AuctionServer`** — khởi động server, khởi tạo toàn bộ dependency (DAO, Service, Controller), lắng nghe kết nối TCP và spawn `ClientHandler` cho mỗi client.
- **`ClientHandler`** — xử lý một kết nối TCP: đọc request JSON, chuyển sang `AppDispatcher`, gửi response.
- **`AppDispatcher`** — định tuyến `ActionType` → Controller method tương ứng.
- **`RequestParser`** — parse JSON thành `ServerRequest<?>` với đúng kiểu payload.
- **`BroadcastChannel` / `BroadcastChannelImpl`** — ghi response broadcast tới tập hợp các `ClientHandler` đang kết nối.
- **`AuctionEventSubscriber`** — lắng nghe `AuctionEventBus` và gửi broadcast khi có sự kiện.

#### Auth

- **`SessionManager`** — quản lý phiên đăng nhập phía server: map `token → userId`, cấp và thu hồi token.

#### Controller (8 class)

`AuthController`, `UserController`, `ItemController`, `BidController`, `AdminController`, `FavoriteController`, `NotificationController`, `RatingController`.

#### Service — Auction

| Class | Vai trò |
|---|---|
| `AuctionService` | CRUD phiên đấu giá, mở / đóng phiên |
| `AuctionClosingService` | Scheduler định kỳ kiểm tra phiên hết hạn và đóng |
| `AntiSnipingService` | Gia hạn phiên khi có bid trong giây cuối |
| `AutoBidService` | Tự động đặt giá thay user đến ngưỡng tối đa |
| `HybridBidExecutor` | Điều phối luồng bid: thông thường + auto-bid |
| `BidValidator` | Kiểm tra điều kiện hợp lệ của bid (số dư, giá tối thiểu, trạng thái phiên) |
| `BidProcessor` | Thực thi bid đã được validate: cập nhật DB, trừ số dư |
| `AuctionEventBus` | Pub/sub nội bộ server cho các sự kiện đấu giá |

#### Service — Item

`ItemWriteService`, `ItemQueryService`, `FavoriteService`, `ItemSpecValidator`.

#### Service — User

`AuthService`, `RegisterService`, `ProfileService`, `BalanceService`, `PasswordService`, `RatingService`, `AdminUserService`, `NotificationService`, `UserPolicy`.

#### DAO (10 class)

`UserDAO`, `ItemDAO`, `AuctionSessionDAO`, `BidTransactionDAO`, `AutoBidDAO`, `FavoriteDAO`, `NotificationDAO`, `RatingDAO`, `TransactionDAO`, `DatabaseConnection` (HikariCP).

#### Util

`FileUtil`, `PasswordUtil`.

---

## 9. Giao thức truyền tin

### Định dạng tin nhắn

Mọi gói tin gửi qua socket đều là **một dòng JSON** (delimited bằng `\n`), serialize bằng Gson.

#### Request (client → server)

```json
{
  "requestId": "uuid-v4",
  "action": "bid_place",
  "token": "<session token>",
  "payload": { ... }
}
```

Trường `action` ánh xạ tới một giá trị trong enum `ActionType`. Một số `ActionType` thường dùng:

| `ActionType` | Mục đích |
|---|---|
| `LOGIN`, `REGISTER`, `LOGOUT` | Xác thực |
| `AUTH_REQUEST_OTP`, `AUTH_RESET_PASSWORD` | Quên mật khẩu |
| `ITEM_GET_ALL`, `ITEM_GET_DETAIL` | Tra cứu sản phẩm |
| `ITEM_CREATE`, `ITEM_UPDATE`, `ITEM_DELETE`, `ITEM_MY_LISTINGS` | Quản lý sản phẩm (Seller) |
| `BID_PLACE`, `BID_AUTO`, `BID_HISTORY` | Đặt giá |
| `USER_PROFILE`, `USER_DEPOSIT`, `USER_WITHDRAW`, `USER_BIDS`, `USER_CHANGE_PASSWORD`, `USER_UPDATE_PROFILE` | Hồ sơ + ví |
| `WALLET_GET_TRANSACTIONS` | Lịch sử giao dịch |
| `FAVORITE_LIST`, `FAVORITE_ADD`, `FAVORITE_REMOVE` | Yêu thích |
| `NOTIF_GET_ALL`, `NOTIF_MARK_READ`, `NOTIF_DELETE` | Thông báo |
| `ADMIN_DASHBOARD`, `ADMIN_GET_USERS`, `ADMIN_UPDATE_USER_STATUS`, `ADMIN_GET_AUCTIONS`, `ADMIN_CANCEL_AUCTION` | Admin |

#### Response (server → client)

```json
{
  "requestId": "uuid-v4 hoặc null nếu là broadcast",
  "success": true,
  "message": "OK",
  "eventType": "price_update | auction_ended | notification | null",
  "data": { ... }
}
```

### Sự kiện đẩy (broadcast)

Khi `requestId == null` và `eventType != null`, đây là tin nhắn server chủ động đẩy:

| `EventType` | Khi nào? | Payload |
|---|---|---|
| `PRICE_UPDATE` | Có bid mới trong phiên đấu giá | `PriceUpdateBroadcastResponse` |
| `AUCTION_ENDED` | Phiên kết thúc (`SOLD` / `NO_BID`) | `AuctionEndedBroadcastResponse` |
| `NOTIFICATION` | Thông báo cá nhân (outbid, thắng, ...) | `NotificationDTO` |

Client subscribe bằng `ResponseDispatcher.subscribe(EventType, listener)` và nhận callback ngay khi sự kiện về.

---

## 10. Thiết kế giao diện

### Design Tokens (`tokens.css`)

Nhóm sử dụng **design system** thống nhất qua file `tokens.css`, định nghĩa biến CSS dùng chung:

#### Bảng màu thương hiệu

| Token | Hex | Mô tả |
|---|---|---|
| `-fx-color-primary` | `#F97316` | Cam chủ đạo (Auctiva orange) |
| `-fx-color-primary-light` | `#FB923C` | Cam nhạt — hover |
| `-fx-color-primary-bg` | `#FFF3EB` | Nền cam rất nhạt |
| `-fx-color-navy` | `#1E3A6E` | Xanh navy của logo |

#### Bảng màu trạng thái

| Token | Hex | Dùng cho |
|---|---|---|
| `-fx-color-danger` | `#E03030` | Lỗi, xoá, ngừng |
| `-fx-color-success` | `#22C55E` | Thành công |
| `-fx-color-info` | `#3B82F6` | Thông tin |
| `-fx-color-warning` | `#D4A853` | Cảnh báo |

### Phân lớp CSS (6 file)

| File | Vai trò |
|---|---|
| `tokens.css` | Biến thiết kế dùng chung — màu, kích thước, spacing |
| `layout.css` | Bố cục — sidebar, header, content area |
| `components.css` | Component tái sử dụng — button, card, input, table |
| `light-theme.css` | Giao diện sáng (mặc định) |
| `dark-theme.css` | Giao diện tối — chuyển qua Settings |
| `login.css` | Style riêng cho màn hình đăng nhập / đăng ký |

### FXML — 40 file

Mỗi file FXML tương ứng một Controller hoặc component tái sử dụng. Đặt tên theo quy ước `<Name>View.fxml` cho màn hình lớn, `<Name>Content.fxml` cho nội dung được nhúng, và `<Name>.fxml` cho component (`ProductCard.fxml`, `MainLayout.fxml`, `AdminLayout.fxml`).

---

## 11. Kiểm thử (Unit Test)

Hệ thống sử dụng **JUnit Jupiter 5.10.0** + **Mockito 5.8.0** + **JaCoCo 0.8.11** đo coverage.

### Phạm vi test

| Module | Số file test | Phạm vi |
|---|---|---|
| `auction-common` | 39 file | Entity, Enum, DTO |
| `auction-server` | 51 file | Controller, Service (auction/item/user), DAO, network (AppDispatcher, ClientHandler, BroadcastChannel, RequestParser), auth |
| **Tổng** | **90 file** | — |

### Chạy test

```bash
# Toàn bộ (bao gồm Checkstyle + JaCoCo report)
mvn clean verify

# Chỉ chạy test, bỏ qua Checkstyle
mvn test

# Chỉ một module
mvn test -pl auction-common
mvn test -pl auction-server

# Một file cụ thể
mvn test -pl auction-server -Dtest=BidValidatorTest
```

Sau khi chạy `mvn verify`, báo cáo coverage JaCoCo được tạo tại `auction-server/target/site/jacoco/index.html`.

### Quy ước viết test

- Mỗi method `@Test` có `@DisplayName` bằng tiếng Việt có dấu để báo cáo dễ đọc.
- Test nhóm theo `@Nested class` theo chức năng (Builder, Logic, Validation, Edge cases).
- Dùng `@ParameterizedTest` cho input dạng bảng.
- Mock dependency bằng `@ExtendWith(MockitoExtension.class)` khi test Service và Controller.

---

## 12. CI/CD

GitHub Actions chạy mỗi lần `push` hoặc `pull_request` vào branch `main`. Pipeline gồm 4 bước:

1. **Checkout code** (`actions/checkout@v4`).
2. **Cài JDK 17 Temurin** + cache Maven (`actions/setup-java@v4`).
3. **`mvn clean verify`** — kích hoạt Checkstyle (Google Style Guide) + chạy toàn bộ JUnit test + sinh báo cáo JaCoCo.
4. **`mvn package -DskipTests`** + upload `target/*.jar` làm build artifact (`auction-system-binary`) để thầy hoặc team tải về chạy thử.

File cấu hình: [`.github/workflows/java-ci.yaml`](.github/workflows/java-ci.yaml).

---

## 13. Quy ước phát triển

### Branch model

| Branch | Vai trò |
|---|---|
| `main` | Mã ổn định — chỉ merge sau khi review, CI phải xanh |
| `dev_entity` | Phát triển phía server + entity (do nhóm trưởng quản) |
| `dev_ui` | Phát triển phía client + UI |
| `feature/*` | Branch tính năng riêng của mỗi thành viên |

### Quy ước commit

Theo chuẩn **Conventional Commits**:

| Prefix | Khi nào dùng |
|---|---|
| `feat:` | Thêm tính năng mới |
| `fix:` | Sửa bug |
| `test:` | Thêm / sửa test |
| `refactor:` | Refactor không đổi hành vi |
| `docs:` | Sửa README, comment, javadoc |
| `chore:` | Cấu hình build, dependency |

Ví dụ:

```
feat: them man hinh dau gia truc tiep voi anti-sniping
fix: sua bug fullName() khong duoc ke thua tu super class
test: them unit test cho BidValidator va AuctionEventBus
chore: them JaCoCo plugin vao pom.xml
```

### Coding style

- Tuân thủ **Google Java Style Guide** — enforced bởi `maven-checkstyle-plugin` ở phase `validate` (chạy trước khi compile).
- Tên class: `PascalCase`. Tên method, biến: `camelCase`. Hằng: `UPPER_SNAKE_CASE`.
- Javadoc bắt buộc cho mọi class public và method public phức tạp.
- Comment trong code dùng tiếng Việt (thống nhất trong cùng file).

---

## 14. Link PDF và Video
- Báo cáo dự án: https://drive.google.com/file/d/1eR1wb-UqqglAeWlAqGboDHq4wGkRmjm5/view?usp=drive_link hoặc có ở trong thư mục docs, file Báo cáo bài tập lớn.pdf
- Video các chức năng chính: https://drive.google.com/file/d/1M-5LSMYr_cJn2FQiv1XfZz6BtfbRqUVo/view?usp=drive_link

---

## 15. Nhóm phát triển

**Group 8 — Lớp Lập trình Nâng cao — UET-VNU.**

| Họ tên | Vai trò |
|---|---|
| Phạm Ngọc Thành | Trưởng nhóm — kiến trúc server, entity, business logic |
| Đỗ Đức Quân | UI/UX, JavaFX Controller, Unit Test |
| Nguyễn Đắc Thịnh | Server networking, Server controller |
|Vũ Minh Triết | Client Networking, UI/UX Controller |
