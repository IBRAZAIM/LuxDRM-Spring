ALTER TABLE products ADD COLUMN IF NOT EXISTS sku VARCHAR(64);
UPDATE products SET sku = 'UXD-' || id::text WHERE sku IS NULL OR TRIM(sku) = '';
