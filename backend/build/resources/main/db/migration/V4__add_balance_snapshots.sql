ALTER TABLE transactions
    ADD COLUMN source_balance_before NUMERIC(19,2),
    ADD COLUMN source_balance_after  NUMERIC(19,2),
    ADD COLUMN target_balance_before NUMERIC(19,2),
    ADD COLUMN target_balance_after  NUMERIC(19,2);
