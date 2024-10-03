-- Create the clients_accounts table (if it does not already exist)
CREATE TABLE IF NOT EXISTS clients_accounts
(
    id             UUID                              DEFAULT gen_random_uuid() PRIMARY KEY,
    client_id      UUID                     NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    account_number INTEGER UNIQUE           NOT NULL,
    bank_name      VARCHAR(100)             NOT NULL,
    bank_bik       VARCHAR(9)               NOT NULL,
    inn            VARCHAR(12)              NOT NULL,
    kpp            VARCHAR(9),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_clients_accounts_client_id ON clients_accounts (client_id);
CREATE INDEX idx_clients_accounts_account_number ON clients_accounts (account_number);


-- Insert account details for clients into the clients_accounts table
INSERT INTO clients_accounts (client_id, account_number, bank_name, bank_bik, inn, kpp)
VALUES ((SELECT id FROM clients WHERE name = 'John Doe'), 1002, 'Example Bank', '123456789', '123456789012', '123456789'),
       ((SELECT id FROM clients WHERE name = 'Jane Smith'), 1003, 'Sample Bank', '987654321', '987654321012', '987654321'),
       ((SELECT id FROM clients WHERE name = 'Michael Johnson'), 1005, 'Demo Bank', '111222333', '543216789012', '321654987'),
       ((SELECT id FROM clients WHERE name = 'John Doe'), 1008, 'Example Bank', '123456789', '123456789012', '123456789'),
       ((SELECT id FROM clients WHERE name = 'Jane Smith'), 1009, 'Sample Bank', '987654321', '987654321012', '987654321');
