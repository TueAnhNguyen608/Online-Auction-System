-- Tạo database (cho H2 in-memory)
CREATE SCHEMA IF NOT EXISTS auction_db;
SET SCHEMA auction_db;

-- Bảng users
CREATE TABLE IF NOT EXISTS users (
                                     id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'BIDDER',
    balance DECIMAL(15,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    full_name VARCHAR(100),
    phone VARCHAR(20)
    );

-- Bảng items
CREATE TABLE IF NOT EXISTS items (
                                     id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    starting_price DECIMAL(15,2) NOT NULL,
    current_price DECIMAL(15,2) NOT NULL,
    current_bidder_id VARCHAR(36),
    current_bidder_name VARCHAR(100),
    seller_id VARCHAR(36) NOT NULL,
    seller_name VARCHAR(100) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    bid_count INT DEFAULT 0,
    step_price DECIMAL(15,2) DEFAULT 0,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Bảng bids
CREATE TABLE IF NOT EXISTS bids (
                                    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    bidder_id VARCHAR(36) NOT NULL,
    bidder_name VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_auto_bid BOOLEAN DEFAULT FALSE
    );

-- Bảng auto_bid_settings
CREATE TABLE IF NOT EXISTS auto_bid_settings (
                                                 user_id VARCHAR(36) NOT NULL,
    item_id VARCHAR(36) NOT NULL,
    max_amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, item_id)
    );

-- ========== DỮ LIỆU TEST USERS ==========
INSERT INTO users (id, username, password, email, role, balance, full_name, phone) VALUES
                                                                                       ('test_seller', 'seller1', '123456', 'seller1@test.com', 'SELLER', 0, 'Test Seller', '0981000001'),
                                                                                       ('test_bidder_1', 'bidder1', '123456', 'bidder1@test.com', 'BIDDER', 10000000, 'Test Bidder 1', '0981000002'),
                                                                                       ('test_bidder_2', 'bidder2', '123456', 'bidder2@test.com', 'BIDDER', 10000000, 'Test Bidder 2', '0981000003'),
                                                                                       ('test_bidder_3', 'bidder3', '123456', 'bidder3@test.com', 'BIDDER', 10000000, 'Test Bidder 3', '0981000004');

-- Foreign keys
ALTER TABLE items ADD CONSTRAINT IF NOT EXISTS fk_items_seller FOREIGN KEY (seller_id) REFERENCES users(id);
ALTER TABLE bids ADD CONSTRAINT IF NOT EXISTS fk_bids_item FOREIGN KEY (item_id) REFERENCES items(id);
ALTER TABLE bids ADD CONSTRAINT IF NOT EXISTS fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id);
ALTER TABLE auto_bid_settings ADD CONSTRAINT IF NOT EXISTS fk_auto_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE auto_bid_settings ADD CONSTRAINT IF NOT EXISTS fk_auto_item FOREIGN KEY (item_id) REFERENCES items(id);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_items_status ON items(status);
CREATE INDEX IF NOT EXISTS idx_items_end_time ON items(end_time);
CREATE INDEX IF NOT EXISTS idx_bids_item_id ON bids(item_id);
CREATE INDEX IF NOT EXISTS idx_bids_bidder_id ON bids(bidder_id);
CREATE INDEX IF NOT EXISTS idx_auto_user_item ON auto_bid_settings(user_id, item_id););