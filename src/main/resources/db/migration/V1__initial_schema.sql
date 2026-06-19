-- RajLaxmi Jewellers - complete PostgreSQL baseline schema.
-- This schema intentionally matches the current Hibernate physical naming so
-- spring.jpa.hibernate.ddl-auto=validate can run safely after Flyway migration.

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    email VARCHAR(150) NOT NULL,
    email_otp VARCHAR(6),
    failed_login_attempts INTEGER NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL,
    is_email_verified BOOLEAN NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    locked_until TIMESTAMP(6),
    otp_expires_at TIMESTAMP(6),
    password VARCHAR(255) NOT NULL,
    password_reset_expires_at TIMESTAMP(6),
    password_reset_token VARCHAR(255),
    phone VARCHAR(15),
    role VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP(6),
    CONSTRAINT idx_users_email UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'CUSTOMER'))
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    description VARCHAR(300),
    image_url VARCHAR(255),
    is_active BOOLEAN NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL,
    updated_at TIMESTAMP(6),
    parent_id BIGINT,
    CONSTRAINT idx_category_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE gold_prices (
    id BIGSERIAL PRIMARY KEY,
    change_amount NUMERIC(8,2),
    change_percent NUMERIC(5,2),
    currency VARCHAR(20),
    fetched_at TIMESTAMP(6) NOT NULL,
    is_admin_override BOOLEAN NOT NULL,
    is_current BOOLEAN NOT NULL,
    rate18k NUMERIC(10,2) NOT NULL,
    rate22k NUMERIC(10,2) NOT NULL,
    rate24k NUMERIC(10,2) NOT NULL,
    source VARCHAR(50)
);

CREATE TABLE silver_prices (
    id BIGSERIAL PRIMARY KEY,
    change_amount NUMERIC(6,2),
    change_percent NUMERIC(5,2),
    currency VARCHAR(255),
    fetched_at TIMESTAMP(6) NOT NULL,
    is_admin_override BOOLEAN NOT NULL,
    is_current BOOLEAN NOT NULL,
    rate_per_gram NUMERIC(8,2) NOT NULL,
    source VARCHAR(255)
);

CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    description VARCHAR(200),
    discount_type VARCHAR(10) NOT NULL,
    discount_value NUMERIC(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL,
    max_discount_amount NUMERIC(10,2),
    minimum_order_amount NUMERIC(10,2),
    usage_limit INTEGER NOT NULL,
    used_count INTEGER NOT NULL,
    valid_from TIMESTAMP(6),
    valid_until TIMESTAMP(6),
    CONSTRAINT idx_coupon_code UNIQUE (code)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    bis_hallmark_number VARCHAR(20),
    created_at TIMESTAMP(6) NOT NULL,
    description TEXT,
    dimensions VARCHAR(100),
    finish VARCHAR(100),
    gender VARCHAR(50),
    gold_purity VARCHAR(10),
    gst_percentage NUMERIC(5,2),
    is_active BOOLEAN NOT NULL,
    is_best_seller BOOLEAN NOT NULL,
    is_bis_hallmarked BOOLEAN NOT NULL,
    is_featured BOOLEAN NOT NULL,
    is_new_arrival BOOLEAN NOT NULL,
    making_charges NUMERIC(10,2) NOT NULL,
    making_charges_type VARCHAR(10),
    meta_description VARCHAR(350),
    meta_title VARCHAR(200),
    metal_type VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    occasion VARCHAR(50),
    product_category VARCHAR(30),
    sku VARCHAR(50) NOT NULL,
    slug VARCHAR(150),
    stone_charges NUMERIC(10,2),
    updated_at TIMESTAMP(6),
    weight_grams NUMERIC(8,3) NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT idx_product_sku UNIQUE (sku),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT products_gold_purity_check CHECK (gold_purity IS NULL OR gold_purity IN ('GOLD_18K', 'GOLD_22K', 'GOLD_24K')),
    CONSTRAINT products_product_category_check CHECK (
        product_category IS NULL OR product_category IN (
            'GOLD_JEWELLERY', 'DIAMOND_JEWELLERY', 'BRIDAL_COLLECTION',
            'TEMPLE_JEWELLERY', 'ANTIQUE_JEWELLERY', 'SILVER_COLLECTION',
            'MANGALSUTRA', 'EARRINGS', 'NECKLACES', 'BANGLES', 'RINGS',
            'PENDANTS', 'CHAINS', 'ANKLETS'
        )
    )
);

CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    is_in_stock BOOLEAN NOT NULL,
    last_updated TIMESTAMP(6),
    low_stock_threshold INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT uk_inventory_product UNIQUE (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    alt_text VARCHAR(200),
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    ip_address VARCHAR(45),
    is_revoked BOOLEAN NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    user_agent VARCHAR(200),
    user_id BIGINT NOT NULL,
    CONSTRAINT idx_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    address_type VARCHAR(10) NOT NULL,
    city VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    district VARCHAR(100) NOT NULL,
    full_name VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL,
    landmark VARCHAR(100),
    phone VARCHAR(15) NOT NULL,
    pincode VARCHAR(6) NOT NULL,
    state VARCHAR(50) NOT NULL,
    street_address VARCHAR(200) NOT NULL,
    updated_at TIMESTAMP(6),
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    added_at TIMESTAMP(6) NOT NULL,
    quantity INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_carts_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE wishlists (
    id BIGSERIAL PRIMARY KEY,
    added_at TIMESTAMP(6) NOT NULL,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT uk_wishlist_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_wishlists_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    admin_note VARCHAR(500),
    cancelled_at TIMESTAMP(6),
    confirmed_at TIMESTAMP(6),
    coupon_code VARCHAR(20),
    created_at TIMESTAMP(6) NOT NULL,
    customer_note VARCHAR(500),
    delivered_at TIMESTAMP(6),
    discount_amount NUMERIC(10,2),
    gold_rate22kat_order NUMERIC(10,2),
    gold_rate24kat_order NUMERIC(10,2),
    grand_total NUMERIC(12,2) NOT NULL,
    order_number VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20),
    payment_status VARCHAR(20),
    payment_transaction_id VARCHAR(100),
    shipped_at TIMESTAMP(6),
    shipping_charge NUMERIC(8,2),
    shipping_city VARCHAR(255) NOT NULL,
    shipping_full_name VARCHAR(255) NOT NULL,
    shipping_landmark VARCHAR(255),
    shipping_partner VARCHAR(100),
    shipping_phone VARCHAR(255) NOT NULL,
    shipping_pincode VARCHAR(6) NOT NULL,
    shipping_state VARCHAR(255) NOT NULL,
    shipping_street VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    total_gst NUMERIC(10,2) NOT NULL,
    tracking_number VARCHAR(100),
    updated_at TIMESTAMP(6),
    user_id BIGINT NOT NULL,
    CONSTRAINT idx_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT orders_payment_method_check CHECK (payment_method IS NULL OR payment_method IN ('UPI_QR', 'RAZORPAY', 'COD', 'BANK_TRANSFER')),
    CONSTRAINT orders_payment_status_check CHECK (payment_status IS NULL OR payment_status IN ('PENDING', 'PENDING_VERIFICATION', 'SUCCESS', 'FAILED', 'REFUND_PENDING', 'REFUNDED')),
    CONSTRAINT orders_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURN_REQUESTED', 'RETURNED'))
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    base_metal_value NUMERIC(10,2) NOT NULL,
    bis_hallmark_number VARCHAR(255),
    gold_purity VARCHAR(10),
    gold_rate_used NUMERIC(10,2) NOT NULL,
    gst_amount NUMERIC(10,2) NOT NULL,
    gst_percentage NUMERIC(5,2) NOT NULL,
    is_bis_hallmarked BOOLEAN NOT NULL,
    making_charges NUMERIC(10,2) NOT NULL,
    metal_type VARCHAR(20),
    primary_image_url VARCHAR(255),
    product_id BIGINT,
    product_name VARCHAR(200) NOT NULL,
    product_sku VARCHAR(50) NOT NULL,
    product_slug VARCHAR(100),
    quantity INTEGER NOT NULL,
    stone_charges NUMERIC(10,2),
    total_price NUMERIC(12,2) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    weight_grams NUMERIC(8,3),
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT order_items_gold_purity_check CHECK (gold_purity IS NULL OR gold_purity IN ('GOLD_18K', 'GOLD_22K', 'GOLD_24K'))
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    admin_notes VARCHAR(500),
    amount NUMERIC(12,2) NOT NULL,
    cod_notes VARCHAR(200),
    created_at TIMESTAMP(6) NOT NULL,
    failure_reason VARCHAR(500),
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    gateway_signature VARCHAR(500),
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP(6),
    upi_id VARCHAR(100),
    upi_qr_image_url VARCHAR(500),
    utr_number VARCHAR(50),
    verified_at TIMESTAMP(6),
    verified_by VARCHAR(100),
    order_id BIGINT NOT NULL,
    CONSTRAINT uk_payments_order UNIQUE (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT payments_payment_method_check CHECK (payment_method IN ('UPI_QR', 'RAZORPAY', 'COD', 'BANK_TRANSFER')),
    CONSTRAINT payments_status_check CHECK (status IN ('PENDING', 'PENDING_VERIFICATION', 'SUCCESS', 'FAILED', 'REFUND_PENDING', 'REFUNDED'))
);

CREATE TABLE store_visits (
    id BIGSERIAL PRIMARY KEY,
    admin_note VARCHAR(500),
    contact_phone VARCHAR(15),
    created_at TIMESTAMP(6) NOT NULL,
    purpose_note VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP(6),
    visit_date DATE NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_store_visits_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    description VARCHAR(300),
    entity_id BIGINT,
    entity_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    new_value TEXT,
    old_value TEXT,
    user_agent VARCHAR(200),
    user_email VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL
);

CREATE INDEX idx_address_pincode ON addresses (pincode);
CREATE INDEX idx_address_user ON addresses (user_id);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit_logs (user_id);
CREATE INDEX idx_cart_user ON carts (user_id);
CREATE INDEX idx_coupon_active ON coupons (is_active);
CREATE INDEX idx_gold_price_current ON gold_prices (is_current);
CREATE INDEX idx_gold_price_fetched_at ON gold_prices (fetched_at);
CREATE INDEX idx_order_created ON orders (created_at);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);
CREATE INDEX idx_order_status ON orders (status);
CREATE INDEX idx_order_user ON orders (user_id);
CREATE INDEX idx_payment_order ON payments (order_id);
CREATE INDEX idx_payment_status ON payments (status);
CREATE INDEX idx_payment_utr ON payments (utr_number);
CREATE INDEX idx_product_active ON products (is_active);
CREATE INDEX idx_product_category ON products (category_id);
CREATE INDEX idx_product_images_product ON product_images (product_id);
CREATE INDEX idx_product_purity ON products (gold_purity);
CREATE INDEX idx_refresh_token_user ON refresh_tokens (user_id);
CREATE INDEX idx_silver_price_current ON silver_prices (is_current);
CREATE INDEX idx_silver_price_fetched_at ON silver_prices (fetched_at);
CREATE INDEX idx_users_phone ON users (phone);
CREATE INDEX idx_visit_date ON store_visits (visit_date);
CREATE INDEX idx_visit_status ON store_visits (status);
CREATE INDEX idx_visit_user ON store_visits (user_id);
CREATE INDEX idx_wishlist_user ON wishlists (user_id);
