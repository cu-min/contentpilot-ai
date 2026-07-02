CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);
