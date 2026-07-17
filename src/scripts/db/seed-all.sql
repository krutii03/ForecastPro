-- =============================================================================
-- ForecastPro — FULL seed data (users, categories, products, sales, forecasts)
-- =============================================================================
-- Run AFTER db/schema.sql on database `forecastpro`.
--
-- IMPORTANT: product_id values in SALES assume products were inserted in the order below
-- and received AUTO_INCREMENT ids 1–12 with category ids 1–3. If your tables already had
-- data, ids will be wrong — uncomment TRUNCATE below first, or use a new database.
--
-- If you already have rows, either:
--   (1) Use a fresh database, OR
--   (2) Uncomment the TRUNCATE block below (disables FK checks, wipes all app data).
--
-- Table names: users, categories, products, sales, forecasts
-- =============================================================================

USE forecastpro;

-- Uncomment to replace all seeded data (order respects foreign keys):
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE forecasts;
-- TRUNCATE TABLE sales;
-- TRUNCATE TABLE products;
-- TRUNCATE TABLE categories;
-- TRUNCATE TABLE users;
-- SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- USERS (jBCrypt hashes — matches PasswordUtil: Admin@123, Manager@123, Employee@123)
-- -----------------------------------------------------------------------------
INSERT INTO users (username, password, role, enabled) VALUES
('admin', '$2a$10$0oRlWEw2lqIIKrFM9LrB4.WHx5Dj9XtnEm8mydnmPuWjD5Ww0Sb2K', 'ADMIN', 1),
('manager', '$2a$10$WpmOaDE8COuLZR5u195q5ewMhSMg4o5qjTmaW.B0HEhAcrE6XeGQG', 'SALES_MANAGER', 1),
('employee', '$2a$10$m1r2gXS.t5kjGtAAshaFr.9vcDAjN3bUVXaysx91SbxjiF3PN12oO', 'EMPLOYEE', 1);

-- -----------------------------------------------------------------------------
-- CATEGORIES (ids 1–3 if table was empty)
-- -----------------------------------------------------------------------------
INSERT INTO categories (name) VALUES
('Furniture'),
('Home Appliances'),
('Electronics');

-- -----------------------------------------------------------------------------
-- PRODUCTS
-- category_id: 1=Furniture, 2=Home Appliances, 3=Electronics
-- stock_quantity = initial stock MINUS total sales (same as DataSeedService)
-- -----------------------------------------------------------------------------
INSERT INTO products (name, category_id, price, stock_quantity) VALUES
('Oak Desk', 1, 1299.00, 75),
('Office Chair', 1, 349.50, 55),
('Bookshelf', 1, 199.99, 30),
('Refrigerator', 2, 899.00, 61),
('Microwave', 2, 159.00, 45),
('Washing Machine', 2, 649.00, 12),
('Air Fryer', 2, 129.00, 60),
('Laptop 15"', 3, 1199.00, 41),
('Tablet', 3, 399.00, 35),
('Smartphone', 3, 799.00, 55),
('Wireless Headphones', 3, 149.00, 71),
('LED TV 55"', 3, 699.00, 15);

-- -----------------------------------------------------------------------------
-- SALES (monthly totals on the 15th, Oct 2025 → Mar 2026 — matches Java seed)
-- product_id order: 1=Oak Desk, 4=Refrigerator, 8=Laptop, 10=Smartphone, 5=Microwave, 11=Headphones
-- -----------------------------------------------------------------------------
INSERT INTO sales (product_id, quantity_sold, sale_date) VALUES
-- Oak Desk (product_id = 1)
(1, 5, '2025-10-15'), (1, 8, '2025-11-15'), (1, 6, '2025-12-15'), (1, 9, '2026-01-15'), (1, 7, '2026-02-15'), (1, 10, '2026-03-15'),
-- Refrigerator (4)
(4, 2, '2025-10-15'), (4, 3, '2025-11-15'), (4, 2, '2025-12-15'), (4, 4, '2026-01-15'), (4, 3, '2026-02-15'), (4, 5, '2026-03-15'),
-- Laptop (8)
(8, 4, '2025-10-15'), (8, 6, '2025-11-15'), (8, 5, '2025-12-15'), (8, 8, '2026-01-15'), (8, 7, '2026-02-15'), (8, 9, '2026-03-15'),
-- Smartphone (10)
(10, 12, '2025-10-15'), (10, 15, '2025-11-15'), (10, 14, '2025-12-15'), (10, 18, '2026-01-15'), (10, 16, '2026-02-15'), (10, 20, '2026-03-15'),
-- Microwave (5)
(5, 10, '2025-10-15'), (5, 12, '2025-11-15'), (5, 11, '2025-12-15'), (5, 14, '2026-01-15'), (5, 13, '2026-02-15'), (5, 15, '2026-03-15'),
-- Wireless Headphones (11)
(11, 20, '2025-10-15'), (11, 22, '2025-11-15'), (11, 25, '2025-12-15'), (11, 24, '2026-01-15'), (11, 28, '2026-02-15'), (11, 30, '2026-03-15');

-- -----------------------------------------------------------------------------
-- FORECASTS (sample rows for analytics / alerts demo — optional but fills table)
-- -----------------------------------------------------------------------------
INSERT INTO forecasts (product_id, moving_avg_value, ml_regression_value, predicted_sales, predicted_revenue, forecast_month, created_at) VALUES
(1, 8.3333, 9.5000, 8.9167, 11572.8033, '2026-04-01', '2026-03-15 10:00:00.000000'),
(10, 17.5000, 19.2000, 18.3500, 14661.6500, '2026-04-01', '2026-03-15 10:05:00.000000'),
(8, 6.8333, 7.8000, 7.3167, 8772.7233, '2026-04-01', '2026-03-15 10:10:00.000000'),
(4, 3.1667, 3.4000, 3.2834, 2951.7766, '2026-04-01', '2026-03-15 10:15:00.000000');

-- =============================================================================
-- Done. Login: admin / Admin@123 | manager / Manager@123 | employee / Employee@123
-- =============================================================================
