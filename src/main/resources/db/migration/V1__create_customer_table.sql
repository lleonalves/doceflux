CREATE TABLE customers (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    email      VARCHAR(160),
    address    VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
