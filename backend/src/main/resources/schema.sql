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

CREATE TABLE IF NOT EXISTS product_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100) NOT NULL,
    product_intro TEXT,
    official_url VARCHAR(255),
    core_features TEXT,
    target_users TEXT,
    advantages TEXT,
    brand_tone VARCHAR(255),
    banned_words TEXT,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content LONGTEXT,
    type VARCHAR(50) NOT NULL,
    language VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    tags VARCHAR(500),
    keywords VARCHAR(500),
    created_by BIGINT,
    updated_by BIGINT,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS article_platform_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content LONGTEXT,
    tags VARCHAR(500),
    keywords VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_article_platform (article_id, platform),
    KEY idx_article_platform_content_article_id (article_id),
    KEY idx_article_platform_content_status (status)
);
