INSERT INTO document_type (type)
VALUES
    ('RECEIPT'),
    ('CONTRACT'),
    ('BANK_STATEMENT'),
    ('CERTIFICATE'),
    ('NOTE'),
    ('PASSPORT'),
    ('OTHER')
    ON CONFLICT (type) DO NOTHING;