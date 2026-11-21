DELETE FROM cart
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.deleted = TRUE
      AND NOT EXISTS (SELECT 1 FROM order_products op WHERE op.product_id = p.id)
);

DELETE FROM wishlist
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.deleted = TRUE
      AND NOT EXISTS (SELECT 1 FROM order_products op WHERE op.product_id = p.id)
);

DELETE FROM reviews
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.deleted = TRUE
      AND NOT EXISTS (SELECT 1 FROM order_products op WHERE op.product_id = p.id)
);

DELETE FROM "values"
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.deleted = TRUE
      AND NOT EXISTS (SELECT 1 FROM order_products op WHERE op.product_id = p.id)
);

DELETE FROM product_images
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.deleted = TRUE
      AND NOT EXISTS (SELECT 1 FROM order_products op WHERE op.product_id = p.id)
);

DELETE FROM product_sizes
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.deleted = TRUE
      AND NOT EXISTS (SELECT 1 FROM order_products op WHERE op.product_id = p.id)
);

DELETE FROM products p
WHERE p.deleted = TRUE
  AND NOT EXISTS (SELECT 1 FROM order_products op WHERE op.product_id = p.id);
