SET NAMES utf8mb4;
USE sales_dashboard;

-- ============================================================
-- Customers (20 — INSERT IGNORE for idempotency)
-- ============================================================
INSERT IGNORE INTO customers (id, name, email, city) VALUES
    (1,  'Alice Martin',      'alice.martin@mail.fr',      'Paris'),
    (2,  'Bob Dupont',        'bob.dupont@mail.fr',        'Lyon'),
    (3,  'Claire Bernard',    'claire.bernard@mail.fr',    'Marseille'),
    (4,  'David Moreau',      'david.moreau@mail.fr',      'Bordeaux'),
    (5,  'Emma Petit',        'emma.petit@mail.fr',        'Toulouse'),
    (6,  'François Leroy',    'francois.leroy@mail.fr',    'Nantes'),
    (7,  'Gabrielle Simon',  'gabrielle.simon@mail.fr',   'Strasbourg'),
    (8,  'Hugo Laurent',     'hugo.laurent@mail.fr',      'Lille'),
    (9,  'Inès Thomas',      'ines.thomas@mail.fr',       'Rennes'),
    (10, 'Julien Roux',      'julien.roux@mail.fr',       'Nice'),
    (11, 'Karine Blanc',     'karine.blanc@mail.fr',      'Montpellier'),
    (12, 'Louis Garnier',    'louis.garnier@mail.fr',     'Paris'),
    (13, 'Marie Faure',      'marie.faure@mail.fr',       'Lyon'),
    (14, 'Nicolas Rousseau', 'nicolas.rousseau@mail.fr',  'Grenoble'),
    (15, 'Océane Mercier',   'oceane.mercier@mail.fr',    'Bordeaux'),
    (16, 'Paul Girard',      'paul.girard@mail.fr',       'Toulouse'),
    (17, 'Quentin Bonnet',   'quentin.bonnet@mail.fr',    'Nantes'),
    (18, 'Rachel Aubert',    'rachel.aubert@mail.fr',     'Paris'),
    (19, 'Sébastien Morel',  'sebastien.morel@mail.fr',   'Lyon'),
    (20, 'Theo Perrin',       'theo.perrin@mail.fr',       'Marseille');

-- ============================================================
-- Product catalogue (12 — INSERT IGNORE for idempotency)
-- ============================================================
INSERT IGNORE INTO products (id, name, category, price) VALUES
    (1,  'Laptop Pro 15"',      'Electronics', 1299.99),
    (2,  'Smartphone X12',      'Electronics',  799.99),
    (3,  'Wireless Headphones', 'Electronics',  149.99),
    (4,  'Mechanical Keyboard', 'Electronics',   89.99),
    (5,  'Running Shoes',       'Clothing',      119.99),
    (6,  'Winter Jacket',       'Clothing',      189.99),
    (7,  'Yoga Pants',          'Clothing',       59.99),
    (8,  'Cotton T-Shirt',      'Clothing',       24.99),
    (9,  'Clean Code (book)',   'Books',          34.99),
    (10, 'Design Patterns',     'Books',          39.99),
    (11, 'Coffee Maker',        'Home',           79.99),
    (12, 'Desk Lamp LED',       'Home',           44.99);

-- ============================================================
-- Seed 500 orders — only if table is empty (idempotent)
-- ============================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS seed_orders$$

CREATE PROCEDURE seed_orders()
BEGIN
    DECLARE i            INT DEFAULT 0;
    DECLARE v_date       DATE;
    DECLARE v_status     VARCHAR(20);
    DECLARE v_cust_id    INT;
    DECLARE v_order_id   BIGINT;
    DECLARE v_prod1      INT;
    DECLARE v_qty1       INT;
    DECLARE v_prod2      INT;
    DECLARE v_qty2       INT;
    DECLARE v_total      DECIMAL(10,2);
    DECLARE v_price1     DECIMAL(10,2);
    DECLARE v_price2     DECIMAL(10,2);
    DECLARE v_payment    VARCHAR(50);
    DECLARE v_shipping   DECIMAL(10,2);

    IF (SELECT COUNT(*) FROM sales_orders) = 0 THEN
        WHILE i < 500 DO
            SET v_date = DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 365) DAY);

            -- status: PENDING 10%, CONFIRMED 15%, SHIPPED 20%, DELIVERED 45%, CANCELLED 10%
            SET v_status = ELT(1 + FLOOR(RAND() * 20),
                'PENDING','PENDING',
                'CONFIRMED','CONFIRMED','CONFIRMED',
                'SHIPPED','SHIPPED','SHIPPED','SHIPPED',
                'DELIVERED','DELIVERED','DELIVERED','DELIVERED','DELIVERED',
                'DELIVERED','DELIVERED','DELIVERED','DELIVERED',
                'CANCELLED','CANCELLED');

            SET v_cust_id  = 1 + FLOOR(RAND() * 20);
            SET v_payment  = ELT(1 + FLOOR(RAND() * 4), 'Credit Card', 'PayPal', 'Bank Transfer', 'Apple Pay');
            SET v_shipping = ELT(1 + FLOOR(RAND() * 4), 0.00, 4.99, 9.99, 14.99);

            SET v_prod1 = 1 + FLOOR(RAND() * 12);
            SET v_qty1  = 1 + FLOOR(RAND() * 3);
            SELECT price INTO v_price1 FROM products WHERE id = v_prod1;
            SET v_total = v_price1 * v_qty1;

            INSERT INTO sales_orders (order_date, total_amount, customer_id, status, payment_method, shipping_amount)
            VALUES (v_date, v_total + v_shipping, v_cust_id, v_status, v_payment, v_shipping);

            SET v_order_id = LAST_INSERT_ID();
            INSERT INTO order_items (order_id, product_id, quantity, unit_price)
            VALUES (v_order_id, v_prod1, v_qty1, v_price1);

            IF RAND() < 0.6 THEN
                SET v_prod2 = 1 + FLOOR(RAND() * 12);
                SET v_qty2  = 1 + FLOOR(RAND() * 2);
                SELECT price INTO v_price2 FROM products WHERE id = v_prod2;

                INSERT INTO order_items (order_id, product_id, quantity, unit_price)
                VALUES (v_order_id, v_prod2, v_qty2, v_price2);

                UPDATE sales_orders
                SET total_amount = total_amount + (v_price2 * v_qty2)
                WHERE id = v_order_id;
            END IF;

            SET i = i + 1;
        END WHILE;
    END IF;
END$$

DELIMITER ;

CALL seed_orders();
DROP PROCEDURE IF EXISTS seed_orders;
