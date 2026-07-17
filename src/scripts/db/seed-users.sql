-- Demo users only (subset of db/seed-all.sql).
-- For full data (categories, products, sales, forecasts), use db/seed-all.sql instead.

USE forecastpro;

INSERT INTO users (username, password, role, enabled) VALUES
('admin', '$2a$10$0oRlWEw2lqIIKrFM9LrB4.WHx5Dj9XtnEm8mydnmPuWjD5Ww0Sb2K', 'ADMIN', 1),
('manager', '$2a$10$WpmOaDE8COuLZR5u195q5ewMhSMg4o5qjTmaW.B0HEhAcrE6XeGQG', 'SALES_MANAGER', 1),
('employee', '$2a$10$m1r2gXS.t5kjGtAAshaFr.9vcDAjN3bUVXaysx91SbxjiF3PN12oO', 'EMPLOYEE', 1);
