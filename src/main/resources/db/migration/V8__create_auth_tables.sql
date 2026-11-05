CREATE TABLE stores (
    id                  UUID         PRIMARY KEY,
    store_name          VARCHAR(150) NOT NULL,
    main_manager        VARCHAR(120) NOT NULL,
    responsible_manager VARCHAR(120) NOT NULL,
    company_start_date  DATE         NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    store_id      UUID         NOT NULL REFERENCES stores (id),
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_store_id ON users (store_id);
