CREATE TABLE path_for_api
(
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY NOT NULL,
    encrypted_api_path VARCHAR(255) NOT NULL,
    date TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    description VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);
