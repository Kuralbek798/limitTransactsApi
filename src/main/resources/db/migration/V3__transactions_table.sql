CREATE TABLE transactions
(
    id                 UUID  DEFAULT gen_random_uuid()       NOT NULL,
    sum              DECIMAL                                 NOT NULL,
    currency         VARCHAR(3)                              NOT NULL,
    datetime         TIMESTAMP WITH TIME ZONE                NOT NULL,
    account_from     INTEGER                                 NOT NULL,
    account_to       INTEGER                                 NOT NULL,
    expense_category VARCHAR(50),

    PRIMARY KEY (id)
);