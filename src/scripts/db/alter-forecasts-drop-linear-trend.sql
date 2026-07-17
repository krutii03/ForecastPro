-- One-time migration for existing ForecastPro MySQL databases that still have linear_trend_value.
-- The application runs equivalent steps on startup (ForecastTableMigrationStartup); use this if you prefer manual DDL.
USE forecastpro;

UPDATE forecasts f
INNER JOIN products p ON p.id = f.product_id
SET f.predicted_sales = ROUND((f.moving_avg_value + f.ml_regression_value) / 2, 4),
    f.predicted_revenue = ROUND((f.moving_avg_value + f.ml_regression_value) / 2 * p.price, 4);

ALTER TABLE forecasts DROP COLUMN linear_trend_value;
