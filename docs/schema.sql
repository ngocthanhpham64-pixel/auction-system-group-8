-- ================================================================
-- schema.sql
-- Auction System — Database Schema
-- Chạy theo thứ tự: users trước, item sau (vì item có FK → users)
-- ================================================================

-- Tạo database nếu chưa có
CREATE DATABASE IF NOT EXISTS auction_db
    CHARACTER SET utf8mb4        -- hỗ trợ tiếng Việt + emoji
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

    -- ── Specs linh hoạt (Hybrid) ──────────────────────────────
    -- Chỉ chứa dữ liệu hiển thị — không cần filter hay index
    -- Ví dụ: {"brand":"Apple","storage":"512GB","warranty":"12"}
    -- NULL được phép: category OTHER có thể không có specs
    specs           JSON            NULL,

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
-- BẢNG 4: payment_transaction
-- ================================================================
CREATE TABLE IF NOT EXISTS payment_transaction (
    transaction_id  VARCHAR(100)    NOT NULL,
    user_id         INT             NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    
    -- Loại giao dịch: TOP_UP (Nạp), DEPOSIT (Cọc), REFUND (Hoàn), WITHDRAW (Rút)
    transaction_type VARCHAR(20)    NOT NULL, 
    
    -- Trạng thái: PENDING (Đang chờ), SUCCESS (Thành công), FAILED (Thất bại)
    status          VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS', 
    
    -- Ghi chú giao dịch (Ví dụ: "Nạp tiền qua VNPay", "Cọc cho sản phẩm Laptop")
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
    item_id         INT             NOT NULL,
    score           DECIMAL(3,1)    NOT NULL,
    comment         TEXT            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (rating_id),
    CONSTRAINT uq_buyer_item UNIQUE (buyer_id, item_id),
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
        ON UPDATE CASCADE,
    CONSTRAINT chk_rating_logic
        CHECK (score BETWEEN 0.0 AND 5.0 AND seller_id != buyer_id)
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