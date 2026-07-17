-- =============================================================================
-- ForecastPro — demo seed dataset (analytics, forecasts, reports, vendors)
-- =============================================================================
-- Run:
--   mysql -u root -p forecastpro < scripts/db/seed-large.sql
--
-- Logins:
--   admin / Admin@123
--   manager / Manager@123
--   employee / Employee@123
--   craftwood, homecomfort, electrolink, summit / Vendor@123
--     craftwood   = CraftWood Furniture (Furniture)
--     homecomfort = HomeComfort Appliances (Home Appliances)
--     electrolink = ElectroLink Distributors (Electronics)
--     summit      = Summit General Supply (general)
-- Date range: 2025-01-01 → 2026-06-30 (last 18 months)
-- =============================================================================

USE forecastpro;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM forecasts;
DELETE FROM sales;
DELETE FROM inventory;
DELETE FROM stock_requests;
DELETE FROM messages;
DELETE FROM products;
DELETE FROM categories;
DELETE FROM vendors;
DELETE FROM users;

ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE categories AUTO_INCREMENT = 1;
ALTER TABLE products AUTO_INCREMENT = 1;
ALTER TABLE sales AUTO_INCREMENT = 1;
ALTER TABLE forecasts AUTO_INCREMENT = 1;
ALTER TABLE inventory AUTO_INCREMENT = 1;
ALTER TABLE stock_requests AUTO_INCREMENT = 1;
ALTER TABLE messages AUTO_INCREMENT = 1;
ALTER TABLE vendors AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- USERS
INSERT INTO users (username, password, role, enabled) VALUES
('admin', '$2a$10$0oRlWEw2lqIIKrFM9LrB4.WHx5Dj9XtnEm8mydnmPuWjD5Ww0Sb2K', 'ADMIN', 1),
('manager', '$2a$10$WpmOaDE8COuLZR5u195q5ewMhSMg4o5qjTmaW.B0HEhAcrE6XeGQG', 'SALES_MANAGER', 1),
('employee', '$2a$10$m1r2gXS.t5kjGtAAshaFr.9vcDAjN3bUVXaysx91SbxjiF3PN12oO', 'EMPLOYEE', 1),
('craftwood', '$2a$10$DOEkYkpLRDPVrlcY9bjJCuuYq6yuSTFmb0prI/Ccl7POO2wzYQ/3q', 'VENDOR', 1),
('homecomfort', '$2a$10$DOEkYkpLRDPVrlcY9bjJCuuYq6yuSTFmb0prI/Ccl7POO2wzYQ/3q', 'VENDOR', 1),
('electrolink', '$2a$10$DOEkYkpLRDPVrlcY9bjJCuuYq6yuSTFmb0prI/Ccl7POO2wzYQ/3q', 'VENDOR', 1),
('summit', '$2a$10$DOEkYkpLRDPVrlcY9bjJCuuYq6yuSTFmb0prI/Ccl7POO2wzYQ/3q', 'VENDOR', 1);

-- CATEGORIES
INSERT INTO categories (name) VALUES
('Furniture'),
('Home Appliances'),
('Electronics');

-- PRODUCTS
INSERT INTO products (name, category_id, price, stock_quantity) VALUES
('Oak Desk', 1, 1299.00, 120),
('Chair', 1, 349.50, 280),
('Dining Table', 1, 899.00, 90),
('Sofa Set', 1, 2499.00, 55),
('Bed Frame', 1, 1199.00, 75),
('Refrigerator', 2, 899.00, 110),
('Microwave', 2, 159.00, 6),
('Air Conditioner', 2, 3299.00, 65),
('Ceiling Fan', 2, 249.00, 140),
('Water Heater', 2, 449.00, 95),
('Laptop', 3, 1199.00, 5),
('Smartphone', 3, 799.00, 12),
('Smart Watch', 3, 299.00, 8),
('Bluetooth Speaker', 3, 149.00, 7),
('Gaming Console', 3, 499.00, 3);

-- VENDORS
INSERT INTO vendors (name, contact, status, user_id) VALUES
('CraftWood Furniture', 'contact@craftwood.com', 'ACTIVE', 4),
('HomeComfort Appliances', 'sales@homecomfort.com', 'ACTIVE', 5),
('ElectroLink Distributors', 'orders@electrolink.com', 'ACTIVE', 6),
('Summit General Supply', 'info@summitsupply.com', 'ACTIVE', 7);

