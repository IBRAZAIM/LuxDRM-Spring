CREATE DATABASE uxd;

CREATE TABLE categories
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE options
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255),
    category_id INT REFERENCES categories (id)
);

CREATE TABLE products
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255),
    price       INT,
    description TEXT CHECK (char_length(description) <= 5000),
    category_id INT REFERENCES categories (id)
);

ALTER TABLE products
    ADD COLUMN description TEXT CHECK (char_length(description) <= 5000);
ALTER TABLE products
    ADD COLUMN url_image VARCHAR(255);
ALTER TABLE products
    DROP COLUMN url_image;


CREATE TABLE values
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(255),
    product_id INT REFERENCES products (id),
    options_id INT REFERENCES options (id)
);

CREATE TABLE users
(
    id                BIGSERIAL PRIMARY KEY,
    role              VARCHAR(255),
    login             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    name              VARCHAR(255),
    surname           VARCHAR(255),
    registration_date TIMESTAMP
);

-- Администратор
INSERT INTO users (role, login, password, name, surname, registration_date)
VALUES ('ADMIN', 'admin', '$2a$10$Dow1ZkY7VhXhQhQkzHqY2u1XcYQkFvQkQkFvQkFvQkFvQkFvQkFvQ', 'Иван', 'Админов', NOW());

UPDATE users
SET password = '$2a$10$Yog6EG.n0uFlJHhYrTcgPOLMEXU3uF4rNbM6l1bUboOnTDoK7.Rxm'
WHERE login = 'admin';

CREATE TABLE orders
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES users (id) NOT NULL,
    serial_number VARCHAR(50),
    status        VARCHAR(20),
    email         VARCHAR(255),
    phone         VARCHAR(20),
    full_name     VARCHAR(100),
    country       VARCHAR(30),
    city          VARCHAR(34),
    address       VARCHAR(255)                 NOT NULL,
    postal_code   VARCHAR(20),
    date          TIMESTAMP
);

CREATE TABLE reviews
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT REFERENCES users (id)    NOT NULL,
    product_id         BIGINT REFERENCES products (id) NOT NULL,
    publication_status VARCHAR(20)                     NOT NULL,
    estimation         SMALLINT,
    estimation_text    TEXT CHECK (char_length(estimation_text) <= 1000),
    estimation_data    TIMESTAMP
);
CREATE TABLE order_products
(
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT REFERENCES orders (id)   NOT NULL,
    product_id BIGINT REFERENCES products (id) NOT NULL,
    quantity   INT                             NOT NULL
);

CREATE TABLE cart
(
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users (id)    NOT NULL,
    product_id INT REFERENCES products (id) NOT NULL,
    quantity   INT                          NOT NULL
);

CREATE TABLE product_images
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products (id) NOT NULL,
    image      TEXT
);

CREATE TABLE product_sizes
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products (id) NOT NULL,
    size       VARCHAR(50)                     NOT NULL
);

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE cards
(
    id          BIGSERIAL PRIMARY KEY     NOT NULL,
    user_id     INT REFERENCES users (id) NOT NULL,
    card_number VARCHAR(19)               NOT NULL,
    card_holder VARCHAR(40)               NOT NULL,
    card_month  VARCHAR(2)                NOT NULL,
    card_year   VARCHAR(2)                NOT NULL,
    cvv_code    VARCHAR(3)                NOT NULL
);

-- Синхронизация счетчика для характеристик
SELECT setval(pg_get_serial_sequence('values', 'id'), (SELECT MAX(id) FROM "values"));

-- Синхронизация счетчика для товаров
SELECT setval(pg_get_serial_sequence('products', 'id'), (SELECT MAX(id) FROM products));

SELECT MAX(id)
FROM product_images;

SELECT setval('product_images_id_seq', (SELECT MAX(id) FROM product_images));


-- Основная таблица для текущих посещений
CREATE TABLE visits
(
    id         BIGSERIAL PRIMARY KEY,
    visit_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id    BIGINT       NULL, -- если есть авторизация
    page       VARCHAR(255) NULL, -- какая страница посещалась
    session_id VARCHAR(255) NULL  -- уникальный идентификатор сессии
);

-- Индексы для быстрого подсчета посещений по дате и странице
CREATE INDEX idx_visits_date ON visits (visit_date);
CREATE INDEX idx_visits_page ON visits (page);

-- Таблица архива для старых данных
CREATE TABLE visits_archive
(
    id         BIGSERIAL PRIMARY KEY,
    visit_date TIMESTAMP    NOT NULL,
    user_id    BIGINT       NULL,
    page       VARCHAR(255) NULL,
    session_id VARCHAR(255) NULL
);
