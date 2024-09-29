CREATE OR REPLACE FUNCTION get_transactions_with_rates(p_limit_id UUID)
    RETURNS TABLE (
                      id UUID,
                      sum NUMERIC, -- Сумма
                      currency VARCHAR(3),
                      datetime_transaction TIMESTAMPTZ, -- Дата и время транзакции
                      account_from INTEGER,
                      account_to INTEGER,
                      expense_category VARCHAR(50),
                      tr_date TIMESTAMPTZ, -- Округленная дата транзакции
                      exchange_rate NUMERIC -- Курс обмена

                  ) AS $$
BEGIN
    RETURN QUERY
        WITH transaction_data AS (
            -- Этот CTE выбирает все данные транзакций, которые соответствуют определённому критерию
            SELECT
                tr.id,
                tr.sum,
                tr.currency,
                tr.datetime_transaction,
                tr.account_from,
                tr.account_to,
                tr.expense_category,
                date_trunc('day', tr.datetime_transaction) AS tr_date -- Округляем дату до начала дня
            FROM
                transactions tr
            WHERE
                tr.id IN (
                    -- Подзапрос для выбора id транзакций, которые соответствуют определённому лимиту и категории расходов
                    SELECT tr.id
                    FROM transactions tr
                             JOIN checked_on_limit ch ON tr.id = ch.transaction_id
                    WHERE ch.limit_id = p_limit_id
                      AND tr.expense_category IN ('product', 'service')
                      AND ch.limit_exceeded = false
                )
        ),
             exchange_data AS (
                 -- Этот CTE получает данные обменного курса по валютным парам
                 SELECT
                     e.datetime_rate,
                     e.currency_pair,
                     e.rate,
                     LEAD(e.datetime_rate) OVER (PARTITION BY currency_pair ORDER BY datetime_rate DESC) AS next_date -- Получаем следующую дату по сигналу "currency_pair"
                 FROM
                     exchange_rates e
             )
        SELECT
            td.id,
            td.sum,
            td.currency,
            td.datetime_transaction,
            td.account_from,
            td.account_to,
            td.expense_category,
            td.tr_date,
            ed.rate AS exchange_rate

        FROM
            transaction_data td
                LEFT JOIN LATERAL (
                -- Используем LATERAL JOIN для получения соответствующего курса обмена на основании условий
                SELECT e.rate -- Выбираем курс
                FROM exchange_data e
                WHERE e.datetime_rate <= td.tr_date -- Условие: дата обменного курса должна быть меньше или равна дате транзакции
                  AND e.currency_pair = td.currency || '/USD' -- Формируем валютную пару
                ORDER BY e.datetime_rate DESC -- Сортируем по дате в обратном порядке
                LIMIT 1 -- Берем только первую запись, которая соответствует условию
                ) ed ON true -- Условие этой части JOIN всегда истинно
        ORDER BY td.datetime_transaction; -- Сортируем результаты по времени транзакции
END;
$$ LANGUAGE plpgsql;
