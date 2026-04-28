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
        CHECK (balance >= 0),
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

    -- UPCOMING | ACTIVE | SOLD | CANCELLED | ENDED_NO_BID
    status          VARCHAR(20)     NOT NULL DEFAULT 'UPCOMING',

    -- NEW | USED | REFURBISHED — tách thành cột thật vì hay filter
    condition_type  VARCHAR(20)     NULL,

    -- ── Giá ───────────────────────────────────────────────────
    -- DECIMAL(15,2): tối đa 999,999,999,999,999.99 VND — đủ dùng
    starting_price  DECIMAL(15,2)   NOT NULL,
    current_price   DECIMAL(15,2)   NOT NULL,

    -- Đếm số lượt bid — tách thành cột để hiển thị nhanh
    -- không cần COUNT(*) từ bảng bid_transaction mỗi lần
    bid_count       INT             NOT NULL DEFAULT 0,

    -- ── Thời gian đấu giá ─────────────────────────────────────
    -- Lưu UTC — anti-sniping service so sánh với Instant.now()
    start_time      DATETIME        NULL,
    end_time        DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ── Specs linh hoạt (Hybrid) ──────────────────────────────
    -- Chỉ chứa dữ liệu hiển thị — không cần filter hay index
    -- Ví dụ: {"brand":"Apple","storage":"512GB","warranty":"12"}
    -- NULL được phép: category OTHER có thể không có specs
    specs           JSON            NULL,

    -- ── Constraints ───────────────────────────────────────────
    PRIMARY KEY (item_id),

    CONSTRAINT fk_item_seller
        FOREIGN KEY (seller_id)
        REFERENCES users (user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_price_positive
        CHECK (starting_price > 0 AND current_price > 0),

    CONSTRAINT chk_current_gte_starting
        CHECK (current_price >= starting_price),

    CONSTRAINT chk_time_order
        CHECK (start_time IS NULL
            OR end_time IS NULL
            OR end_time > start_time),

    CONSTRAINT chk_status_item
        CHECK (status IN (
            'UPCOMING','ACTIVE','SOLD','CANCELLED','ENDED_NO_BID')),

    CONSTRAINT chk_condition
        CHECK (condition_type IS NULL
            OR condition_type IN ('NEW','USED','REFURBISHED')),

    -- ── Indexes ───────────────────────────────────────────────
    -- Những query phổ biến nhất của hệ thống đấu giá:

    -- "Lọc theo danh mục đang active"
    INDEX idx_category_status   (category, status),

    -- "Sort theo giá" — trang tìm kiếm
    INDEX idx_current_price     (current_price),

    -- "Sắp hết giờ" — anti-sniping + hiển thị countdown
    INDEX idx_end_time          (end_time),

    -- "Item của seller X" — trang quản lý của người bán
    INDEX idx_seller_id         (seller_id),

    -- "Item mới nhất" — trang chủ
    INDEX idx_created_at        (created_at)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
