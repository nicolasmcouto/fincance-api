CREATE TABLE accounts (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_name  VARCHAR(255)   NOT NULL,
    type        VARCHAR(20)    NOT NULL,
    balance     DECIMAL(19, 2) NOT NULL,
    created_at  DATETIME       NOT NULL
);

CREATE TABLE transactions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    description VARCHAR(255)   NOT NULL,
    amount      DECIMAL(19, 2) NOT NULL,
    type        VARCHAR(20)    NOT NULL,
    date        DATETIME       NOT NULL,
    account_id  BIGINT         NOT NULL,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
