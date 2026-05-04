-- H2 in-memory schema for write operations.
-- Mirrors the MySQL schema but without FK constraints (REFERENTIAL_INTEGRITY=FALSE in URL).
-- IDs start at high values to avoid collisions with MySQL-seeded rows.

CREATE TABLE IF NOT EXISTS customers (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    city  VARCHAR(100) NOT NULL
);
ALTER TABLE customers ALTER COLUMN id RESTART WITH 10001;

-- customer_id stores the logical customer ID (may reference a MySQL customer)
CREATE TABLE IF NOT EXISTS sales_orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_date      DATE           NOT NULL,
    total_amount    DECIMAL(10,2)  NOT NULL,
    customer_id     BIGINT         NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    payment_method  VARCHAR(50)    NOT NULL DEFAULT 'Credit Card',
    shipping_amount DECIMAL(10,2)  NOT NULL DEFAULT 0.00
);
ALTER TABLE sales_orders ALTER COLUMN id RESTART WITH 100001;

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT         NOT NULL,
    product_id  BIGINT         NOT NULL,
    quantity    INT            NOT NULL,
    unit_price  DECIMAL(10,2)  NOT NULL
);
ALTER TABLE order_items ALTER COLUMN id RESTART WITH 1000001;
