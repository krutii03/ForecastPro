-- ForecastPro — MySQL 8 schema
--
-- Run this ONCE in MySQL (Workbench, mysql CLI, or NetBeans Services → Databases) if tables do not exist.
-- Your JDBC pool URL must use the same database name (e.g. .../forecastpro).
--
-- Charset: utf8mb4

CREATE DATABASE IF NOT EXISTS forecastpro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE forecastpro;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BIT(1) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    price DECIMAL(14,2) NOT NULL,
    stock_quantity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS sales (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    quantity_sold INT NOT NULL,
    sale_date DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE IF NOT EXISTS forecasts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    moving_avg_value DECIMAL(18,4) NOT NULL,
    ml_regression_value DECIMAL(18,4) NOT NULL,
    predicted_sales DECIMAL(18,4) NOT NULL DEFAULT 0,
    predicted_revenue DECIMAL(18,4) NOT NULL DEFAULT 0,
    forecast_month DATE NOT NULL,
    lower_bound DECIMAL(18,4) NOT NULL DEFAULT 0,
    upper_bound DECIMAL(18,4) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_forecasts_product FOREIGN KEY (product_id) REFERENCES products (id),
    UNIQUE KEY uk_forecasts_product_month (product_id, forecast_month)
);

CREATE INDEX idx_sales_product ON sales (product_id);
CREATE INDEX idx_sales_date ON sales (sale_date);
CREATE INDEX idx_forecasts_product ON forecasts (product_id);
