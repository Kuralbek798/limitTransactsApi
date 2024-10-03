CREATE TABLE IF NOT EXISTS public.checked_on_limit
(
    id                        UUID    DEFAULT gen_random_uuid() NOT NULL,
    transaction_id            UUID                     NOT NULL,
    limit_id                  UUID                     NOT NULL,
    limit_exceeded            boolean   DEFAULT FALSE  NOT NULL,
    datetime timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);
CREATE INDEX idx_checked_on_limit_limit_id ON checked_on_limit (limit_id);
CREATE INDEX idx_checked_on_limit_transaction_id ON checked_on_limit (transaction_id);
