INSERT INTO sys_user (
    username,
    password,
    nickname,
    email,
    role,
    status,
    created_at,
    updated_at
) VALUES (
    'admin',
    '$2a$10$IjUhOeB3SrBWIEm0uVZxvOEDxZUuB0UvJ8YdIOphI3OC2VMh5qfPK',
    '管理员',
    NULL,
    'ADMIN',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE username = username;
