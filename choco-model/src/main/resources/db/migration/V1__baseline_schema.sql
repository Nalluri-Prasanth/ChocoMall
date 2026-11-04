-- ChocoMall baseline schema (generated from the domain model; edit with care).
-- Portable between MySQL 8 and H2 (MySQL mode).

CREATE TABLE ums_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(100) NOT NULL,
    level INT NOT NULL DEFAULT 0,
    sort INT NOT NULL DEFAULT 0,
    name VARCHAR(100),
    icon VARCHAR(200),
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    create_time DATETIME(6) NOT NULL
);

CREATE TABLE ums_resource_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME(6) NOT NULL
);

CREATE TABLE ums_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    name VARCHAR(200) NOT NULL,
    url VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_ums_resource_category_id FOREIGN KEY (category_id) REFERENCES ums_resource_category (id)
);
CREATE INDEX idx_ums_resource_category_id ON ums_resource (category_id);

CREATE TABLE ums_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    admin_count INT NOT NULL DEFAULT 0,
    create_time DATETIME(6) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0
);
CREATE TABLE ums_role_menu_relation (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_ums_role_menu_relation_owner FOREIGN KEY (role_id) REFERENCES ums_role (id),
    CONSTRAINT fk_ums_role_menu_relation_value FOREIGN KEY (menu_id) REFERENCES ums_menu (id)
);
CREATE TABLE ums_role_resource_relation (
    role_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, resource_id),
    CONSTRAINT fk_ums_role_resource_relation_owner FOREIGN KEY (role_id) REFERENCES ums_role (id),
    CONSTRAINT fk_ums_role_resource_relation_value FOREIGN KEY (resource_id) REFERENCES ums_resource (id)
);

CREATE TABLE ums_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    icon VARCHAR(500),
    email VARCHAR(100),
    nick_name VARCHAR(200),
    note VARCHAR(500),
    create_time DATETIME(6) NOT NULL,
    login_time DATETIME(6),
    status INT NOT NULL DEFAULT 1
);
CREATE TABLE ums_admin_role_relation (
    admin_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (admin_id, role_id),
    CONSTRAINT fk_ums_admin_role_relation_owner FOREIGN KEY (admin_id) REFERENCES ums_admin (id),
    CONSTRAINT fk_ums_admin_role_relation_value FOREIGN KEY (role_id) REFERENCES ums_role (id)
);

CREATE TABLE ums_admin_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    create_time DATETIME(6) NOT NULL,
    ip VARCHAR(64),
    address VARCHAR(100),
    user_agent VARCHAR(500),
    CONSTRAINT fk_ums_admin_login_log_admin_id FOREIGN KEY (admin_id) REFERENCES ums_admin (id)
);
CREATE INDEX idx_ums_admin_login_log_admin_id ON ums_admin_login_log (admin_id);

CREATE TABLE ums_member_level (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    growth_point INT NOT NULL DEFAULT 0,
    default_status BOOLEAN NOT NULL DEFAULT FALSE,
    points_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    free_delivery_threshold DECIMAL(10,2),
    note VARCHAR(200)
);

CREATE TABLE ums_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level_id BIGINT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(100),
    status INT NOT NULL DEFAULT 1,
    create_time DATETIME(6) NOT NULL,
    icon VARCHAR(500),
    gender INT NOT NULL DEFAULT 0,
    birthday DATE,
    city VARCHAR(64),
    job VARCHAR(100),
    personalized_sign VARCHAR(200),
    integration INT NOT NULL DEFAULT 0,
    growth INT NOT NULL DEFAULT 0,
    history_integration INT NOT NULL DEFAULT 0,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_ums_member_level_id FOREIGN KEY (level_id) REFERENCES ums_member_level (id)
);
CREATE INDEX idx_ums_member_level_id ON ums_member (level_id);

CREATE TABLE ums_member_receive_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(20),
    default_status BOOLEAN NOT NULL DEFAULT FALSE,
    post_code VARCHAR(20),
    province VARCHAR(100),
    city VARCHAR(100),
    region VARCHAR(100),
    detail_address VARCHAR(300),
    CONSTRAINT fk_ums_member_receive_address_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id)
);
CREATE INDEX idx_ums_member_receive_address_member_id ON ums_member_receive_address (member_id);

