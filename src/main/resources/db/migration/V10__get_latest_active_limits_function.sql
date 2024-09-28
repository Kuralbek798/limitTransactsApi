CREATE OR REPLACE FUNCTION get_latest_active_limits()
    RETURNS TABLE (
                      id UUID,
                      limit_sum NUMERIC(10, 2),
                      currency CHAR(3),
                      datetime TIMESTAMPTZ,
                      client_id UUID,
                      is_base_limit BOOLEAN,
                      is_active BOOLEAN,
                      account_number INTEGER
                  ) AS $$
BEGIN
    RETURN QUERY

        -- creating temp table using window functions for sorting by date
        WITH ranked_limits AS (
            SELECT li.*, cl.account_number,
                   ROW_NUMBER() OVER (PARTITION BY li.client_id ORDER BY li.datetime DESC) AS rnk
            FROM public.limits li
                     JOIN clients_accounts cl ON li.client_id = cl.client_id
            WHERE li.is_active = true
        )
        SELECT id, limit_sum, currency, datetime, client_id, is_base_limit, is_active,
               account_number
        FROM ranked_limits
        WHERE rnk = 1

        UNION

        SELECT l.id, l.limit_sum, l.currency, l.datetime, l.client_id, l.is_base_limit, l.is_active,
               null AS account_number
        FROM public.limits l
        WHERE l.is_base_limit = true
          AND l.is_active = true;
END;
$$ LANGUAGE plpgsql;
