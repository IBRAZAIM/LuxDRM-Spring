CREATE TABLE IF NOT EXISTS categories
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS options
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    category_id INT REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS products
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    price       INT NOT NULL DEFAULT 0,
    discount    INT NOT NULL DEFAULT 0,
    rating      DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    description TEXT,
    category_id INT REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS "values"
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255),
    product_id  INT REFERENCES products (id),
    options_id   INT REFERENCES options (id)
);

CREATE TABLE IF NOT EXISTS users
(
    id                BIGSERIAL PRIMARY KEY,
    role              VARCHAR(255),
    login             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    name              VARCHAR(255),
    surname           VARCHAR(255),
    registration_date TIMESTAMP
);

INSERT INTO users (role, login, password, name, surname, registration_date)
SELECT 'admin', 'admin', '$2a$10$Yog6EG.n0uFlJHhYrTcgPOLMEXU3uF4rNbM6l1bUboOnTDoK7.Rxm', 'Иван', 'Админов', NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE login = 'admin'
);

CREATE TABLE IF NOT EXISTS orders
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES users (id) NOT NULL,
    serial_number VARCHAR(255),
    status        VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    email         VARCHAR(255),
    phone         VARCHAR(255),
    full_name     VARCHAR(255),
    country       VARCHAR(255),
    city          VARCHAR(255),
    address       VARCHAR(255) NOT NULL,
    postal_code   VARCHAR(255),
    date          TIMESTAMP,
    total_price   INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reviews
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT REFERENCES users (id) NOT NULL,
    product_id         INT REFERENCES products (id) NOT NULL,
    publication_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    estimation         SMALLINT,
    estimation_text    VARCHAR(255),
    estimation_data    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_products
(
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT REFERENCES orders (id)   NOT NULL,
    product_id        INT REFERENCES products (id) NOT NULL,
    size              VARCHAR(255),
    unit_price        INT NOT NULL,
    discount_percent  INT NOT NULL DEFAULT 0,
    line_total        INT NOT NULL,
    quantity          INT NOT NULL
);

CREATE TABLE IF NOT EXISTS cart
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES users (id)    NOT NULL,
    product_id INT REFERENCES products (id) NOT NULL,
    size       VARCHAR(255),
    quantity   INT NOT NULL
);

