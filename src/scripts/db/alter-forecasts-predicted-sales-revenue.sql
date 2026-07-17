-- Run once on existing ForecastPro MySQL databases if columns are missing.
-- (The app also adds these automatically on startup via ForecastTableMigrationStartup.)
USE forecastpro;

ALTER TABLE forecasts
    ADD COLUMN predicted_sales DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER ml_regression_value;

ALTER TABLE forecasts
    ADD COLUMN predicted_revenue DECIMAL(18,4) NOT NULL DEFAULT 0 AFTER predicted_sales;
