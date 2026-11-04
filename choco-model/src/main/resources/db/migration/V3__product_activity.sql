CREATE TABLE pms_product_daily_view (
    product_id BIGINT NOT NULL,
    view_date DATE NOT NULL,
    visitor_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (product_id, view_date, visitor_hash)
);
CREATE INDEX idx_daily_view_retention ON pms_product_daily_view (view_date);
CREATE INDEX idx_cart_product_member ON oms_cart_item (product_id, member_id);
CREATE INDEX idx_order_item_activity ON oms_order_item (product_id, order_id);
