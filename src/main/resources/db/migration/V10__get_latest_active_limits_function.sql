CREATE OR REPLACE FUNCTION public.get_latest_active_limits(
    account_numbers INTEGER[]
)
    RETURNS TABLE(
                     id UUID,
                     limit_sum NUMERIC,
                     currency CHAR(3),
                     datetime TIMESTAMPTZ,
                     client_id UUID,
                     is_base_limit BOOLEAN,
                     is_active BOOLEAN,
                     account_number INTEGER
                 )
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        WITH ranked_limits AS (
            SELECT
                li.id AS limit_id,
                li.limit_sum AS limit_sum,
                li.currency AS currency,
                li.datetime AS datetime,
                li.client_id AS client_id,
                li.is_base_limit AS is_base_limit,
                li.is_active AS is_active,
                cl.account_number AS account_number,
                ROW_NUMBER() OVER (PARTITION BY li.client_id ORDER BY li.datetime DESC) AS rnk
            FROM public.limits li
                     JOIN clients_accounts cl ON li.client_id = cl.client_id
            WHERE li.is_active = true
              AND (cl.account_number = ANY(account_numbers) OR account_numbers IS NULL)  -- Filtering by incoming account_numbers
        )
        SELECT
            ranked_limits.limit_id AS id,
            ranked_limits.limit_sum AS limit_sum,
            ranked_limits.currency AS currency,
            ranked_limits.datetime AS datetime,
            ranked_limits.client_id AS client_id,
            ranked_limits.is_base_limit AS is_base_limit,
            ranked_limits.is_active AS is_active,
            ranked_limits.account_number AS account_number
        FROM ranked_limits
        WHERE ranked_limits.rnk = 1

        UNION

        SELECT
            l.id AS id,
            l.limit_sum AS limit_sum,
            l.currency AS currency,
            l.datetime AS datetime,
            l.client_id AS client_id,
            l.is_base_limit AS is_base_limit,
            l.is_active AS is_active,
            NULL AS account_number
        FROM public.limits l
        WHERE l.is_base_limit = true
          AND l.is_active = true;
END;
$$;
