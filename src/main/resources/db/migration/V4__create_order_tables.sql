CREATE TABLE orders (
    id          UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers (id),
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_items (
    id                    UUID          PRIMARY KEY,
    order_id              UUID          NOT NULL REFERENCES orders (id),
    product_id            UUID          REFERENCES products (id),
    item_type             VARCHAR(20)   NOT NULL,
    product_name_snapshot VARCHAR(120)  NOT NULL,
    description           TEXT,
    quantity              INTEGER       NOT NULL,
    unit_price_snapshot   NUMERIC(12,2) NOT NULL,
    subtotal              NUMERIC(12,2) NOT NULL,
    CONSTRAINT ck_order_items_type CHECK (item_type IN ('CATALOG', 'CUSTOM')),
    CONSTRAINT ck_order_items_catalog_has_product CHECK (item_type <> 'CATALOG' OR product_id IS NOT NULL),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
