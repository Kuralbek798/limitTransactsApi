CREATE TABLE exchange_rates
(
    id                 UUID                              NOT NULL,
    currency_pair VARCHAR(20)                            NOT NULL,
    rate          DECIMAL                                NOT NULL,
    close         DOUBLE PRECISION                       NOT NULL,
    date          TIMESTAMP WITH TIME ZONE               NOT NULL,
    CONSTRAINT pk_exchange_rates PRIMARY KEY (id)
);