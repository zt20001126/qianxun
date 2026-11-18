INSERT INTO admin (username, password_hash, enabled)
SELECT initial_admin.username, initial_admin.password_hash, 1
FROM (SELECT 'admin' AS username,
             '$2a$10$aUblLef5eaK6EIcFr1jBUO.89uomyyYxoMzqRsNSAJcDBmY3BNbt2' AS password_hash) AS initial_admin
LEFT JOIN admin AS existing_admin ON 1 = 1
WHERE existing_admin.id IS NULL;