CREATE TABLE pms_brand (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    region VARCHAR(50),
    associated_market VARCHAR(100),
    segment VARCHAR(100),
    typical_formats VARCHAR(300),
    shop_role VARCHAR(200),
    evidence_status VARCHAR(200),
    reference_url VARCHAR(500),
    stocking_status VARCHAR(30) NOT NULL DEFAULT 'CANDIDATE',
    authorized_supplier VARCHAR(200),
    landed_cost_note VARCHAR(300),
    logo VARCHAR(500),
    show_status BOOLEAN NOT NULL DEFAULT TRUE,
    sort INT NOT NULL DEFAULT 0,
    product_count INT NOT NULL DEFAULT 0,
    create_time DATETIME(6) NOT NULL
);
CREATE INDEX idx_pms_brand_region ON pms_brand (region);

CREATE TABLE pms_product_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    level INT NOT NULL DEFAULT 0,
    product_count INT NOT NULL DEFAULT 0,
    nav_status BOOLEAN NOT NULL DEFAULT TRUE,
    show_status BOOLEAN NOT NULL DEFAULT TRUE,
    sort INT NOT NULL DEFAULT 0,
    icon VARCHAR(500),
    keywords VARCHAR(200),
    description VARCHAR(500)
);

CREATE TABLE pms_chocolate_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    classification VARCHAR(100),
    distinguishing VARCHAR(500),
    best_shop_use VARCHAR(300),
    priority VARCHAR(30) NOT NULL
);

CREATE TABLE pms_flavor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    flavor_group VARCHAR(100),
    name VARCHAR(100) NOT NULL,
    chocolate_base VARCHAR(100),
    application VARCHAR(300),
    priority VARCHAR(30) NOT NULL
);
CREATE INDEX idx_pms_flavor_flavor_group ON pms_flavor (flavor_group);

CREATE TABLE pms_allergen (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE pms_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_id BIGINT,
    category_id BIGINT NOT NULL,
    chocolate_type_id BIGINT,
    product_sn VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    pic VARCHAR(500),
    sub_title VARCHAR(300),
    description VARCHAR(2000),
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    stock INT NOT NULL DEFAULT 0,
    lock_stock INT NOT NULL DEFAULT 0,
    low_stock INT NOT NULL DEFAULT 10,
    sale INT NOT NULL DEFAULT 0,
    unit VARCHAR(30) DEFAULT 'piece',
    weight_grams INT,
    kind VARCHAR(30) NOT NULL DEFAULT 'MADE_IN_SHOP',
    launch_tier VARCHAR(30) NOT NULL DEFAULT 'TEST',
    selling_format VARCHAR(200),
    sales_purpose VARCHAR(300),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    new_status BOOLEAN NOT NULL DEFAULT FALSE,
    recommend_status BOOLEAN NOT NULL DEFAULT FALSE,
    preorder_only BOOLEAN NOT NULL DEFAULT FALSE,
    build_a_box BOOLEAN NOT NULL DEFAULT FALSE,
    requires_cold_chain BOOLEAN NOT NULL DEFAULT FALSE,
    allergens_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    shelf_life_days INT,
    ingredient_cost DECIMAL(10,2),
    packaging_cost DECIMAL(10,2),
    labor_cost DECIMAL(10,2),
    selling_fee_rate DECIMAL(5,4) NOT NULL DEFAULT 0,
    keywords VARCHAR(200),
    sort INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_pms_product_brand_id FOREIGN KEY (brand_id) REFERENCES pms_brand (id),
    CONSTRAINT fk_pms_product_category_id FOREIGN KEY (category_id) REFERENCES pms_product_category (id),
    CONSTRAINT fk_pms_product_chocolate_type_id FOREIGN KEY (chocolate_type_id) REFERENCES pms_chocolate_type (id)
);
CREATE INDEX idx_pms_product_brand_id ON pms_product (brand_id);
CREATE INDEX idx_pms_product_category_id ON pms_product (category_id);
CREATE INDEX idx_pms_product_chocolate_type_id ON pms_product (chocolate_type_id);
CREATE INDEX idx_pms_product_published ON pms_product (published);
CREATE TABLE pms_product_flavor (
    product_id BIGINT NOT NULL,
    flavor_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, flavor_id),
    CONSTRAINT fk_pms_product_flavor_owner FOREIGN KEY (product_id) REFERENCES pms_product (id),
    CONSTRAINT fk_pms_product_flavor_value FOREIGN KEY (flavor_id) REFERENCES pms_flavor (id)
);
CREATE TABLE pms_product_allergen (
    product_id BIGINT NOT NULL,
    allergen_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, allergen_id),
    CONSTRAINT fk_pms_product_allergen_owner FOREIGN KEY (product_id) REFERENCES pms_product (id),
    CONSTRAINT fk_pms_product_allergen_value FOREIGN KEY (allergen_id) REFERENCES pms_allergen (id)
);

