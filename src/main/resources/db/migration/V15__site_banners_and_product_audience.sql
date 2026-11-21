CREATE TABLE IF NOT EXISTS site_banners (
    slot_key    VARCHAR(64) PRIMARY KEY,
    title       VARCHAR(160) NOT NULL,
    subtitle    VARCHAR(255),
    target_url  VARCHAR(255) NOT NULL,
    image_url   VARCHAR(512),
    sort_order  INT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO site_banners (slot_key, title, subtitle, target_url, image_url, sort_order)
VALUES
    ('hero', 'Домашний комфорт', 'Новая коллекция · UXD', '/products', NULL, 1),
    ('section_men', 'Мужское', 'Коллекция для него', '/products?section=men', NULL, 10),
    ('section_women', 'Женское', 'Коллекция для неё', '/products?section=women', NULL, 11),
    ('section_kids', 'Детское', 'Комфорт для детей', '/products?section=kids', NULL, 12),
    ('cat_pyjamas', 'Пижамы', 'Коллекция', '/products?category=Пижамы', NULL, 20),
    ('cat_home', 'Домашняя одежда', 'Коллекция', '/products?category=Домашняя одежда', NULL, 21),
    ('cat_kids', 'Детская', 'Коллекция', '/products?category=Детская', NULL, 22),
    ('cat_lounge', 'Lounge', 'Коллекция', '/products?category=Lounge', NULL, 23)
ON CONFLICT (slot_key) DO NOTHING;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS audience VARCHAR(20) NOT NULL DEFAULT 'UNISEX';

UPDATE products
SET audience = 'MEN'
WHERE audience = 'UNISEX'
  AND (LOWER(name) LIKE '%муж%' OR LOWER(name) LIKE '%men''s%' OR LOWER(name) LIKE '% men %');

UPDATE products
SET audience = 'WOMEN'
WHERE audience = 'UNISEX'
  AND (LOWER(name) LIKE '%жен%' OR LOWER(name) LIKE '%women%');

UPDATE products
SET audience = 'KIDS'
WHERE audience = 'UNISEX'
  AND (LOWER(name) LIKE '%детск%' OR LOWER(name) LIKE '%kid%' OR LOWER(name) LIKE '%малыш%');

CREATE INDEX IF NOT EXISTS idx_products_audience ON products (audience);
