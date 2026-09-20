DROP TRIGGER customers_sync_name_formats ON customers;
DROP FUNCTION sync_customer_name_formats();

ALTER TABLE customers
    DROP CONSTRAINT uq_customers_contact,
    DROP COLUMN full_name;

ALTER TABLE orders
    DROP COLUMN customer_full_name,
    DROP COLUMN customer_address,
    DROP COLUMN customer_phone;

ALTER TABLE order_items
    DROP COLUMN product_name,
    DROP COLUMN product_price;