CREATE TABLE IF NOT EXISTS product_images
(
    id         BIGSERIAL PRIMARY KEY,
    product_id INT REFERENCES products (id) NOT NULL,
    image      VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS product_sizes
(
    id         BIGSERIAL PRIMARY KEY,
    product_id INT REFERENCES products (id) NOT NULL,
    size       VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS cards
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users (id) NOT NULL,
    card_number TEXT NOT NULL,
    card_holder TEXT NOT NULL,
    card_month  TEXT NOT NULL,
    card_year   TEXT NOT NULL,
    cvv_code    TEXT
);

CREATE TABLE IF NOT EXISTS visits
(
    id         BIGSERIAL PRIMARY KEY,
    visit_date DATE NOT NULL DEFAULT CURRENT_DATE,
    user_id    BIGINT NULL,
    page       VARCHAR(255) NULL,
    session_id VARCHAR(255) NULL,
    CONSTRAINT ux_visits_session_date UNIQUE (session_id, visit_date)
);

CREATE INDEX IF NOT EXISTS idx_visits_date ON visits (visit_date);
CREATE INDEX IF NOT EXISTS idx_visits_page ON visits (page);

CREATE TABLE IF NOT EXISTS visits_archive
(
    id         BIGSERIAL PRIMARY KEY,
    visit_date DATE NOT NULL,
    user_id    BIGINT NULL,
    page       VARCHAR(255) NULL,
    session_id VARCHAR(255) NULL
);

DO $$
BEGIN
    UPDATE users
    SET role = lower(trim(role))
    WHERE role IS NOT NULL;
END $$;

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class tbl ON tbl.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = tbl.relnamespace
        JOIN unnest(con.conkey) AS cols(attnum) ON TRUE
        JOIN pg_attribute att ON att.attrelid = tbl.oid AND att.attnum = cols.attnum
        WHERE ns.nspname = 'public'
          AND tbl.relname = 'users'
          AND con.contype = 'u'
        GROUP BY con.conname
        HAVING COUNT(*) = 1 AND MAX(att.attname) = 'password'
    LOOP
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT IF EXISTS %I', constraint_name);
    END LOOP;
END $$;

DO $$
DECLARE
    index_name text;
BEGIN
    FOR index_name IN
        SELECT indexname
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'users'
          AND indexdef ILIKE 'CREATE UNIQUE INDEX%'
          AND indexdef ~* '\\(\\s*password\\s*\\)'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS %I.%I', 'public', index_name);
    END LOOP;
END $$;

DO $$
DECLARE
    duplicate_sample text;
BEGIN
    SELECT string_agg(login_value, ', ')
    INTO duplicate_sample
    FROM (
        SELECT lower(login) AS login_value
        FROM users
        WHERE login IS NOT NULL
        GROUP BY lower(login)
        HAVING COUNT(*) > 1
        ORDER BY lower(login)
        LIMIT 5
    ) duplicates;

    IF duplicate_sample IS NOT NULL THEN
        RAISE EXCEPTION
            'Found duplicate logins in users table (case-insensitive). Duplicates sample: %',
            duplicate_sample;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_login_ci ON users ((lower(login)));

DO $$
BEGIN
    ALTER TABLE cards
    ALTER COLUMN card_number TYPE TEXT USING card_number::TEXT,
    ALTER COLUMN card_holder TYPE TEXT USING card_holder::TEXT,
    ALTER COLUMN card_month TYPE TEXT USING card_month::TEXT,
    ALTER COLUMN card_year TYPE TEXT USING card_year::TEXT,
    ALTER COLUMN cvv_code DROP NOT NULL,
    ALTER COLUMN cvv_code TYPE TEXT USING cvv_code::TEXT;

    UPDATE cards
    SET cvv_code = NULL
    WHERE cvv_code IS NOT NULL;
END $$;

DO $$
BEGIN
    UPDATE orders
    SET status = CASE
        WHEN status IS NULL OR btrim(status::text) = '' THEN 'PENDING'
        WHEN upper(btrim(status::text)) IN ('PENDING', 'В ОЖИДАНИИ', 'ОФОРМЛЕНО') THEN 'PENDING'
        WHEN upper(btrim(status::text)) IN ('SHIPPED', 'ОТПРАВЛЕНО') THEN 'SHIPPED'
        WHEN upper(btrim(status::text)) IN ('DELIVERED', 'ДОСТАВЛЕНО') THEN 'DELIVERED'
        WHEN upper(btrim(status::text)) IN ('CANCELED', 'CANCELLED', 'ОТМЕНЕНО') THEN 'CANCELED'
        ELSE 'PENDING'
    END;

    ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'PENDING';
    ALTER TABLE orders ALTER COLUMN status SET NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_status') THEN
        ALTER TABLE orders ADD CONSTRAINT chk_orders_status
            CHECK (status IN ('PENDING', 'SHIPPED', 'DELIVERED', 'CANCELED'));
    END IF;
END $$;

DO $$
BEGIN
    UPDATE reviews
    SET publication_status = CASE
        WHEN publication_status IS NULL OR btrim(publication_status::text) = '' THEN 'PENDING'
        WHEN upper(btrim(publication_status::text)) IN ('PENDING', 'НЕ ПРОВЕРЕНО') THEN 'PENDING'
        WHEN upper(btrim(publication_status::text)) IN ('APPROVED', 'ПРОВЕРЕНО') THEN 'APPROVED'
        ELSE 'PENDING'
    END;

    ALTER TABLE reviews ALTER COLUMN publication_status SET DEFAULT 'PENDING';
    ALTER TABLE reviews ALTER COLUMN publication_status SET NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_reviews_status') THEN
        ALTER TABLE reviews ADD CONSTRAINT chk_reviews_status
            CHECK (publication_status IN ('PENDING', 'APPROVED'));
    END IF;
END $$;
