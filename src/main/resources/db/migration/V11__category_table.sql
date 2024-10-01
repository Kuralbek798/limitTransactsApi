-- Скрипт для создания таблицы category
CREATE TABLE IF NOT EXISTS category
(
    id         UUID                  DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Инсерты категорий
INSERT INTO category (name, is_active, created_at)
VALUES ('service', TRUE, NOW()),
       ('product', TRUE, NOW())
ON CONFLICT (name) DO NOTHING;
