CREATE TABLE IF NOT EXISTS account (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,

    amount NUMERIC(19, 2) NOT NULL,

    type VARCHAR(20) NOT NULL,

    from_account_id BIGINT,
    to_account_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    description VARCHAR(255),

    CONSTRAINT fk_transaction_from_account
        FOREIGN KEY (from_account_id)
        REFERENCES account(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_transaction_to_account
        FOREIGN KEY (to_account_id)
        REFERENCES account(id)
        ON DELETE SET NULL
);
