ALTER TABLE customers
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255);

ALTER TABLE orders
    ALTER COLUMN customer_full_name DROP NOT NULL;

ALTER TABLE order_items
    ALTER COLUMN product_name DROP NOT NULL,
    ALTER COLUMN product_price DROP NOT NULL;

UPDATE customers
SET first_name = COALESCE(NULLIF(split_part(btrim(full_name), ' ', 1), ''), 'Unknown'),
    last_name = CASE
        WHEN strpos(btrim(full_name), ' ') > 0
            THEN COALESCE(NULLIF(btrim(substr(btrim(full_name), strpos(btrim(full_name), ' ') + 1)), ''), '')
        ELSE ''
    END;

CREATE OR REPLACE FUNCTION sync_customer_name_formats()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.first_name IS NULL THEN
            NEW.first_name := COALESCE(NULLIF(split_part(btrim(NEW.full_name), ' ', 1), ''), 'Unknown');
            NEW.last_name := CASE
                WHEN strpos(btrim(NEW.full_name), ' ') > 0
                    THEN COALESCE(
                            NULLIF(btrim(substr(btrim(NEW.full_name), strpos(btrim(NEW.full_name), ' ') + 1)), ''),
                            '')
                ELSE ''
            END;
        ELSE
            NEW.last_name := COALESCE(NEW.last_name, '');
            NEW.full_name := btrim(concat_ws(' ', NEW.first_name, NEW.last_name));
        END IF;
    ELSIF NEW.full_name IS DISTINCT FROM OLD.full_name
            AND NEW.first_name IS NOT DISTINCT FROM OLD.first_name
            AND NEW.last_name IS NOT DISTINCT FROM OLD.last_name THEN
        NEW.first_name := COALESCE(NULLIF(split_part(btrim(NEW.full_name), ' ', 1), ''), 'Unknown');
        NEW.last_name := CASE
            WHEN strpos(btrim(NEW.full_name), ' ') > 0
                THEN COALESCE(
                        NULLIF(btrim(substr(btrim(NEW.full_name), strpos(btrim(NEW.full_name), ' ') + 1)), ''),
                        '')
            ELSE ''
        END;
    ELSE
        NEW.first_name := COALESCE(NEW.first_name, 'Unknown');
        NEW.last_name := COALESCE(NEW.last_name, '');
        NEW.full_name := btrim(concat_ws(' ', NEW.first_name, NEW.last_name));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER customers_sync_name_formats
BEFORE INSERT OR UPDATE OF full_name, first_name, last_name ON customers
FOR EACH ROW EXECUTE FUNCTION sync_customer_name_formats();
