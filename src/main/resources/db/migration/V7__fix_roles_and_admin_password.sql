-- Roles were stored lowercase; entity enum expects ADMIN/USER/MODER
UPDATE users
  SET role = upper(trim(role))
  WHERE role IS NOT NULL;

-- Known working admin password: admin123
UPDATE users
  SET password = '$2a$10$5KHmY2cx5Yu2zQ7K18.KSeDSQnQSQCVxnJ4EJhKq9wGDVIfKztuD2',
      email = COALESCE(NULLIF(email, ''), 'admin@luxdrm.kz')
  WHERE lower(login) = 'admin' OR lower(email) = 'admin@luxdrm.kz';