-- Создание таблицы limits
CREATE TABLE limits
(
    id            UUID                              DEFAULT gen_random_uuid() PRIMARY KEY,
    limit_sum     NUMERIC(10, 2)           NOT NULL DEFAULT 1000.00,
    currency      CHAR(3)                  NOT NULL DEFAULT 'USD',
    datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    client_id     UUID,
    is_base_limit BOOLEAN                  NOT NULL DEFAULT FALSE,
    is_active     BOOLEAN                  NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_limits_is_active ON limits (is_active);

-- Функция, разрешающая обновление только поля isActive
CREATE OR REPLACE FUNCTION allow_is_active_update_only()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Проверим, чтобы изменялось только поле isActive
    IF OLD.is_active IS DISTINCT FROM NEW.is_active AND
       OLD.id = NEW.id AND
       OLD.limit_sum = NEW.limit_sum AND
       OLD.currency = NEW.currency AND
       OLD.datetime = NEW.datetime AND
       OLD.client_id IS NOT DISTINCT FROM NEW.client_id AND
       OLD.is_base_limit = NEW.is_base_limit THEN
        RETURN NEW; -- Разрешаем обновление только isActive
    ELSE
        RAISE EXCEPTION 'Updating records is not allowed except for isActive field.';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Удаление существующего триггера, если он есть
DROP TRIGGER IF EXISTS prevent_update_trigger ON limits;

-- Создание триггера для использования с функцией
CREATE TRIGGER prevent_update_trigger
    BEFORE UPDATE
    ON limits
    FOR EACH ROW
EXECUTE FUNCTION allow_is_active_update_only();


-- Функция для установки временной отметки при вставке
CREATE OR REPLACE FUNCTION enforce_limit_datetime()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.datetime = now() AT TIME ZONE 'UTC';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Создание триггера для функции установки временной отметки
CREATE TRIGGER set_datetime_trigger
    BEFORE INSERT
    ON limits
    FOR EACH ROW
EXECUTE FUNCTION enforce_limit_datetime();

-- Вставка базового лимита
INSERT INTO limits (limit_sum, currency, datetime, client_id, is_base_limit)
VALUES (1000.00, 'USD', now() AT TIME ZONE 'UTC', NULL, TRUE);

-- Вставка лимитов клиентов
INSERT INTO limits (limit_sum, currency, datetime, client_id, is_base_limit)
VALUES (1200.00, 'USD', now() AT TIME ZONE 'UTC', (SELECT id FROM clients WHERE name = 'John Doe'), FALSE),
       (1500.00, 'USD', now() AT TIME ZONE 'UTC', (SELECT id FROM clients WHERE name = 'Jane Smith'), FALSE),
       (800.00, 'USD', now() AT TIME ZONE 'UTC', (SELECT id FROM clients WHERE name = 'Michael Johnson'), FALSE);
