INSERT IGNORE INTO roles (id, nombre) VALUES
    (1, 'Director'),
    (2, 'Celador'),
    (3, 'Residente');

INSERT IGNORE INTO habitaciones (numero, piso, activo) VALUES
    ('101', 1, true), ('102', 1, true), ('103', 1, true),
    ('201', 2, true), ('202', 2, true), ('203', 2, true),
    ('301', 3, true), ('302', 3, true), ('303', 3, true);

INSERT IGNORE INTO usuarios (
    nombre, apellidos, email, password_hash,
    rol_id, activo, coro_iglesia, token_version, intentos_fallidos
) VALUES
(
    'Director', 'AAUCA', 'director@aauca.edu',
    '$2a$10$HzTQmYdTwics5Gs5APWt1eSiHOBynNUOL18OANmBHjJSREi5j0BBi',
    1, true, false, 0, 0
),
(
    'Celador', 'AAUCA', 'celador@aauca.edu',
    '$2a$10$csefZNSFT4UHW1pTf/EjY.l/bKKwqwTnXy3Sy5DwX46Zy9dnlQWCa',
    2, true, false, 0, 0
),
(
    'Residente', 'AAUCA', 'residente@aauca.edu',
    '$2a$10$NM3JulcA/.SbDslwHHYjQe9HDI21Hgf/sNOcnaR6vAWwOyEYVKfPy',
    3, true, false, 0, 0
);