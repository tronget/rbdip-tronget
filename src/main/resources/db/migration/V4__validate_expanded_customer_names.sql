UPDATE customers
SET first_name = COALESCE(NULLIF(btrim(first_name), ''), 'Unknown'),
    last_name = COALESCE(btrim(last_name), '');

ALTER TABLE customers
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;

ALTER TABLE customers
    ADD CONSTRAINT chk_customers_first_name_not_blank CHECK (btrim(first_name) <> '');

ALTER TABLE customers
    ADD CONSTRAINT uq_customers_split_contact
    UNIQUE NULLS NOT DISTINCT (first_name, last_name, address, phone);
