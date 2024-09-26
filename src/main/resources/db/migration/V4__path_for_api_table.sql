CREATE TABLE path_for_api
(
    id UUID DEFAULT gen_random_uuid() NOT NULL,
    encrypted_api_path VARCHAR(255) NOT NULL,
    datetime TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    description VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (id)
);

INSERT INTO path_for_api (encrypted_api_path,  description, active)
VALUES
    (
        'F5z8tDG0AI0Xon4zLcuLZsXXPAbiMkYDQJHBzYUOLXGHRFDqq4AWvWxil+mRt0+w',
        'twelve',
        true
    );