CREATE TABLE pms_product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    units INT NOT NULL DEFAULT 1,
    price DECIMAL(10,2) NOT NULL,
    pic VARCHAR(500),
    sort INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_pms_product_sku_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_pms_product_sku_product_id ON pms_product_sku (product_id);

CREATE TABLE pms_product_claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    claim_type VARCHAR(30) NOT NULL,
    evidence VARCHAR(500),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by VARCHAR(64),
    verified_time DATETIME(6),
    CONSTRAINT uq_pms_product_claim_product_id_claim_type UNIQUE (product_id, claim_type),
    CONSTRAINT fk_pms_product_claim_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_pms_product_claim_product_id ON pms_product_claim (product_id);

CREATE TABLE pms_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    member_nick_name VARCHAR(64),
    star INT NOT NULL DEFAULT 5,
    content VARCHAR(1000),
    show_status BOOLEAN NOT NULL DEFAULT TRUE,
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_pms_comment_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id),
    CONSTRAINT fk_pms_comment_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id)
);
CREATE INDEX idx_pms_comment_product_id ON pms_comment (product_id);
CREATE INDEX idx_pms_comment_member_id ON pms_comment (member_id);

CREATE TABLE pms_preorder_window (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    occasion VARCHAR(100),
    order_cutoff DATETIME(6) NOT NULL,
    fulfil_from DATE NOT NULL,
    fulfil_to DATE NOT NULL,
    capacity_units INT NOT NULL,
    reserved_units INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    note VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE pms_experience_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    duration_minutes INT DEFAULT 60,
    capacity INT NOT NULL,
    booked INT NOT NULL DEFAULT 0,
    price DECIMAL(10,2),
    location VARCHAR(200),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    CONSTRAINT fk_pms_experience_session_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_pms_experience_session_product_id ON pms_experience_session (product_id);

CREATE TABLE pms_stock_movement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    change_units INT NOT NULL,
    balance_after INT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    ref_sn VARCHAR(64),
    operator VARCHAR(64),
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_pms_stock_movement_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_pms_stock_movement_product_id ON pms_stock_movement (product_id);

CREATE TABLE sms_bundle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    pic VARCHAR(500),
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    start_time DATETIME(6),
    end_time DATETIME(6),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    sort INT NOT NULL DEFAULT 0
);

CREATE TABLE sms_bundle_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bundle_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT,
    quantity INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_sms_bundle_item_bundle_id FOREIGN KEY (bundle_id) REFERENCES sms_bundle (id),
    CONSTRAINT fk_sms_bundle_item_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_sms_bundle_item_bundle_id ON sms_bundle_item (bundle_id);
CREATE INDEX idx_sms_bundle_item_product_id ON sms_bundle_item (product_id);

CREATE TABLE ums_member_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    flavor_id BIGINT NOT NULL,
    consent_time DATETIME(6) NOT NULL,
    CONSTRAINT uq_ums_member_preference_member_id_flavor_id UNIQUE (member_id, flavor_id),
    CONSTRAINT fk_ums_member_preference_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id),
    CONSTRAINT fk_ums_member_preference_flavor_id FOREIGN KEY (flavor_id) REFERENCES pms_flavor (id)
);
CREATE INDEX idx_ums_member_preference_member_id ON ums_member_preference (member_id);
CREATE INDEX idx_ums_member_preference_flavor_id ON ums_member_preference (flavor_id);

