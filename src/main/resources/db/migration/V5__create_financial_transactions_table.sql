CREATE TABLE financial_transactions (
    id               UUID          PRIMARY KEY,
    type             VARCHAR(10)   NOT NULL,
    description      VARCHAR(255)  NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    transaction_date DATE          NOT NULL,
    category         VARCHAR(60),
    order_id         UUID          REFERENCES orders (id),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_financial_transactions_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_financial_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_financial_transactions_order_only_income CHECK (type = 'INCOME' OR order_id IS NULL)
);

CREATE INDEX idx_financial_transactions_date ON financial_transactions (transaction_date);