-- SALES (2025-01-01 → 2026-06-30, 1 sale per product per month)
INSERT INTO sales (product_id, quantity_sold, sale_date) VALUES
(1, 2, '2025-01-29'),
(1, 2, '2025-01-06'),
(2, 12, '2025-01-16'),
(3, 3, '2025-01-13'),
(4, 3, '2025-01-31'),
(5, 3, '2025-01-28'),
(6, 5, '2025-01-13'),
(7, 3, '2025-01-05'),
(8, 3, '2025-01-21'),
(9, 5, '2025-01-16'),
(10, 7, '2025-01-27'),
(11, 10, '2025-01-19'),
(12, 3, '2025-01-07'),
(13, 3, '2025-01-31'),
(14, 3, '2025-01-15'),
(15, 3, '2025-01-16'),
(1, 4, '2025-02-08'),
(2, 16, '2025-02-02'),
(3, 3, '2025-02-09'),
(4, 4, '2025-02-09'),
(5, 3, '2025-02-06'),
(6, 4, '2025-02-17'),
(7, 3, '2025-02-08'),
(8, 3, '2025-02-25'),
(9, 6, '2025-02-27'),
(10, 9, '2025-02-02'),
(11, 10, '2025-02-27'),
(12, 3, '2025-02-22'),
(13, 5, '2025-02-26'),
(14, 4, '2025-02-11'),
(15, 3, '2025-02-07'),
(1, 6, '2025-03-21'),
(2, 16, '2025-03-18'),
(3, 3, '2025-03-21'),
(4, 4, '2025-03-06'),
(5, 4, '2025-03-25'),
(6, 7, '2025-03-09'),
(7, 4, '2025-03-04'),
(8, 3, '2025-03-04'),
(9, 16, '2025-03-20'),
(10, 3, '2025-03-09'),
(11, 12, '2025-03-19'),
(12, 3, '2025-03-12'),
(13, 4, '2025-03-03'),
(14, 3, '2025-03-06'),
(15, 3, '2025-03-27'),
(1, 3, '2025-04-01'),
(1, 3, '2025-04-05'),
(2, 12, '2025-04-07'),
(3, 4, '2025-04-11'),
(4, 7, '2025-04-10'),
(5, 4, '2025-04-26'),
(6, 5, '2025-04-15'),
(7, 4, '2025-04-24'),
(8, 10, '2025-04-24'),
(9, 11, '2025-04-04'),
(10, 4, '2025-04-25'),
(11, 11, '2025-04-25'),
(12, 4, '2025-04-01'),
(13, 4, '2025-04-11'),
(14, 7, '2025-04-15'),
(15, 3, '2025-04-09'),
(1, 4, '2025-05-14'),
(2, 15, '2025-05-04'),
(3, 4, '2025-05-08'),
(4, 6, '2025-05-05'),
(5, 3, '2025-05-06'),
(6, 6, '2025-05-29'),
(7, 3, '2025-05-12'),
(8, 12, '2025-05-21'),
(9, 16, '2025-05-23'),
(10, 3, '2025-05-09'),
(11, 12, '2025-05-02'),
(12, 4, '2025-05-15'),
(13, 4, '2025-05-01'),
(14, 3, '2025-05-28'),
(14, 3, '2025-05-18'),
(15, 3, '2025-05-01'),
(1, 4, '2025-06-08'),
(2, 14, '2025-06-24'),
(3, 3, '2025-06-04'),
(4, 7, '2025-06-02'),
(5, 3, '2025-06-24'),
(6, 5, '2025-06-21'),
(7, 4, '2025-06-13'),
(8, 13, '2025-06-17'),
(9, 14, '2025-06-16'),
(10, 3, '2025-06-01'),
(11, 12, '2025-06-30'),
(12, 4, '2025-06-30'),
(13, 4, '2025-06-15'),
(14, 7, '2025-06-29'),
(15, 3, '2025-06-10'),
(1, 4, '2025-07-02'),
(2, 15, '2025-07-14'),
(3, 4, '2025-07-14'),
(4, 5, '2025-07-04'),
(5, 3, '2025-07-14'),
(6, 5, '2025-07-23'),
(7, 3, '2025-07-22'),
(8, 4, '2025-07-27'),
(9, 15, '2025-07-25'),
(10, 3, '2025-07-20'),
(11, 12, '2025-07-14'),
(12, 4, '2025-07-28'),
(13, 4, '2025-07-18'),
(14, 6, '2025-07-04'),
(15, 3, '2025-07-05'),
(1, 4, '2025-08-22'),
(2, 19, '2025-08-06'),
(3, 4, '2025-08-02'),
(4, 7, '2025-08-26'),
(5, 2, '2025-08-18'),
(5, 2, '2025-08-25'),
(6, 4, '2025-08-16'),
(7, 3, '2025-08-09'),
(8, 3, '2025-08-24'),
(9, 5, '2025-08-25'),
(10, 4, '2025-08-18'),
(11, 13, '2025-08-30'),
(12, 3, '2025-08-27'),
(13, 3, '2025-08-06'),
(14, 6, '2025-08-15'),
(15, 3, '2025-08-17'),
(1, 4, '2025-09-23'),
(2, 18, '2025-09-23'),
(3, 4, '2025-09-04'),
(4, 4, '2025-09-07'),
(5, 3, '2025-09-28'),
(6, 5, '2025-09-04'),
(7, 4, '2025-09-28'),
(8, 4, '2025-09-29'),
(9, 6, '2025-09-15'),
(10, 3, '2025-09-17'),
(11, 13, '2025-09-16'),
(12, 3, '2025-09-16'),
(13, 3, '2025-09-26'),
(14, 4, '2025-09-13'),
(15, 3, '2025-09-10'),
(1, 4, '2025-10-31'),
(2, 6, '2025-10-04'),
(2, 7, '2025-10-29'),
(3, 4, '2025-10-19'),
(4, 3, '2025-10-01'),
(5, 3, '2025-10-11'),
(6, 5, '2025-10-10'),
(7, 4, '2025-10-08'),
(8, 3, '2025-10-27'),
(9, 5, '2025-10-31'),
(10, 1, '2025-10-26'),
(10, 2, '2025-10-06'),
(11, 15, '2025-10-29'),
(12, 5, '2025-10-18'),
(13, 2, '2025-10-09'),
(13, 2, '2025-10-10'),
(14, 3, '2025-10-16'),
(15, 2, '2025-10-17'),
(1, 4, '2025-11-03'),
(2, 6, '2025-11-01'),
(2, 7, '2025-11-08'),
(3, 4, '2025-11-03'),
(4, 3, '2025-11-16'),
(5, 3, '2025-11-06'),
(6, 4, '2025-11-29'),
(7, 3, '2025-11-09'),
(8, 3, '2025-11-30'),
(9, 6, '2025-11-20'),
(10, 10, '2025-11-02'),
(11, 14, '2025-11-22'),
(12, 10, '2025-11-14'),
(13, 8, '2025-11-02'),
(14, 10, '2025-11-12'),
(15, 5, '2025-11-23'),
(1, 5, '2025-12-26'),
(2, 13, '2025-12-25'),
(3, 3, '2025-12-18'),
(4, 3, '2025-12-06'),
(5, 3, '2025-12-11'),
(6, 4, '2025-12-01'),
(7, 4, '2025-12-31'),
(8, 3, '2025-12-12'),
(9, 7, '2025-12-31'),
(10, 9, '2025-12-12'),
(11, 16, '2025-12-24'),
(12, 11, '2025-12-09'),
(13, 9, '2025-12-08'),
(14, 9, '2025-12-18'),
(15, 4, '2025-12-22'),
(1, 5, '2026-01-01'),
(2, 13, '2026-01-10'),
(3, 3, '2026-01-14'),
(4, 4, '2026-01-27'),
(5, 4, '2026-01-27'),
(6, 5, '2026-01-29'),
(7, 3, '2026-01-29'),
(8, 4, '2026-01-05'),
(9, 6, '2026-01-04'),
(10, 8, '2026-01-20'),
(11, 16, '2026-01-02'),
(12, 2, '2026-01-05'),
(12, 2, '2026-01-18'),
(13, 6, '2026-01-29'),
(14, 4, '2026-01-01'),
(15, 2, '2026-01-04'),
(15, 2, '2026-01-14'),
(1, 5, '2026-02-20'),
(2, 14, '2026-02-10'),
(3, 3, '2026-02-20'),
(4, 3, '2026-02-16'),
(5, 3, '2026-02-04'),
(6, 2, '2026-02-23'),
(6, 3, '2026-02-11'),
(7, 4, '2026-02-25'),
(8, 3, '2026-02-27'),
(9, 6, '2026-02-22'),
(10, 10, '2026-02-18'),
(11, 15, '2026-02-05'),
(12, 3, '2026-02-24'),
(12, 3, '2026-02-20'),
(13, 15, '2026-02-28'),
(14, 3, '2026-02-09'),
(15, 4, '2026-02-04'),
(1, 5, '2026-03-21'),
(2, 16, '2026-03-01'),
(3, 2, '2026-03-06'),
(3, 2, '2026-03-10'),
(4, 3, '2026-03-17'),
(5, 5, '2026-03-16'),
(6, 7, '2026-03-21'),
(7, 4, '2026-03-24'),
(8, 3, '2026-03-30'),
(9, 16, '2026-03-09'),
(10, 4, '2026-03-14'),
(11, 16, '2026-03-23'),
(12, 3, '2026-03-17'),
(12, 3, '2026-03-10'),
(13, 5, '2026-03-21'),
(14, 4, '2026-03-30'),
(15, 4, '2026-03-03'),
(1, 3, '2026-04-17'),
(1, 3, '2026-04-07'),
(2, 16, '2026-04-11'),
(3, 4, '2026-04-01'),
(4, 6, '2026-04-29'),
(5, 5, '2026-04-20'),
(6, 7, '2026-04-17'),
(7, 4, '2026-04-07'),
(8, 14, '2026-04-30'),
(9, 15, '2026-04-30'),
(10, 4, '2026-04-22'),
(11, 16, '2026-04-20'),
(12, 6, '2026-04-22'),
(13, 4, '2026-04-30'),
(14, 7, '2026-04-05'),
(15, 4, '2026-04-19'),
(1, 6, '2026-05-09'),
(2, 15, '2026-05-22'),
(3, 4, '2026-05-03'),
(4, 6, '2026-05-04'),
(5, 4, '2026-05-13'),
(6, 7, '2026-05-01'),
(7, 4, '2026-05-22'),
(8, 14, '2026-05-09'),
(9, 15, '2026-05-13'),
(10, 4, '2026-05-02'),
(11, 17, '2026-05-06'),
(12, 5, '2026-05-27'),
(13, 4, '2026-05-22'),
(14, 6, '2026-05-03'),
(15, 5, '2026-05-01'),
(1, 4, '2026-06-23'),
(2, 15, '2026-06-16'),
(3, 3, '2026-06-30'),
(4, 7, '2026-06-19'),
(5, 4, '2026-06-06'),
(6, 5, '2026-06-01'),
(7, 4, '2026-06-21'),
(8, 12, '2026-06-21'),
(9, 14, '2026-06-11'),
(10, 3, '2026-06-09'),
(11, 17, '2026-06-20'),
(12, 5, '2026-06-30'),
(13, 6, '2026-06-21'),
(14, 9, '2026-06-26'),
(15, 4, '2026-06-04');

