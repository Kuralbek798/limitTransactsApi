-- Create the clients table (if it does not already exist)
CREATE TABLE IF NOT EXISTS clients (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15),
    address VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Insert sample clients into the clients table
INSERT INTO clients (name, email, phone, address) VALUES
('John Doe', 'john.doe@example.com', '+123456789', '123 Main St, Anytown, AN'),
('Jane Smith', 'jane.smith@example.com', '+987654321', '456 Elm St, Othertown, OT'),
('Michael Johnson', 'michael.johnson@example.com', '+192837465', '789 Oak St, Anycity, AC');
