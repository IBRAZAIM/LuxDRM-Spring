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
    price   INT,
    category_id INT REFERENCES categories(id)
);

CREATE TABLE categories
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);

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
    password          VARCHAR(255) NOT NULL UNIQUE,
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

CREATE TABLE cart (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) NOT NULL,
    product_id INT REFERENCES products(id) NOT NULL,
    quantity INT NOT NULL
);
