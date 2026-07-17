-- Optional: backfill predicted_sales / predicted_revenue for rows that were 0 after adding columns.
-- The app runs this automatically on startup (ForecastTableMigrationStartup) when both values are 0
-- but demand columns are non-zero.
USE forecastpro;

UPDATE forecasts f
INNER JOIN products p ON p.id = f.product_id
SET f.predicted_sales = ROUND((f.moving_avg_value + f.ml_regression_value) / 2, 4),
    f.predicted_revenue = ROUND((f.moving_avg_value + f.ml_regression_value) / 2 * p.price, 4)
WHERE f.predicted_sales = 0
  AND f.predicted_revenue = 0
  AND (f.moving_avg_value + f.ml_regression_value) <> 0;
