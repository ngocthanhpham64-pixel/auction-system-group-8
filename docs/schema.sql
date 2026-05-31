-- ================================================================
-- schema.sql
-- Auction System — Database Schema
-- Chạy theo thứ tự: users trước, item sau (vì item có FK → users)
-- ================================================================

-- Tạo database nếu chưa có
CREATE DATABASE IF NOT EXISTS auction_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auction_db;

-- ================================================================
-- BẢNG 1: users
-- Fixed columns — không dùng JSON vì mọi user có cùng cấu trúc
-- ================================================================

CREATE TABLE IF NOT EXISTS users (

    -- ── Identity ──────────────────────────────────────────────
    user_id         INT             NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL,
    email           VARCHAR(100)    NOT NULL,
    -- BCrypt output cố định 60 ký tự, dùng 255 để dự phòng
    password_hash   VARCHAR(255)    NOT NULL,

    -- ── Profile (nullable — user có thể bỏ trống lúc đăng ký) ─
    full_name       VARCHAR(100)    NULL,
    phone           VARCHAR(20)     NULL,
    address         VARCHAR(255)    NULL,
    avatar_url      VARCHAR(500)    NULL,

    -- ── Phân quyền ────────────────────────────────────────────
    -- Lưu dạng "BIDDER" | "BIDDER,SELLER" | "ADMIN"
    -- Không tách bảng roles riêng vì tối đa 3 giá trị cố định
    roles           VARCHAR(100)    NOT NULL DEFAULT 'BIDDER',

    -- ACTIVE | SUSPENDED | BANNED
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    -- NULL nếu không phải admin
    -- "MODERATOR" | "SUPER_ADMIN" nếu là admin
    admin_level     VARCHAR(20)     NULL,

    -- ── Tài chính ─────────────────────────────────────────────
    -- DECIMAL thay vì DOUBLE để tránh lỗi floating point
    -- 15 chữ số tổng, 2 chữ số thập phân → đủ cho VND
    balance         DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    frozen_balance  DECIMAL(15,2)   NOT NULL DEFAULT 0.00,

    -- NULL = chưa có ai đánh giá (khác với 0.0 = bị đánh giá xấu)
    seller_rating   DECIMAL(3,1)    NULL,

    -- ── Thống kê ──────────────────────────────────────────────
    total_bids_placed   INT         NOT NULL DEFAULT 0,
    total_items_sold    INT         NOT NULL DEFAULT 0,

    -- ── Timestamps (UTC) ──────────────────────────────────────
    -- Lưu UTC để nhất quán với Instant trong Java
    -- Tầng UI mới convert sang giờ địa phương (Asia/Ho_Chi_Minh)
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   DATETIME        NULL,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,

    -- ── Constraints ───────────────────────────────────────────
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_username  (username),
    UNIQUE KEY uq_email     (email),

    -- ── Indexes ───────────────────────────────────────────────
    -- Những trường hay xuất hiện trong WHERE clause
    INDEX idx_status        (status),
    INDEX idx_roles         (roles),

    -- ── Validation ────────────────────────────────────────────
    CONSTRAINT chk_balance
        CHECK (balance >= 0 AND frozen_balance >= 0),
    CONSTRAINT chk_seller_rating
        CHECK (seller_rating IS NULL
            OR seller_rating BETWEEN 0.0 AND 5.0),
    CONSTRAINT chk_status_users
        CHECK (status IN ('ACTIVE','SUSPENDED','BANNED')),
    CONSTRAINT chk_admin_level
        CHECK (admin_level IS NULL
            OR admin_level IN ('MODERATOR','SUPER_ADMIN'))

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- BẢNG 2: item
-- Hybrid design: cột thật cho những gì cần filter/sort,
-- JSON cho specs linh hoạt theo từng category
-- ================================================================

CREATE TABLE IF NOT EXISTS item (

    -- ── Identity ──────────────────────────────────────────────
    item_id         INT             NOT NULL AUTO_INCREMENT,

    -- FK → users: người đăng bán
    -- ON DELETE RESTRICT: không xoá user nếu còn item
    seller_id       INT             NOT NULL,

    -- ── Thông tin cơ bản ──────────────────────────────────────
    title           VARCHAR(200)    NOT NULL,
    description     TEXT            NULL,

    -- Giá trị khớp với ItemCategory enum trong Java
    category        VARCHAR(50)     NOT NULL,

    -- NEW | USED | REFURBISHED — tách thành cột thật vì hay filter
    condition_type  VARCHAR(20)     NULL,

    -- DRAFT | LISTED | SOLD ...
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',

    -- ── Specs linh hoạt (Hybrid) ──────────────────────────────
    -- Chỉ chứa dữ liệu hiển thị — không cần filter hay index
    -- Ví dụ: {"brand":"Apple","storage":"512GB","warranty":"12"}
    -- NULL được phép: category OTHER có thể không có specs
    specs           JSON            NULL,

    -- Chứa mảng các đường dẫn ảnh (URL/Path)
    image_urls      JSON            NULL,

    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,

    -- ── Constraints ───────────────────────────────────────────
    PRIMARY KEY (item_id),

    CONSTRAINT fk_item_seller
        FOREIGN KEY (seller_id)
        REFERENCES users (user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_condition
        CHECK (condition_type IS NULL
            OR condition_type IN ('NEW','USED','REFURBISHED','LIKENEW','USED_AS_IS','DAMAGE')),

    -- ── Indexes ───────────────────────────────────────────────
    INDEX idx_seller_id         (seller_id),
    INDEX idx_category          (category),
    INDEX idx_created_at        (created_at)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- BẢNG 3: auction_session
-- Quản lý các phiên đấu giá của sản phẩm
-- ================================================================

CREATE TABLE IF NOT EXISTS auction_session (

    -- ── Identity ──────────────────────────────────────────────
    session_id      INT             NOT NULL AUTO_INCREMENT,
    item_id         INT             NOT NULL,

    -- ── Giá ───────────────────────────────────────────────────
    starting_price  DECIMAL(15,2)   NOT NULL,
    current_price   DECIMAL(15,2)   NOT NULL,

    -- UPCOMING | ACTIVE | SOLD | CANCELLED | ENDED_NO_BID
    status          VARCHAR(20)     NOT NULL DEFAULT 'UPCOMING',

    -- Đếm số lượt bid
    bid_count       INT             NOT NULL DEFAULT 0,

    -- ── Thời gian đấu giá ─────────────────────────────────────
    start_time      DATETIME        NOT NULL,
    end_time        DATETIME        NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    highest_bidder_id INT           NULL,

    -- ── Constraints ───────────────────────────────────────────
    PRIMARY KEY (session_id),

    CONSTRAINT fk_session_item
        FOREIGN KEY (item_id)
        REFERENCES item (item_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_session_highest_bidder
        FOREIGN KEY (highest_bidder_id)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT chk_price_positive
        CHECK (starting_price >= 0 AND current_price >= 0),

    CONSTRAINT chk_current_gte_starting
        CHECK (current_price >= starting_price),

    CONSTRAINT chk_time_order
        CHECK (end_time > start_time),

    CONSTRAINT chk_status_session
        CHECK (status IN (
            'UPCOMING','ACTIVE','SOLD','CANCELLED','ENDED_NO_BID')),

    -- ── Indexes ───────────────────────────────────────────────
    INDEX idx_status            (status),
    INDEX idx_current_price     (current_price),
    INDEX idx_end_time          (end_time),
    INDEX idx_item_id           (item_id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ================================================================
-- BẢNG 4: wallet_transaction
-- ================================================================
CREATE TABLE IF NOT EXISTS wallet_transaction (
    transaction_id  VARCHAR(100)    NOT NULL,
    user_id         INT             NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    
    -- Loại giao dịch: TOP_UP (Nạp), DEPOSIT (Cọc), REFUND (Hoàn), WITHDRAW (Rút)
    transaction_type VARCHAR(20)    NOT NULL, 
    
    -- Trạng thái: PENDING (Đang chờ), SUCCESS (Thành công), FAILED (Thất bại)
    status          VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS', 

    -- Phiên đấu giá
    session_id      INT             NULL,
    
    -- Ghi chú giao dịch
    description     VARCHAR(255)    NULL, 

    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transaction_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- BẢNG 5: Đánh giá người bán (ratings)
-- ================================================================
CREATE TABLE IF NOT EXISTS ratings (
    rating_id       INT             NOT NULL AUTO_INCREMENT,
    seller_id       INT             NOT NULL,
    buyer_id        INT             NOT NULL,
    buyer_username  VARCHAR(50)     NOT NULL,
    item_id         INT             NOT NULL,
    score           DECIMAL(3,1)    NOT NULL,
    comment         TEXT            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (rating_id),
    CONSTRAINT uq_rater_item UNIQUE (buyer_id, item_id),
    CONSTRAINT fk_rating_seller
        FOREIGN KEY (seller_id) REFERENCES users (user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_rating_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_rating_item
        FOREIGN KEY (item_id) REFERENCES item (item_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- BẢNG 6: Lịch sử đặt giá (bid_transaction)
-- ================================================================
CREATE TABLE IF NOT EXISTS bid_transaction (
    bid_id          INT             NOT NULL AUTO_INCREMENT,
    session_id      INT             NOT NULL,
    bidder_id       INT             NOT NULL,
    bid_amount      DECIMAL(15,2)   NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'LEADER',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (bid_id),
    CONSTRAINT fk_bid_session
        FOREIGN KEY (session_id) REFERENCES auction_session (session_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_bid_bidder
        FOREIGN KEY (bidder_id) REFERENCES users (user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    INDEX idx_session_bid (session_id, bid_amount DESC)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- BẢNG 7: favorites (Sản phẩm yêu thích)
-- ================================================================

CREATE TABLE IF NOT EXISTS favorites (
    user_id         INT             NOT NULL,
    item_id         INT             NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, item_id),
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_item FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- BẢNG 8: notifications (Thông báo hệ thống)
-- ================================================================

CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT             NOT NULL AUTO_INCREMENT,
    user_id         INT             NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    message         TEXT            NOT NULL,
    type            VARCHAR(50)     NOT NULL, 
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,

    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- BẢNG 9: auto_bid_config (Cấu hình đặt giá tự động)
-- ================================================================
CREATE TABLE IF NOT EXISTS auto_bid_config (
    config_id       INT             NOT NULL AUTO_INCREMENT,
    session_id      INT             NOT NULL,
    user_id         INT             NOT NULL,
    max_price       DECIMAL(15,2)   NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (config_id),
    CONSTRAINT fk_autobid_session 
        FOREIGN KEY (session_id) 
        REFERENCES auction_session (session_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_autobid_user 
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `users` VALUES (1,'nva','nva@gmail.com','$2a$12$btAXgzt7w1dFHndh7pT2UulSRE.VLvcrtcWbiCMlL.U/L9YEzbak2','nguyen van a','0987654321','',NULL,'BIDDER,SELLER','ACTIVE',NULL,98900000.00,1100000.00,0.0,0,0,'2026-05-27 13:45:45','2026-05-29 08:28:39',0),(2,'nvb','nvb@gmai.com','$2a$12$99sbbEPMABbNPAwhEEmYSeqJGrrq0cSToZLOJtEYuUusWVNiZmBcG','nguyen van b','0987654322','',NULL,'BIDDER,SELLER','ACTIVE',NULL,0.00,0.00,0.0,0,0,'2026-05-28 07:36:05','2026-05-29 07:27:16',0),(3,'nvc','nvc@gmail.com','$2a$12$6mHVNx6/8sonh5BQOFNycOJHtPAZr8UacVIODjbdH2l2pe7E4AV1.','nguyen van c','0987654323','',NULL,'SELLER,BIDDER','ACTIVE',NULL,0.00,0.00,0.0,0,0,'2026-05-29 07:49:16','2026-05-29 07:49:40',0);
INSERT IGNORE INTO `item` VALUES (1,1,'Đồng hồ pateck philippe','Đồng hồ chính hãng, đầy đủ hộp, thẻ','WATCHES','NEW','LISTED',NULL,'[\"http://localhost:8081/uploads/2063c8b0-1b6a-4a32-8ead-5226c967c747.jpg\"]','2026-05-27 13:47:01',0),(2,1,'Đồng hồ rolex','Đồng hồ chính hãng, bảo hành 10 năm','WATCHES','NEW','LISTED',NULL,'[\"http://localhost:8081/uploads/8fbad2e0-a325-4be7-9aad-373d6f7e056e.jpg\"]','2026-05-27 13:54:11',0),(3,1,'Tranh mona lisa','Tranh nàng mona lisa vô cùng quý hiếm','ART','NEW','LISTED',NULL,'[\"http://localhost:8081/uploads/fa0f8a7a-f66c-45ea-9e36-80f79a4ab1c6.jpg\"]','2026-05-27 13:57:46',0),(4,1,'Iphone 16 pro max','Hang chinh hang VN/A, bao hanh 6 thang','ELECTRONICS','USED','LISTED',NULL,'[\"http://localhost:8081/uploads/37ff16b5-0f44-4c11-a671-21701177fa35.jpg\"]','2026-05-27 14:01:38',0),(5,1,'ip 15 pro','Hàng chính hãng, cũ đẹp, mặt lưng xước nhẹ. Dung lượng 128 GB','ELECTRONICS','USED','LISTED',NULL,'[\"http://localhost:8081/uploads/7838fbf5-b8b2-45d3-b9ba-cc887f6ce496.jpg\"]','2026-05-27 14:05:04',0),(6,1,'iphone 12 cũ','sản phẩm như mới (99%), hàng mỹ','ELECTRONICS','LIKENEW','LISTED',NULL,'[\"http://localhost:8081/uploads/8060e965-7bd0-4155-9d60-f1350fe96335.jpg\"]','2026-05-27 14:11:11',0),(7,1,'Sinh viên Bách Khoa','Chăm ngoan, học giỏi, biết làm việc nhà','OTHER','LIKENEW','LISTED',NULL,'[\"http://localhost:8081/uploads/3593d9cc-f1f3-4756-b215-069103db9d04.jpg\"]','2026-05-27 14:17:13',0),(8,1,'Xe máy dream','Sản phẩm đã qua sửa chữa, sản xuất năm 2000, máy chạy vẫn mượt (không chạy được xăng E10)','VEHICLES','REFURBISHED','LISTED',NULL,'[\"http://localhost:8081/uploads/7b2f8eb0-4908-4252-aa5b-16da2cdde53c.jpg\"]','2026-05-27 14:24:14',0),(9,1,'Xe ga SH','Xe mới, 150cc, cực đẹp','VEHICLES','NEW','LISTED',NULL,'[\"http://localhost:8081/uploads/b5b38521-e31c-48a2-872d-31f5c14b3926.jpg\"]','2026-05-27 14:27:39',0),(10,2,'Đồng xu cổ','Đồng xu cổ từ xa xưa, còn nguyên vẹn, mẫu mã đẹp','ANTIQUES','USED_AS_IS','LISTED',NULL,'[\"http://localhost:8081/uploads/d47efa4a-d1a2-43f4-8441-a3ff0ac2c67c.jpg\"]','2026-05-28 07:43:47',0),(11,2,'20 đồng tiền xu cổ','20 đồng tiền cổ Phù Nam Óc Eo từ thế kỷ (1-3) đẹp hiếm.','ANTIQUES','USED_AS_IS','LISTED',NULL,'[\"http://localhost:8081/uploads/8463cd48-e8dc-4b04-80d3-87e49de87a48.jpg\"]','2026-05-28 07:46:13',0),(12,2,'Bộ tem hitler','Bộ tem hitler, đẹp như mới, cực kì hiếm có','ANTIQUES','LIKENEW','LISTED',NULL,'[\"http://localhost:8081/uploads/86728a74-8afa-4c92-bbc8-9cd22f19131d.jpg\"]','2026-05-28 07:48:51',0),(13,2,'Đồng hồ Atmos 540 lắc bi Thuỵ Sĩ đẹp, mới 98%','sản phẩm đẹp, chính hãng','WATCHES','LIKENEW','LISTED',NULL,'[\"http://localhost:8081/uploads/a4a321c6-89b4-446f-bb8f-71adaf69a159.jpg\"]','2026-05-28 07:51:02',0),(14,2,'Đồng hồ để bàn','Đồng hồ Thụy Sĩ cực đẹp, decor phòng ngủ, phòng khách','WATCHES','USED','LISTED',NULL,'[\"http://localhost:8081/uploads/29cfa784-7862-4310-9eed-aac74a5f6029.jpg\"]','2026-05-28 07:53:05',0),(15,2,'máy giặt electrolux','sản phẩm chính hãng','ELECTRONICS','USED','LISTED',NULL,'[\"http://localhost:8081/uploads/8696fb88-e7c0-4646-a682-67d967f1be4a.jpg\"]','2026-05-29 07:29:23',0),(16,2,'máy giặt samsung','sản phẩm sử dụng được 1 năm, chuyển nhà đã có máy giặt mới, cần bán','ELECTRONICS','USED_AS_IS','LISTED',NULL,'[\"http://localhost:8081/uploads/2f18b4d1-db89-439d-afe6-77dce9a5ab4e.jpg\"]','2026-05-29 07:31:01',0),(17,2,'Nike air jordan x travis scott','Sản phẩm chính hãng của Nike','FASHION','USED','LISTED',NULL,'[\"http://localhost:8081/uploads/e8ac4a08-c2dd-4d01-b0ff-73401fcfef9e.jpg\"]','2026-05-29 07:45:38',0),(18,3,'Kim cương 4.6 ly','kim cương E SI1 G','JEWELRY','NEW','LISTED',NULL,'[\"http://localhost:8081/uploads/320ffb0c-d88c-428c-8522-9d333edb1420.jpg\"]','2026-05-29 07:56:32',0),(19,3,'Bộ trang sức Jaminary','Bộ Trang Sức Jaminary H&A 2C','JEWELRY','NEW','LISTED',NULL,'[\"http://localhost:8081/uploads/f94d2374-09c1-416e-b2f5-a35d6f7b0cbd.jpg\"]','2026-05-29 07:58:01',0),(20,3,'Ô tô Vinfast VF6','Ô tô VF6 hàng siêu lướt','VEHICLES','LIKENEW','LISTED',NULL,'[\"http://localhost:8081/uploads/9efa6409-ef2d-4eb5-869c-a8c90b8759e5.jpg\", \"http://localhost:8081/uploads/df1cb814-053b-441c-9636-741049efef35.jpg\"]','2026-05-29 08:03:44',0);
INSERT IGNORE INTO `auction_session` VALUES (1,1,300000000.00,300000000.00,'ACTIVE',0,'2026-05-27 13:50:00','2026-05-30 13:00:00','2026-05-27 13:47:01',0,NULL),(2,2,100000000.00,100000000.00,'ACTIVE',0,'2026-05-27 15:00:00','2026-05-30 09:00:00','2026-05-27 13:54:11',0,NULL),(3,3,100000000.00,100000000.00,'ACTIVE',0,'2026-05-27 13:58:00','2026-06-30 13:00:00','2026-05-27 13:57:46',0,NULL),(4,4,20000000.00,20000000.00,'ACTIVE',0,'2026-05-27 14:02:00','2026-06-29 21:00:00','2026-05-27 14:01:38',0,NULL),(5,5,14000000.00,14000000.00,'ACTIVE',0,'2026-05-27 14:05:04','2026-06-29 15:00:04','2026-05-27 14:05:04',0,NULL),(6,6,6000000.00,6000000.00,'ACTIVE',0,'2026-05-27 14:11:11','2026-06-27 22:01:11','2026-05-27 14:11:11',0,NULL),(7,7,1000000.00,1000000.00,'ACTIVE',0,'2026-05-27 15:00:00','2026-07-01 13:00:00','2026-05-27 14:17:13',0,NULL),(8,8,5000000.00,5000000.00,'ACTIVE',0,'2026-05-27 15:00:00','2026-07-04 03:00:00','2026-05-27 14:24:14',0,NULL),(9,9,50000000.00,50000000.00,'ACTIVE',0,'2026-05-27 15:00:00','2026-06-30 16:00:00','2026-05-27 14:27:39',0,NULL),(10,10,200000.00,200000.00,'ACTIVE',0,'2026-05-28 08:00:00','2026-06-30 13:00:00','2026-05-28 07:43:47',0,NULL),(11,11,1000000.00,1000000.00,'ACTIVE',0,'2026-05-28 08:00:00','2026-07-01 20:00:00','2026-05-28 07:46:13',0,NULL),(12,12,500000.00,500000.00,'ACTIVE',0,'2026-05-28 07:50:00','2026-06-29 21:00:00','2026-05-28 07:48:51',0,NULL),(13,13,900000.00,900000.00,'ACTIVE',0,'2026-05-28 07:51:02','2026-06-29 22:00:02','2026-05-28 07:51:02',0,NULL),(14,14,1000000.00,1100000.00,'ACTIVE',1,'2026-05-28 08:00:00','2026-06-28 21:00:00','2026-05-28 07:53:05',0,1),(15,15,7000000.00,7000000.00,'ACTIVE',0,'2026-05-29 08:00:00','2026-06-30 16:00:00','2026-05-29 07:29:23',0,NULL),(16,16,4000000.00,4000000.00,'ACTIVE',0,'2026-05-29 08:00:00','2026-06-30 13:00:00','2026-05-29 07:31:01',0,NULL),(17,17,30000000.00,30000000.00,'ACTIVE',0,'2026-05-29 07:50:00','2026-07-01 10:00:00','2026-05-29 07:45:38',0,NULL),(18,18,19000000.00,19000000.00,'ACTIVE',0,'2026-05-29 08:00:00','2026-07-02 18:00:00','2026-05-29 07:56:32',0,NULL),(19,19,70000000.00,70000000.00,'ACTIVE',0,'2026-05-29 08:00:00','2026-06-28 17:00:00','2026-05-29 07:58:01',0,NULL),(20,20,620000000.00,620000000.00,'ACTIVE',0,'2026-05-29 08:05:00','2026-07-01 22:00:00','2026-05-29 08:03:44',0,NULL);
INSERT IGNORE INTO `wallet_transaction` VALUES ('d50640e4-2cc1-4e21-b085-f54482f38e33',1,100000000.00,'DEPOSIT','SUCCESS',0,'Giao dịch thành công qua BANK_TRANSFER','2026-05-29 15:55:19'),('f1ae0bdd-3bcb-475d-9abe-320913c0bfb7',1,1100000.00,'BID_HOLD','SUCCESS',14,'Ký quỹ đặt giá','2026-05-29 15:55:41');