-- FORECASTS (last 3 months per product)
INSERT INTO forecasts (product_id, moving_avg_value, ml_regression_value, predicted_sales, predicted_revenue, forecast_month, lower_bound, upper_bound, created_at) VALUES
(1, 5.0000, 4.7273, 4.6000, 5975.4000, '2026-04-01', 3.9545, 5.2455, '2026-04-06 09:15:00.000000'),
(1, 5.3333, 5.5455, 5.4000, 7014.6000, '2026-05-01', 4.7545, 6.0455, '2026-05-06 09:15:00.000000'),
(1, 5.6667, 5.9394, 5.7500, 7469.2500, '2026-06-01', 5.0046, 6.4954, '2026-06-06 09:15:00.000000'),
(2, 14.3333, 14.4697, 17.2800, 6039.3600, '2026-04-01', 15.2201, 19.3399, '2026-04-07 09:15:00.000000'),
(2, 15.3333, 14.3939, 15.0500, 5259.9750, '2026-05-01', 13.1153, 16.9847, '2026-05-07 09:15:00.000000'),
(2, 15.6667, 14.4394, 15.3000, 5347.3500, '2026-06-01', 13.3653, 17.2347, '2026-06-07 09:15:00.000000'),
(3, 3.3333, 3.3485, 3.6800, 3308.3200, '2026-04-01', 3.2086, 4.1514, '2026-04-08 09:15:00.000000'),
(3, 3.6667, 3.5303, 3.6300, 3263.3700, '2026-05-01', 3.1586, 4.1014, '2026-05-08 09:15:00.000000'),
(3, 4.0000, 3.7121, 3.9100, 3515.0900, '2026-06-01', 3.4386, 4.3814, '2026-06-08 09:15:00.000000'),
(4, 3.3333, 2.6667, 3.2400, 8096.7600, '2026-04-01', 1.5838, 4.8962, '2026-04-09 09:15:00.000000'),
(4, 4.0000, 3.2000, 3.7600, 9396.2400, '2026-05-01', 2.2054, 5.3146, '2026-05-09 09:15:00.000000'),
(4, 5.0000, 4.0000, 4.7000, 11745.3000, '2026-06-01', 3.1454, 6.2546, '2026-06-09 09:15:00.000000'),
(5, 4.0000, 3.7576, 4.6000, 5515.4000, '2026-04-01', 3.9599, 5.2401, '2026-04-10 09:15:00.000000'),
(5, 4.3333, 4.4091, 4.3600, 5227.6400, '2026-05-01', 3.5962, 5.1238, '2026-05-10 09:15:00.000000'),
(5, 4.6667, 4.4697, 4.6100, 5527.3900, '2026-06-01', 3.8508, 5.3692, '2026-06-10 09:15:00.000000'),
(6, 5.6667, 5.1818, 7.5600, 6796.4400, '2026-04-01', 6.7435, 8.3765, '2026-04-11 09:15:00.000000'),
(6, 6.3333, 5.8485, 6.1900, 5564.8100, '2026-05-01', 5.2040, 7.1760, '2026-05-11 09:15:00.000000'),
(6, 7.0000, 6.6364, 6.8900, 6194.1100, '2026-06-01', 5.8003, 7.9797, '2026-06-11 09:15:00.000000'),
(7, 3.6667, 3.7424, 3.6800, 585.1200, '2026-04-01', 3.1870, 4.1730, '2026-04-12 09:15:00.000000'),
(7, 4.0000, 3.9697, 3.9900, 634.4100, '2026-05-01', 3.4970, 4.4830, '2026-05-12 09:15:00.000000'),
(7, 16.5600, 18.9000, 18.0000, 2862.0000, '2026-06-01', 17.5286, 18.4714, '2026-06-12 09:15:00.000000'),
(8, 3.3333, 2.6667, 3.2400, 10688.7600, '2026-04-01', 0.0000, 6.9246, '2026-04-13 09:15:00.000000'),
(8, 6.6667, 5.3333, 6.2700, 20684.7300, '2026-05-01', 2.0446, 10.4954, '2026-05-13 09:15:00.000000'),
(8, 10.3333, 8.2667, 9.7100, 32033.2900, '2026-06-01', 5.2108, 14.2092, '2026-06-13 09:15:00.000000'),
(9, 9.3333, 7.4667, 14.7200, 3665.2800, '2026-04-01', 10.3144, 19.1256, '2026-04-14 09:15:00.000000'),
(9, 12.3333, 9.8667, 11.5900, 2885.9100, '2026-05-01', 6.9330, 16.2470, '2026-05-14 09:15:00.000000'),
(9, 15.3333, 12.2667, 14.4100, 3588.0900, '2026-06-01', 9.8578, 18.9622, '2026-06-14 09:15:00.000000'),
(10, 7.3333, 8.6515, 4.3200, 1939.6800, '2026-04-01', 1.4818, 7.1582, '2026-04-15 09:15:00.000000'),
(10, 6.0000, 7.2000, 6.3600, 2855.6400, '2026-05-01', 3.5218, 9.1982, '2026-05-15 09:15:00.000000'),
(10, 4.0000, 4.8000, 4.2400, 1903.7600, '2026-06-01', 1.4574, 7.0226, '2026-06-15 09:15:00.000000'),
(11, 15.6667, 16.8182, 14.7200, 17649.2800, '2026-04-01', 12.9819, 16.4581, '2026-04-16 09:15:00.000000'),
(11, 15.6667, 16.9848, 16.0600, 19255.9400, '2026-05-01', 14.4355, 17.6845, '2026-05-16 09:15:00.000000'),
(11, 32.2000, 36.7500, 35.0000, 41965.0000, '2026-06-01', 33.3438, 36.6562, '2026-06-16 09:15:00.000000'),
(12, 5.3333, 6.4000, 6.4800, 5177.5200, '2026-04-01', 3.9856, 8.9744, '2026-04-17 09:15:00.000000'),
(12, 6.0000, 7.2000, 6.3600, 5081.6400, '2026-05-01', 3.8936, 8.8264, '2026-05-17 09:15:00.000000'),
(12, 46.0000, 52.5000, 50.0000, 39950.0000, '2026-06-01', 47.5690, 52.4310, '2026-06-17 09:15:00.000000'),
(13, 8.6667, 9.5000, 4.6000, 1375.4000, '2026-04-01', 1.2802, 7.9198, '2026-04-18 09:15:00.000000'),
(13, 8.0000, 8.5455, 8.1600, 2439.8400, '2026-05-01', 4.8402, 11.4798, '2026-05-18 09:15:00.000000'),
(13, 4.3333, 5.2000, 4.5900, 1372.4100, '2026-06-01', 1.2702, 7.9098, '2026-06-18 09:15:00.000000'),
(14, 3.6667, 4.4000, 4.3200, 643.6800, '2026-04-01', 2.1549, 6.4851, '2026-04-19 09:15:00.000000'),
(14, 4.6667, 5.1818, 4.8200, 718.1800, '2026-05-01', 2.6549, 6.9851, '2026-05-19 09:15:00.000000'),
(14, 27.6000, 31.5000, 30.0000, 4470.0000, '2026-06-01', 27.8349, 32.1651, '2026-06-19 09:15:00.000000'),
(15, 4.0000, 4.2576, 3.6800, 1836.3200, '2026-04-01', 2.9208, 4.4392, '2026-04-20 09:15:00.000000'),
(15, 4.0000, 4.3636, 4.1100, 2050.8900, '2026-05-01', 3.3462, 4.8738, '2026-05-20 09:15:00.000000'),
(15, 18.4000, 21.0000, 20.0000, 9980.0000, '2026-06-01', 19.1502, 20.8498, '2026-06-20 09:15:00.000000');

