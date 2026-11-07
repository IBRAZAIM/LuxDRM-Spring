ALTER TABLE users
  ADD COLUMN IF NOT EXISTS email VARCHAR(255);

UPDATE users
  SET email = 'admin@luxdrm.kz'
  WHERE login = 'admin' AND email IS NULL;

UPDATE users
  SET email = login
  WHERE email IS NULL AND login LIKE '%@%';

UPDATE users
  SET email = login || '@luxdrm.kz'
  WHERE email IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users (lower(email));