CREATE TABLE ums_member_product_collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT uq_ums_member_product_collection_member_id_product_id UNIQUE (member_id, product_id),
    CONSTRAINT fk_ums_member_product_collection_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id),
    CONSTRAINT fk_ums_member_product_collection_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_ums_member_product_collection_member_id ON ums_member_product_collection (member_id);
CREATE INDEX idx_ums_member_product_collection_product_id ON ums_member_product_collection (product_id);

CREATE TABLE ums_member_read_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    view_time DATETIME(6) NOT NULL,
    CONSTRAINT uq_ums_member_read_history_member_id_product_id UNIQUE (member_id, product_id),
    CONSTRAINT fk_ums_member_read_history_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id),
    CONSTRAINT fk_ums_member_read_history_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_ums_member_read_history_member_id ON ums_member_read_history (member_id);
CREATE INDEX idx_ums_member_read_history_product_id ON ums_member_read_history (product_id);

CREATE TABLE ums_member_brand_attention (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    brand_id BIGINT NOT NULL,
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT uq_ums_member_brand_attention_member_id_brand_id UNIQUE (member_id, brand_id),
    CONSTRAINT fk_ums_member_brand_attention_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id),
    CONSTRAINT fk_ums_member_brand_attention_brand_id FOREIGN KEY (brand_id) REFERENCES pms_brand (id)
);
CREATE INDEX idx_ums_member_brand_attention_member_id ON ums_member_brand_attention (member_id);
CREATE INDEX idx_ums_member_brand_attention_brand_id ON ums_member_brand_attention (brand_id);

CREATE TABLE ums_integration_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    change_type VARCHAR(30) NOT NULL,
    change_count INT NOT NULL,
    balance_after INT NOT NULL,
    note VARCHAR(200),
    order_sn VARCHAR(64),
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_ums_integration_history_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id)
);
CREATE INDEX idx_ums_integration_history_member_id ON ums_integration_history (member_id);

CREATE TABLE ops_production_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_code VARCHAR(40) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    process_code VARCHAR(10),
    quantity_units INT NOT NULL,
    waste_units INT NOT NULL DEFAULT 0,
    produced_at DATETIME(6),
    best_before DATE,
    allergen_checked BOOLEAN NOT NULL DEFAULT FALSE,
    temper_test_passed BOOLEAN,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    note VARCHAR(500),
    created_by VARCHAR(64),
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_ops_production_batch_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_ops_production_batch_product_id ON ops_production_batch (product_id);

CREATE TABLE ops_guide_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guide_type VARCHAR(30) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    detail VARCHAR(1500),
    purpose VARCHAR(500),
    applies_to VARCHAR(500),
    control_note VARCHAR(1000),
    phase VARCHAR(50),
    priority VARCHAR(30),
    reference_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    implemented_by VARCHAR(300),
    sort INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_ops_guide_item_guide_type ON ops_guide_item (guide_type);

CREATE TABLE ops_guide_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(200) NOT NULL,
    scope VARCHAR(500),
    checked_on DATE,
    reference_url VARCHAR(500)
);

CREATE TABLE cms_food_pairing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    food_group VARCHAR(100),
    food VARCHAR(100) NOT NULL,
    chocolate_combination VARCHAR(200),
    shop_format VARCHAR(200),
    priority VARCHAR(30) NOT NULL
);
CREATE INDEX idx_cms_food_pairing_food_group ON cms_food_pairing (food_group);

CREATE TABLE cms_serving_format (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    eating_format VARCHAR(100) NOT NULL,
    how_to_serve VARCHAR(300),
    suitable_product VARCHAR(200),
    sales_application VARCHAR(200),
    priority VARCHAR(30) NOT NULL
);

CREATE TABLE cms_subject (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    pic VARCHAR(500),
    description VARCHAR(500),
    content VARCHAR(4000),
    category VARCHAR(100),
    show_status BOOLEAN NOT NULL DEFAULT TRUE,
    recommend_status BOOLEAN NOT NULL DEFAULT FALSE,
    sort INT NOT NULL DEFAULT 0,
    read_count INT NOT NULL DEFAULT 0,
    create_time DATETIME(6) NOT NULL
);
CREATE TABLE cms_subject_product (
    subject_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (subject_id, product_id),
    CONSTRAINT fk_cms_subject_product_owner FOREIGN KEY (subject_id) REFERENCES cms_subject (id),
    CONSTRAINT fk_cms_subject_product_value FOREIGN KEY (product_id) REFERENCES pms_product (id)
);