-- INVENTORY
INSERT INTO inventory (product_id, quantity_added, date_added, source, created_at) VALUES
(11, 16, '2025-01-10', 'ElectroLink Distributors', '2025-01-10 10:00:00.000000'),
(4, 19, '2025-01-22', 'CraftWood Furniture', '2025-01-22 16:00:00.000000'),
(15, 20, '2025-02-02', 'ElectroLink Distributors', '2025-02-02 17:00:00.000000'),
(13, 72, '2025-02-09', 'ElectroLink Distributors', '2025-02-09 15:00:00.000000'),
(15, 16, '2025-02-15', 'ElectroLink Distributors', '2025-02-15 17:00:00.000000'),
(2, 45, '2025-02-23', 'CraftWood Furniture', '2025-02-23 17:00:00.000000'),
(12, 54, '2025-03-09', 'ElectroLink Distributors', '2025-03-09 11:00:00.000000'),
(15, 59, '2025-03-15', 'ElectroLink Distributors', '2025-03-15 10:00:00.000000'),
(2, 27, '2025-03-21', 'CraftWood Furniture', '2025-03-21 12:00:00.000000'),
(8, 57, '2025-03-28', 'HomeComfort Appliances', '2025-03-28 17:00:00.000000'),
(1, 67, '2025-04-08', 'CraftWood Furniture', '2025-04-08 13:00:00.000000'),
(12, 19, '2025-04-16', 'ElectroLink Distributors', '2025-04-16 16:00:00.000000'),
(7, 65, '2025-04-29', 'HomeComfort Appliances', '2025-04-29 11:00:00.000000'),
(5, 23, '2025-05-07', 'CraftWood Furniture', '2025-05-07 15:00:00.000000'),
(12, 52, '2025-05-14', 'ElectroLink Distributors', '2025-05-14 08:00:00.000000'),
(11, 9, '2025-05-28', 'ElectroLink Distributors', '2025-05-28 08:00:00.000000'),
(12, 35, '2025-06-04', 'ElectroLink Distributors', '2025-06-04 16:00:00.000000'),
(7, 44, '2025-06-17', 'HomeComfort Appliances', '2025-06-17 13:00:00.000000'),
(15, 20, '2025-06-28', 'ElectroLink Distributors', '2025-06-28 11:00:00.000000'),
(14, 61, '2025-07-07', 'ElectroLink Distributors', '2025-07-07 08:00:00.000000'),
(15, 55, '2025-07-14', 'ElectroLink Distributors', '2025-07-14 12:00:00.000000'),
(2, 35, '2025-07-21', 'CraftWood Furniture', '2025-07-21 11:00:00.000000'),
(14, 25, '2025-07-31', 'ElectroLink Distributors', '2025-07-31 13:00:00.000000'),
(15, 57, '2025-08-03', 'ElectroLink Distributors', '2025-08-03 12:00:00.000000'),
(14, 15, '2025-08-13', 'ElectroLink Distributors', '2025-08-13 15:00:00.000000'),
(12, 57, '2025-08-21', 'ElectroLink Distributors', '2025-08-21 14:00:00.000000'),
(15, 17, '2025-08-26', 'ElectroLink Distributors', '2025-08-26 12:00:00.000000'),
(9, 13, '2025-09-03', 'HomeComfort Appliances', '2025-09-03 14:00:00.000000'),
(10, 75, '2025-09-17', 'HomeComfort Appliances', '2025-09-17 13:00:00.000000'),
(8, 8, '2025-09-20', 'HomeComfort Appliances', '2025-09-20 10:00:00.000000');

