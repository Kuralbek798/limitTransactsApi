CREATE TABLE limits
(
    id            UUID                              DEFAULT gen_random_uuid() PRIMARY KEY,
    limit_sum     NUMERIC(10, 2)           NOT NULL DEFAULT 1000.00,
    currency      CHAR(3)                  NOT NULL DEFAULT 'USD',
    datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    client_id     UUID,
    is_base_limit BOOLEAN                  NOT NULL DEFAULT FALSE
);

-- Function that prevents updates
CREATE OR REPLACE FUNCTION prevent_updates()
    RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION 'Updating records is not allowed.';
END;
$$ LANGUAGE plpgsql;

-- Trigger that calls the function before update
CREATE TRIGGER prevent_update_trigger
    BEFORE UPDATE
    ON limits
    FOR EACH ROW
EXECUTE FUNCTION prevent_updates();

-- Function to enforce the current timestamp on insertion
CREATE OR REPLACE FUNCTION enforce_limit_datetime()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.datetime = now() AT TIME ZONE 'UTC';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger that calls the function before insert
CREATE TRIGGER set_datetime_trigger
    BEFORE INSERT
    ON limits
    FOR EACH ROW
EXECUTE FUNCTION enforce_limit_datetime();


-- -- Вставка базового лимита
-- INSERT INTO limits (limit_sum, currency, datetime, client_id, is_base_limit)
-- VALUES (1000.00, 'USD', now() AT TIME ZONE 'UTC', NULL, TRUE);
--
-- -- Вставка лимитов клиентов
-- INSERT INTO limits (limit_sum, currency, datetime, client_id, is_base_limit)
-- VALUES (1200.00, 'USD', now() AT TIME ZONE 'UTC', (SELECT id FROM clients WHERE name = 'John Doe'), FALSE),
--       (1500.00, 'USD', now() AT TIME ZONE 'UTC', (SELECT id FROM clients WHERE name = 'Jane Smith'), FALSE),
--        (800.00, 'USD', now() AT TIME ZONE 'UTC', (SELECT id FROM clients WHERE name = 'Michael Johnson'), FALSE);