CREATE TABLE cms_help_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    show_status BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE cms_help (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(4000),
    show_status BOOLEAN NOT NULL DEFAULT TRUE,
    read_count INT NOT NULL DEFAULT 0,
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_cms_help_category_id FOREIGN KEY (category_id) REFERENCES cms_help_category (id)
);
CREATE INDEX idx_cms_help_category_id ON cms_help (category_id);

CREATE TABLE sms_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_type VARCHAR(30) NOT NULL DEFAULT 'ALL',
    name VARCHAR(200) NOT NULL,
    total_count INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    per_limit INT NOT NULL DEFAULT 1,
    min_spend DECIMAL(10,2) NOT NULL DEFAULT 0,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NOT NULL,
    note VARCHAR(500),
    received_count INT NOT NULL DEFAULT 0,
    used_count INT NOT NULL DEFAULT 0,
    code VARCHAR(64) UNIQUE,
    member_level_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_sms_coupon_member_level_id FOREIGN KEY (member_level_id) REFERENCES ums_member_level (id)
);
CREATE INDEX idx_sms_coupon_member_level_id ON sms_coupon (member_level_id);
CREATE TABLE sms_coupon_product (
    coupon_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, product_id),
    CONSTRAINT fk_sms_coupon_product_owner FOREIGN KEY (coupon_id) REFERENCES sms_coupon (id),
    CONSTRAINT fk_sms_coupon_product_value FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE TABLE sms_coupon_category (
    coupon_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, category_id),
    CONSTRAINT fk_sms_coupon_category_owner FOREIGN KEY (coupon_id) REFERENCES sms_coupon (id),
    CONSTRAINT fk_sms_coupon_category_value FOREIGN KEY (category_id) REFERENCES pms_product_category (id)
);

CREATE TABLE sms_coupon_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    coupon_code VARCHAR(64),
    get_type VARCHAR(30) NOT NULL,
    create_time DATETIME(6) NOT NULL,
    use_status VARCHAR(30) NOT NULL DEFAULT 'UNUSED',
    use_time DATETIME(6),
    order_id BIGINT,
    order_sn VARCHAR(64),
    CONSTRAINT fk_sms_coupon_history_coupon_id FOREIGN KEY (coupon_id) REFERENCES sms_coupon (id),
    CONSTRAINT fk_sms_coupon_history_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id)
);
CREATE INDEX idx_sms_coupon_history_coupon_id ON sms_coupon_history (coupon_id);
CREATE INDEX idx_sms_coupon_history_member_id ON sms_coupon_history (member_id);

CREATE TABLE sms_home_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slot_type VARCHAR(30) NOT NULL,
    ref_id BIGINT,
    name VARCHAR(200),
    pic VARCHAR(500),
    url VARCHAR(500),
    sort INT NOT NULL DEFAULT 0,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    start_time DATETIME(6),
    end_time DATETIME(6),
    note VARCHAR(300)
);
CREATE INDEX idx_sms_home_slot_slot_type ON sms_home_slot (slot_type);