-- STOCK REQUESTS
INSERT INTO stock_requests (product_id, requested_quantity, status, vendor_id, request_date) VALUES
(11, 66, 'PENDING', 3, '2025-02-01'),
(11, 27, 'APPROVED', 3, '2025-02-06'),
(7, 35, 'READY_PACKED', 2, '2025-02-21'),
(5, 50, 'OUT_FOR_DELIVERY', 1, '2025-03-07'),
(2, 24, 'DELIVERED', 1, '2025-03-14'),
(4, 76, 'COMPLETED', 1, '2025-03-30'),
(13, 116, 'REJECTED', 3, '2025-04-14'),
(13, 26, 'PENDING', 3, '2025-05-02'),
(10, 97, 'APPROVED', 2, '2025-05-13'),
(13, 50, 'READY_PACKED', 3, '2025-05-25'),
(8, 104, 'OUT_FOR_DELIVERY', 2, '2025-06-01'),
(5, 32, 'DELIVERED', 1, '2025-06-16'),
(10, 115, 'COMPLETED', 2, '2025-06-26'),
(13, 46, 'REJECTED', 3, '2025-07-04'),
(2, 63, 'PENDING', 1, '2025-07-14'),
(3, 60, 'APPROVED', 1, '2025-07-25'),
(14, 83, 'READY_PACKED', 3, '2025-08-02'),
(1, 12, 'OUT_FOR_DELIVERY', 1, '2025-08-15'),
(11, 88, 'DELIVERED', 3, '2025-08-30'),
(4, 21, 'COMPLETED', 1, '2025-09-11');

