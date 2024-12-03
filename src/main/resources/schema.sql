CREATE TABLE options(
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255),
                        category_id INT REFERENCES categories(id)
);

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
    description TEXT CHECK (char_length(description) <= 500),
    category_id INT REFERENCES categories(id)
);
ALTER TABLE products ADD COLUMN description TEXT CHECK (char_length(description) <= 500);


CREATE TABLE categories
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE orders
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT REFERENCES users (id) NOT NULL,
    status VARCHAR(255),
    address VARCHAR(255)                 NOT NULL,
    date    TIMESTAMP
);

CREATE TABLE users
(
    id                BIGSERIAL PRIMARY KEY,
    role              VARCHAR(255),
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
    publication_status VARCHAR(20)                  NOT NULL,
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

CREATE TABLE cart (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) NOT NULL,
    product_id INT REFERENCES products(id) NOT NULL,
    quantity INT NOT NULL
);

ALTER TABLE products ADD COLUMN url_image VARCHAR(255);
ALTER TABLE products DROP COLUMN url_image;


CREATE TABLE product_images(
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products(id) NOT NULL,
    image TEXT
)