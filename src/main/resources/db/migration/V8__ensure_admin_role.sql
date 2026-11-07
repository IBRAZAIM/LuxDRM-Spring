-- Ensure the default admin has ADMIN role
UPDATE users
  SET role = 'ADMIN'
  WHERE lower(login) = 'admin' OR lower(email) = 'admin@luxdrm.kz';

