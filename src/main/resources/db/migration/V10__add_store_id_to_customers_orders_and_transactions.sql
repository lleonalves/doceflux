ALTER TABLE customers
    ADD COLUMN store_id UUID NOT NULL;

ALTER TABLE orders
    ADD COLUMN store_id UUID NOT NULL;

ALTER TABLE financial_transactions
    ADD COLUMN store_id UUID NOT NULL;
