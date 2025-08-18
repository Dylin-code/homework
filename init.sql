CREATE DATABASE IF NOT EXISTS taskdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE taskdb;

CREATE TABLE IF NOT EXISTS users (
                                     user_id       VARCHAR(64) PRIMARY KEY,
    balance       DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transfers (
                                         transfer_id   VARCHAR(64) PRIMARY KEY,
    from_user_id  VARCHAR(64) NOT NULL,
    to_user_id    VARCHAR(64) NOT NULL,
    amount        DECIMAL(18,2) NOT NULL,
    status        VARCHAR(32) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    canceled_at   TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_from_user FOREIGN KEY (from_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_to_user   FOREIGN KEY (to_user_id)   REFERENCES users(user_id),
    INDEX idx_user_created (from_user_id, created_at),
    INDEX idx_user2_created (to_user_id, created_at),
    INDEX idx_created (created_at)
    ) ENGINE=InnoDB;

