CREATE TABLE IF NOT EXISTS customers (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    user_created BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS products (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)   NOT NULL,
    category     VARCHAR(50)    NOT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    user_created BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS sales_orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_date      DATE           NOT NULL,
    total_amount    DECIMAL(10, 2) NOT NULL,
    customer_id     BIGINT         NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    payment_method  VARCHAR(50)    NOT NULL DEFAULT 'Credit Card',
    shipping_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    user_created    BOOLEAN        NOT NULL DEFAULT FALSE,
    FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES sales_orders (id),
    FOREIGN KEY (product_id) REFERENCES products (id)
);
