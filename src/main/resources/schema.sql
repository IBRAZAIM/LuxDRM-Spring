CREATE TABLE options(
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255),
                        category_id INT REFERENCES categories(id));

CREATE TABLE values(
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(255),
                       product_id INT REFERENCES products(id),
                       options_id INT REFERENCES options(id)
);

CREATE TABLE products
(
    id    SERIAL PRIMARY KEY ,
    name  VARCHAR(255),
    price   INT
);
INSERT INTO products(name, price)
VALUES ('Хлеб', 150);
INSERT INTO products(name, price)
VALUES ('Халва', 330);

select * FROM products;

CREATE TABLE categories
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);
ALTER TABLE products ADD COLUMN category_id INT REFERENCES categories(id);

ALTER TABLE products DROP COLUMN category_id;
INSERT INTO categories (name) VALUES ('Фрукты');
INSERT INTO categories (name) VALUES ('Овощи');
INSERT INTO categories (name) VALUES ('Молочные продукты');
INSERT INTO categories (name) VALUES ('Сладости');

UPDATE products SET category_id = 3 WHERE name = 'Молоко';




-- Добавление опций для категорий
INSERT INTO options (name, category_id) VALUES ('Производитель', 1);
INSERT INTO options (name, category_id) VALUES ('Срок годности', 1);
INSERT INTO options (name, category_id) VALUES ('Производитель', 2);
INSERT INTO options (name, category_id) VALUES ('Срок годности', 2);


-- Добавление значений для характеристик
INSERT INTO values(name, product_id, options_id) VALUES ('', 1, 1);
INSERT INTO values (name, product_id, options_id) VALUES ('Караганда', 1, 2);
INSERT INTO values (name, product_id, options_id) VALUES ('Алмата', 3, 2);


CREATE TABLE orders
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT REFERENCES users (id) NOT NULL,
    status           INT,
    delivery_address VARCHAR(255)                 NOT NULL,
    date_of_order    TIMESTAMP
);

CREATE TABLE users
(
    id                BIGSERIAL PRIMARY KEY,
    role              INT,
    login             VARCHAR(255) NOT NULL UNIQUE ,
    password          VARCHAR(255) NOT NULL,
    name              VARCHAR(255),
    surname           VARCHAR(255),
    registration_date TIMESTAMP
);


CREATE TABLE reviews
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT REFERENCES users (id)    NOT NULL,
    product_id         BIGINT REFERENCES products (id) NOT NULL,
    publication_status BOOLEAN                  NOT NULL,
    estimation         SMALLINT                        NOT NULL,
    estimation_text    TEXT,
    estimation_data    TIMESTAMP
);

CREATE TABLE order_products
(
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT REFERENCES orders (id)   NOT NULL,
    product_id BIGINT REFERENCES products (id) NOT NULL,
    quantity   INT                             NOT NULL
);