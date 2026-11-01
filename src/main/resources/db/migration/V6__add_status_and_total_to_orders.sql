ALTER TABLE orders
    ADD COLUMN status       VARCHAR(20),
    ADD COLUMN total_amount NUMERIC(12,2);

UPDATE orders o
SET status       = 'PENDING',
    total_amount = COALESCE((SELECT SUM(i.subtotal) FROM order_items i WHERE i.order_id = o.id), 0);

ALTER TABLE orders
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN total_amount SET NOT NULL,
    ADD CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'IN_PRODUCTION', 'COMPLETED', 'CANCELLED')),
    ADD CONSTRAINT ck_orders_total_amount CHECK (total_amount >= 0);

CREATE INDEX idx_orders_status ON orders (status);
