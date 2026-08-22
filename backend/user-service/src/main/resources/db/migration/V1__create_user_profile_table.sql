CREATE TABLE IF NOT EXISTS user_profiles (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(150),
    bio         TEXT,
    organization VARCHAR(255),
    role        VARCHAR(50) NOT NULL DEFAULT 'RESEARCHER',
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

