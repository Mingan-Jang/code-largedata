CREATE SCHEMA IF NOT EXISTS largedata;

CREATE TABLE IF NOT EXISTS largedata.detail (
    id BIGINT PRIMARY KEY,
    product_group VARCHAR(30) NOT NULL,
    component_code VARCHAR(30) NOT NULL,
    qty INTEGER NOT NULL CHECK (qty >= 0),
    amount NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    report_date DATE NOT NULL,
    region_code VARCHAR(10),
    country_code VARCHAR(2),
    facility_code VARCHAR(20),
    customer_code VARCHAR(20),
    order_no VARCHAR(30),
    lot_no VARCHAR(30),
    serial_no VARCHAR(30),
    status VARCHAR(20),
    priority SMALLINT,
    sales_channel VARCHAR(20),
    currency_code VARCHAR(3),
    unit_price NUMERIC(14, 2),
    discount_amount NUMERIC(14, 2),
    tax_amount NUMERIC(14, 2),
    net_amount NUMERIC(14, 2),
    shipped_qty INTEGER,
    pending_qty INTEGER,
    is_active BOOLEAN,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS detail_product_group_component_idx
    ON largedata.detail (product_group, component_code);

CREATE INDEX IF NOT EXISTS detail_report_date_product_group_created_idx
    ON largedata.detail (report_date DESC, product_group, created_at DESC);
