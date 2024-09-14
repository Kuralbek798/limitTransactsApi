CREATE TABLE exchange_rates
(
    id            UUID DEFAULT gen_random_uuid() NOT NULL,
    currency_pair VARCHAR(20)                    NOT NULL,
    rate          DECIMAL                        NOT NULL,
    close         DOUBLE PRECISION               NOT NULL,
    datetime_rate      TIMESTAMP WITH TIME ZONE       NOT NULL,
     PRIMARY KEY (id)
);