-- MESSAGES
INSERT INTO messages (sender_id, receiver_id, subject, message, status, created_at) VALUES
(3, 1, 'Inventory Issue', 'Laptop stock is critically low. Please approve an urgent restock request.', 'CLOSED', '2025-01-01 00:00:00'),
(2, 1, 'Forecast Review', 'Please review the updated demand forecast before the monthly planning meeting.', 'OPEN', '2025-01-15 01:00:00'),
(1, 3, 'Monthly Report', 'Please find the monthly KPI summary for admin review.', 'OPEN', '2025-01-29 02:00:00'),
(1, 4, 'Stock Shortage', 'Bluetooth Speaker stock will run out within two weeks at current sales rate.', 'CLOSED', '2025-02-12 03:00:00'),
(4, 1, 'Vendor Complaint', 'Vendor delivery was incomplete — 15 Smartphone units missing from invoice.', 'OPEN', '2025-02-26 04:00:00'),
(5, 1, 'Delivery Delay', 'Ceiling Fan order is stuck in transit. Warehouse needs an update.', 'OPEN', '2025-03-12 05:00:00'),
(2, 3, 'Forecast Accuracy', 'Last month forecast for Smartphone was 12% above actual sales.', 'CLOSED', '2025-03-26 06:00:00'),
(1, 2, 'System Feedback', 'Suggestion: add export option on inventory alerts table.', 'OPEN', '2025-04-09 07:00:00'),
(3, 1, 'Inventory Issue', 'Several furniture items show negative movement in the last audit.', 'OPEN', '2025-04-23 00:00:00'),
(2, 1, 'Forecast Review', 'Smartphone demand forecast for next month looks high. Can we review assumptions?', 'CLOSED', '2025-05-07 01:00:00'),
(1, 3, 'Monthly Report', 'Monthly revenue report is ready. Electronics category led growth this month.', 'OPEN', '2025-05-21 02:00:00'),
(1, 4, 'Stock Shortage', 'Smart Watch inventory is below safety level. Requesting approval to reorder.', 'OPEN', '2025-06-04 03:00:00'),
(4, 1, 'Vendor Complaint', 'ElectroLink Distributors delivered damaged Laptop units in the last shipment.', 'CLOSED', '2025-06-18 04:00:00'),
(5, 1, 'Delivery Delay', 'Vendor confirmed Water Heater delivery pushed to next Friday.', 'OPEN', '2025-07-02 05:00:00'),
(2, 3, 'Forecast Accuracy', 'Can we discuss why Refrigerator forecast missed actual demand?', 'OPEN', '2025-07-16 06:00:00');

