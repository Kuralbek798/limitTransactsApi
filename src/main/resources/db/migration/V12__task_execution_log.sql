CREATE TABLE task_execution_log
(
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    task_name           VARCHAR(255) UNIQUE,
    last_execution_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    status              VARCHAR(50)
);
