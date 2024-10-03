CREATE TABLE exchange_rates
(
    id            UUID DEFAULT gen_random_uuid() NOT NULL,
    currency_pair VARCHAR(20)                    NOT NULL,
    rate          DECIMAL                        NOT NULL,
    close         DECIMAL              NOT NULL,
    datetime_rate      TIMESTAMP WITH TIME ZONE       NOT NULL,
     PRIMARY KEY (id)
);
CREATE INDEX idx_exchange_rates_currency_pair_datetime_rate ON exchange_rates (currency_pair, datetime_rate DESC);
