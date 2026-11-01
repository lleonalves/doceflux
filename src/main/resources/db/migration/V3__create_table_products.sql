CREATE TABLE products (
    id          UUID          PRIMARY KEY,
    name        VARCHAR(120)  NOT NULL,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_products_active ON products (active);