-- INDEXES (idempotent)
-- Index idx_sales_product_date on sales
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sales'
    AND INDEX_NAME = 'idx_sales_product_date'
);
SET @idx_sql = IF(@idx_exists = 0,
  'CREATE INDEX idx_sales_product_date ON sales (product_id, sale_date)',
  'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

-- Index idx_forecasts_month on forecasts
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'forecasts'
    AND INDEX_NAME = 'idx_forecasts_month'
);
SET @idx_sql = IF(@idx_exists = 0,
  'CREATE INDEX idx_forecasts_month ON forecasts (forecast_month)',
  'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

-- Index idx_inventory_product on inventory
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'inventory'
    AND INDEX_NAME = 'idx_inventory_product'
);
SET @idx_sql = IF(@idx_exists = 0,
  'CREATE INDEX idx_inventory_product ON inventory (product_id)',
  'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

-- Index idx_stock_requests_status on stock_requests
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'stock_requests'
    AND INDEX_NAME = 'idx_stock_requests_status'
);
SET @idx_sql = IF(@idx_exists = 0,
  'CREATE INDEX idx_stock_requests_status ON stock_requests (status)',
  'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

-- Index idx_messages_created_at on messages
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'messages'
    AND INDEX_NAME = 'idx_messages_created_at'
);
SET @idx_sql = IF(@idx_exists = 0,
  'CREATE INDEX idx_messages_created_at ON messages (created_at)',
  'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

-- Ensure unique (product_id, forecast_month)
SET @uk_exists = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'forecasts'
    AND CONSTRAINT_NAME = 'uk_forecasts_product_month'
);
SET @uk_sql = IF(@uk_exists = 0,
  'ALTER TABLE forecasts ADD CONSTRAINT uk_forecasts_product_month UNIQUE (product_id, forecast_month)',
  'SELECT 1');
PREPARE uk_stmt FROM @uk_sql;
EXECUTE uk_stmt;
DEALLOCATE PREPARE uk_stmt;

-- STATS: sales=285, forecasts=45, inventory=30, stock_requests=20, messages=15
-- Done.
