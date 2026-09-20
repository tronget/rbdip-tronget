CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50),
    CONSTRAINT uq_customers_contact UNIQUE NULLS NOT DISTINCT (full_name, address, phone)
);

ALTER TABLE orders ADD COLUMN customer_id BIGINT;
ALTER TABLE order_items ADD COLUMN product_id BIGINT;

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone
FROM orders;

UPDATE orders AS legacy_order
SET customer_id = customer.id
FROM customers AS customer
WHERE customer.full_name = legacy_order.customer_full_name
  AND customer.address IS NOT DISTINCT FROM legacy_order.customer_address
  AND customer.phone IS NOT DISTINCT FROM legacy_order.customer_phone;

INSERT INTO products (name, price, description)
SELECT legacy_item.product_name, legacy_item.product_price, NULL
FROM (
    SELECT DISTINCT product_name, product_price
    FROM order_items
) AS legacy_item
WHERE NOT EXISTS (
    SELECT 1
    FROM products AS product
    WHERE product.name = legacy_item.product_name
      AND product.price = legacy_item.product_price
);

UPDATE order_items AS legacy_item
SET product_id = (
    SELECT MIN(product.id)
    FROM products AS product
    WHERE product.name = legacy_item.product_name
      AND product.price = legacy_item.product_price
);

ALTER TABLE orders ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE order_items ALTER COLUMN product_id SET NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers (id);
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products (id);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
