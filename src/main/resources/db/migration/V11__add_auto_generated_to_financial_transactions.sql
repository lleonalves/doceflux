ALTER TABLE financial_transactions
    ADD COLUMN auto_generated BOOLEAN NOT NULL DEFAULT FALSE;

-- Receita gerada automaticamente ao concluir o pedido: no máximo uma por (loja, pedido).
-- Entradas manuais vinculadas ao pedido (ex.: sinal e restante) continuam permitidas.
CREATE UNIQUE INDEX uq_financial_transactions_auto_order
    ON financial_transactions (store_id, order_id)
    WHERE auto_generated AND order_id IS NOT NULL;
