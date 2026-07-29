ALTER TABLE transactions DROP FOREIGN KEY fk_transactions_account;
DROP INDEX idx_transactions_account_id ON transactions;

ALTER TABLE transactions RENAME COLUMN account_id TO receiver_id;
ALTER TABLE transactions ADD COLUMN sender_id BIGINT NOT NULL AFTER receiver_id;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_receiver FOREIGN KEY (receiver_id) REFERENCES accounts (id),
    ADD CONSTRAINT fk_transactions_sender FOREIGN KEY (sender_id) REFERENCES accounts (id);

CREATE INDEX idx_transactions_receiver_id ON transactions (receiver_id);
CREATE INDEX idx_transactions_sender_id ON transactions (sender_id);
