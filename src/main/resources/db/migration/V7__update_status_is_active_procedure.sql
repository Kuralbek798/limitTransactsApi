CREATE OR REPLACE PROCEDURE public.update_status_is_active()
    LANGUAGE plpgsql AS $$
BEGIN
    -- Update is_active to false for records where datetime is in the previous month
    UPDATE public.limits
    SET is_active = false
    WHERE datetime < CURRENT_DATE
      AND datetime >= (CURRENT_DATE - INTERVAL '1 month');
END;
$$;
