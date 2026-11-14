ALTER TABLE products
    ADD COLUMN store_id       UUID    NOT NULL,
    ADD COLUMN stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    ADD COLUMN image_url      VARCHAR(2048);