CREATE TABLE oms_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    coupon_history_id BIGINT,
    order_sn VARCHAR(64) NOT NULL UNIQUE,
    create_time DATETIME(6) NOT NULL,
    member_username VARCHAR(64),
    total_amount DECIMAL(10,2) NOT NULL,
    pay_amount DECIMAL(10,2) NOT NULL,
    freight_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    gift_packaging_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    promotion_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    integration_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    coupon_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    pay_method VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    fulfillment_type VARCHAR(30) NOT NULL,
    preorder BOOLEAN NOT NULL DEFAULT FALSE,
    preorder_window_id BIGINT,
    preorder_units INT,
    pickup_slot_start DATETIME(6),
    pickup_slot_end DATETIME(6),
    delivery_company VARCHAR(100),
    delivery_sn VARCHAR(100),
    auto_confirm_days INT NOT NULL DEFAULT 7,
    integration_earned INT NOT NULL DEFAULT 0,
    growth_earned INT NOT NULL DEFAULT 0,
    receiver_name VARCHAR(100),
    receiver_phone VARCHAR(20),
    receiver_post_code VARCHAR(20),
    receiver_province VARCHAR(100),
    receiver_city VARCHAR(100),
    receiver_region VARCHAR(100),
    receiver_detail_address VARCHAR(300),
    note VARCHAR(500),
    gift_message VARCHAR(300),
    gift_sleeve BOOLEAN NOT NULL DEFAULT FALSE,
    use_integration INT NOT NULL DEFAULT 0,
    payment_time DATETIME(6),
    delivery_time DATETIME(6),
    receive_time DATETIME(6),
    comment_time DATETIME(6),
    cancel_reason VARCHAR(200),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    update_time DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_oms_order_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id),
    CONSTRAINT fk_oms_order_preorder_window_id FOREIGN KEY (preorder_window_id) REFERENCES pms_preorder_window (id)
);
CREATE INDEX idx_oms_order_member_id ON oms_order (member_id);
CREATE INDEX idx_oms_order_status ON oms_order (status);
CREATE INDEX idx_oms_order_preorder_window_id ON oms_order (preorder_window_id);

CREATE TABLE oms_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_sn VARCHAR(64),
    product_id BIGINT NOT NULL,
    product_pic VARCHAR(500),
    product_name VARCHAR(200),
    product_sn VARCHAR(20),
    product_sku_id BIGINT,
    product_sku_code VARCHAR(50),
    sku_label VARCHAR(100),
    units_per_sku INT NOT NULL DEFAULT 1,
    product_price DECIMAL(10,2) NOT NULL,
    product_quantity INT NOT NULL,
    promotion_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    coupon_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    integration_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    real_amount DECIMAL(10,2) NOT NULL,
    box_selection VARCHAR(2000),
    CONSTRAINT fk_oms_order_item_order_id FOREIGN KEY (order_id) REFERENCES oms_order (id),
    CONSTRAINT fk_oms_order_item_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_oms_order_item_order_id ON oms_order_item (order_id);
CREATE INDEX idx_oms_order_item_product_id ON oms_order_item (product_id);

CREATE TABLE oms_order_operate_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    operator VARCHAR(64),
    create_time DATETIME(6) NOT NULL,
    order_status VARCHAR(30),
    note VARCHAR(500),
    CONSTRAINT fk_oms_order_operate_history_order_id FOREIGN KEY (order_id) REFERENCES oms_order (id)
);
CREATE INDEX idx_oms_order_operate_history_order_id ON oms_order_operate_history (order_id);

CREATE TABLE oms_order_return_reason (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    create_time DATETIME(6) NOT NULL
);

CREATE TABLE oms_order_return_apply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    order_sn VARCHAR(64),
    create_time DATETIME(6) NOT NULL,
    member_id BIGINT NOT NULL,
    member_username VARCHAR(64),
    return_amount DECIMAL(10,2),
    return_name VARCHAR(100),
    return_phone VARCHAR(20),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    handle_time DATETIME(6),
    product_name VARCHAR(200),
    product_quantity INT,
    reason VARCHAR(200),
    description VARCHAR(500),
    proof_pics VARCHAR(1000),
    handle_note VARCHAR(500),
    handle_man VARCHAR(64),
    receive_man VARCHAR(64),
    receive_time DATETIME(6),
    receive_note VARCHAR(500),
    CONSTRAINT fk_oms_order_return_apply_order_id FOREIGN KEY (order_id) REFERENCES oms_order (id),
    CONSTRAINT fk_oms_order_return_apply_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id),
    CONSTRAINT fk_oms_order_return_apply_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id)
);
CREATE INDEX idx_oms_order_return_apply_order_id ON oms_order_return_apply (order_id);
CREATE INDEX idx_oms_order_return_apply_product_id ON oms_order_return_apply (product_id);
CREATE INDEX idx_oms_order_return_apply_member_id ON oms_order_return_apply (member_id);

