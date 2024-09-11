-- Создание таблицы limits
CREATE TABLE limits (
                        id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                        limit_sum NUMERIC(10, 2) NOT NULL DEFAULT 1000.00,
                        limit_currency CHAR(3) NOT NULL DEFAULT 'USD',
                        limit_datetime TIMESTAMP WITH TIME ZONE NOT NULL  -- Без DEFAULT и CHECK
);

-- Функция, которая мешает обновлениям
CREATE OR REPLACE FUNCTION prevent_updates()
    RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Updating records is not allowed.';
END;
$$ LANGUAGE plpgsql;

-- Триггер, который вызывает функцию перед обновлением
CREATE TRIGGER prevent_update_trigger
    BEFORE UPDATE ON limits
    FOR EACH ROW EXECUTE FUNCTION prevent_updates();

-- Функция для предотвращения явной вставки времени
CREATE OR REPLACE FUNCTION enforce_limit_datetime()
    RETURNS TRIGGER AS $$
BEGIN
    -- Принудительно устанавливаем limit_datetime на текущее время
    NEW.limit_datetime = now() AT TIME ZONE 'UTC';  -- Установка времени
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггер, который вызывает функцию перед вставкой
CREATE TRIGGER set_datetime_trigger
    BEFORE INSERT ON limits
    FOR EACH ROW
EXECUTE FUNCTION enforce_limit_datetime();
