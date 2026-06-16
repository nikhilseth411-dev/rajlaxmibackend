-- ================================================================
-- RajLaxmi Jewellers — V1 Initial Schema
-- Database: PostgreSQL
-- Note: JPA creates tables via ddl-auto=update in dev.
-- This script is for reference and manual production deployments.
-- ================================================================

-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(15),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    is_email_verified BOOLEAN DEFAULT FALSE,
    email_otp VARCHAR(6),
    otp_expires_at TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    token_hash VARCHAR(64) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(200),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) UNIQUE NOT NULL,
    description VARCHAR(300),
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    parent_id BIGINT REFERENCES categories(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Products
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    sku VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES categories(id),
    product_category VARCHAR(30),
    metal_type VARCHAR(20) DEFAULT 'GOLD',
    gold_purity VARCHAR(10),
    weight_grams NUMERIC(8,3) NOT NULL,
    making_charges NUMERIC(10,2) DEFAULT 0,
    making_charges_type VARCHAR(10) DEFAULT 'PER_GRAM',
    stone_charges NUMERIC(10,2) DEFAULT 0,
    gst_percentage NUMERIC(5,2) DEFAULT 3.00,
    is_bis_hallmarked BOOLEAN DEFAULT TRUE,
    bis_hallmark_number VARCHAR(20),
    occasion VARCHAR(50),
    gender VARCHAR(50),
    dimensions VARCHAR(100),
    finish VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    is_featured BOOLEAN DEFAULT FALSE,
    is_new_arrival BOOLEAN DEFAULT FALSE,
    is_best_seller BOOLEAN DEFAULT FALSE,
    meta_title VARCHAR(200),
    meta_description VARCHAR(350),
    slug VARCHAR(150) UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_product_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_product_active ON products(is_active);

-- Product Images
CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    alt_text VARCHAR(200),
    is_primary BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0
);

-- Inventory
CREATE TABLE IF NOT EXISTS inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT UNIQUE REFERENCES products(id),
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT DEFAULT 0,
    low_stock_threshold INT DEFAULT 2,
    is_in_stock BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMP DEFAULT NOW()
);

-- Gold Prices
CREATE TABLE IF NOT EXISTS gold_prices (
    id BIGSERIAL PRIMARY KEY,
    rate_24k NUMERIC(10,2) NOT NULL,
    rate_22k NUMERIC(10,2) NOT NULL,
    rate_18k NUMERIC(10,2) NOT NULL,
    change_amount NUMERIC(8,2) DEFAULT 0,
    change_percent NUMERIC(5,2) DEFAULT 0,
    fetched_at TIMESTAMP NOT NULL,
    currency VARCHAR(20) DEFAULT 'INR',
    source VARCHAR(50) DEFAULT 'metals.live',
    is_current BOOLEAN DEFAULT FALSE,
    is_admin_override BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_gold_current ON gold_prices(is_current);

-- Silver Prices
CREATE TABLE IF NOT EXISTS silver_prices (
    id BIGSERIAL PRIMARY KEY,
    rate_per_gram NUMERIC(8,2) NOT NULL,
    change_amount NUMERIC(6,2) DEFAULT 0,
    change_percent NUMERIC(5,2) DEFAULT 0,
    fetched_at TIMESTAMP NOT NULL,
    currency VARCHAR(20) DEFAULT 'INR',
    source VARCHAR(50) DEFAULT 'metals.live',
    is_current BOOLEAN DEFAULT FALSE,
    is_admin_override BOOLEAN DEFAULT FALSE
);

-- Carts
CREATE TABLE IF NOT EXISTS carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    product_id BIGINT REFERENCES products(id),
    quantity INT NOT NULL DEFAULT 1,
    added_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, product_id)
);

-- Wishlists
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    product_id BIGINT REFERENCES products(id),
    added_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, product_id)
);

-- Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    user_email VARCHAR(150),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(200),
    description VARCHAR(300),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);