CREATE TABLE oms_order_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    normal_order_timeout_minutes INT NOT NULL DEFAULT 30,
    auto_confirm_days INT NOT NULL DEFAULT 7,
    comment_timeout_days INT NOT NULL DEFAULT 15,
    delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 4.50,
    free_delivery_threshold DECIMAL(10,2) NOT NULL DEFAULT 40.00,
    gift_packaging_fee DECIMAL(10,2) NOT NULL DEFAULT 2.00,
    points_per_currency_unit INT NOT NULL DEFAULT 100,
    max_redeem_percent INT NOT NULL DEFAULT 20,
    delivery_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    pickup_lead_minutes INT NOT NULL DEFAULT 30,
    preorder_lead_days INT NOT NULL DEFAULT 3,
    points_per_currency_spent INT NOT NULL DEFAULT 1,
    return_window_days INT NOT NULL DEFAULT 7,
    experience_cancel_hours INT NOT NULL DEFAULT 24
);

CREATE TABLE oms_shop_location (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    province VARCHAR(100),
    city VARCHAR(100),
    region VARCHAR(100),
    detail_address VARCHAR(300),
    pickup_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    return_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    opening_hours VARCHAR(200)
);

CREATE TABLE oms_cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_sku_id BIGINT,
    quantity INT NOT NULL,
    price DECIMAL(10,2),
    product_name VARCHAR(200),
    product_sku_label VARCHAR(100),
    product_pic VARCHAR(500),
    box_selection VARCHAR(2000),
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_oms_cart_item_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id),
    CONSTRAINT fk_oms_cart_item_product_id FOREIGN KEY (product_id) REFERENCES pms_product (id)
);
CREATE INDEX idx_oms_cart_item_member_id ON oms_cart_item (member_id);
CREATE INDEX idx_oms_cart_item_product_id ON oms_cart_item (product_id);

CREATE TABLE oms_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    order_sn VARCHAR(64),
    personalization_id BIGINT,
    booking_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(64) UNIQUE,
    create_time DATETIME(6) NOT NULL,
    paid_time DATETIME(6)
);
CREATE INDEX idx_oms_payment_order_id ON oms_payment (order_id);

CREATE TABLE oms_personalization_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_sn VARCHAR(64) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    kind VARCHAR(30) NOT NULL,
    text_content VARCHAR(300),
    quantity INT NOT NULL DEFAULT 1,
    occasion_note VARCHAR(500),
    requested_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    quote_amount DECIMAL(10,2),
    deposit_amount DECIMAL(10,2),
    lead_time_days INT,
    proof_version INT NOT NULL DEFAULT 0,
    admin_note VARCHAR(500),
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_oms_personalization_request_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id)
);
CREATE INDEX idx_oms_personalization_request_member_id ON oms_personalization_request (member_id);
CREATE INDEX idx_oms_personalization_request_status ON oms_personalization_request (status);

CREATE TABLE oms_personalization_proof (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    version INT NOT NULL,
    artwork_note VARCHAR(1000),
    created_by VARCHAR(64),
    create_time DATETIME(6) NOT NULL,
    approved_by_member BOOLEAN NOT NULL DEFAULT FALSE,
    approved_time DATETIME(6),
    CONSTRAINT uq_oms_personalization_proof_request_id_version UNIQUE (request_id, version),
    CONSTRAINT fk_oms_personalization_proof_request_id FOREIGN KEY (request_id) REFERENCES oms_personalization_request (id)
);
CREATE INDEX idx_oms_personalization_proof_request_id ON oms_personalization_proof (request_id);

CREATE TABLE oms_experience_booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_sn VARCHAR(64) NOT NULL UNIQUE,
    session_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    seats INT NOT NULL,
    amount DECIMAL(10,2),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    create_time DATETIME(6) NOT NULL,
    CONSTRAINT fk_oms_experience_booking_session_id FOREIGN KEY (session_id) REFERENCES pms_experience_session (id),
    CONSTRAINT fk_oms_experience_booking_member_id FOREIGN KEY (member_id) REFERENCES ums_member (id)
);
CREATE INDEX idx_oms_experience_booking_session_id ON oms_experience_booking (session_id);
CREATE INDEX idx_oms_experience_booking_member_id ON oms_experience_booking (member_id);
