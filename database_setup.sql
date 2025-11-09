CREATE DATABASE IF NOT EXISTS eviaai_db;
USE eviaai_db;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(100) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    credits INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS items (
    name VARCHAR(100) PRIMARY KEY,
    raw_materials TEXT NOT NULL,
    how_to_make TEXT NOT NULL,
    how_to_use TEXT NOT NULL,
    where_to_use TEXT NOT NULL,
    contributor VARCHAR(100) NOT NULL,
    credits INT DEFAULT 0,
    FOREIGN KEY (contributor) REFERENCES users(id)
);