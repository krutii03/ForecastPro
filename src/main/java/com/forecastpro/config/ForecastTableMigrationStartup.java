package com.forecastpro.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ensures {@code forecasts.predicted_sales} and {@code forecasts.predicted_revenue} exist.
 * Migrates away from legacy {@code linear_trend_value} (dropped after recalculating ensemble).
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
public class ForecastTableMigrationStartup {

    private static final Logger LOG = Logger.getLogger(ForecastTableMigrationStartup.class.getName());

    @Resource(lookup = "jdbc/ForecastProDS")
    private DataSource dataSource;

    @PostConstruct
    public void ensureForecastColumns() {
        try (Connection c = dataSource.getConnection()) {
            if (!columnExists(c, "predicted_sales")) {
                execute(c, "ALTER TABLE forecasts ADD COLUMN predicted_sales DECIMAL(18,4) NOT NULL DEFAULT 0 "
                        + "AFTER ml_regression_value");
                LOG.info("ForecastPro: added column forecasts.predicted_sales");
            }
            if (!columnExists(c, "predicted_revenue")) {
                execute(c, "ALTER TABLE forecasts ADD COLUMN predicted_revenue DECIMAL(18,4) NOT NULL DEFAULT 0 "
                        + "AFTER predicted_sales");
                LOG.info("ForecastPro: added column forecasts.predicted_revenue");
            }
            if (!columnExists(c, "lower_bound")) {
                execute(c, "ALTER TABLE forecasts ADD COLUMN lower_bound DECIMAL(18,4) NOT NULL DEFAULT 0 "
                        + "AFTER forecast_month");
                LOG.info("ForecastPro: added column forecasts.lower_bound");
            }
            if (!columnExists(c, "upper_bound")) {
                execute(c, "ALTER TABLE forecasts ADD COLUMN upper_bound DECIMAL(18,4) NOT NULL DEFAULT 0 "
                        + "AFTER lower_bound");
                LOG.info("ForecastPro: added column forecasts.upper_bound");
            }
            migrateDropLinearTrendIfPresent(c);
            if (columnExists(c, "predicted_sales") && columnExists(c, "predicted_revenue")) {
                int updated = backfillPredictedSalesAndRevenue(c);
                if (updated > 0) {
                    LOG.info(() -> "ForecastPro: backfilled predicted_sales/predicted_revenue for " + updated
                            + " forecast row(s) (legacy rows or DEFAULT 0).");
                }
            }
            if (columnExists(c, "lower_bound") && columnExists(c, "upper_bound")) {
                int bounds = backfillConfidenceBounds(c);
                if (bounds > 0) {
                    LOG.info(() -> "ForecastPro: backfilled lower_bound/upper_bound for " + bounds + " row(s).");
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE,
                    "ForecastPro: could not migrate forecast table. Run SQL under db/ on your MySQL database, then redeploy.",
                    e);
        }
    }

    /**
     * Legacy schema: drop {@code linear_trend_value} and align {@code predicted_*} with MA + regression / 2.
     */
    private static void migrateDropLinearTrendIfPresent(Connection c) throws SQLException {
        if (!columnExists(c, "linear_trend_value")) {
            return;
        }
        execute(c, "UPDATE forecasts f INNER JOIN products p ON p.id = f.product_id "
                + "SET f.predicted_sales = ROUND((f.moving_avg_value + f.ml_regression_value) / 2, 4), "
                + "    f.predicted_revenue = ROUND((f.moving_avg_value + f.ml_regression_value) / 2 * p.price, 4)");
        execute(c, "ALTER TABLE forecasts DROP COLUMN linear_trend_value");
        LOG.info("ForecastPro: dropped forecasts.linear_trend_value; predicted_* recalculated (MA + regression) / 2");
    }

    /**
     * Recompute ensemble sales and revenue from stored demand components (same formula as {@code ForecastService}).
     * Fixes rows that were DEFAULT 0 after ALTER TABLE.
     */
    private static int backfillPredictedSalesAndRevenue(Connection c) throws SQLException {
        final String sql = "UPDATE forecasts f "
                + "INNER JOIN products p ON p.id = f.product_id "
                + "SET f.predicted_sales = ROUND((f.moving_avg_value + f.ml_regression_value) / 2, 4), "
                + "    f.predicted_revenue = ROUND((f.moving_avg_value + f.ml_regression_value) / 2 * p.price, 4) "
                + "WHERE f.predicted_sales = 0 AND f.predicted_revenue = 0 "
                + "  AND (f.moving_avg_value + f.ml_regression_value) <> 0";
        try (Statement s = c.createStatement()) {
            return s.executeUpdate(sql);
        }
    }

    /** Set bounds to predicted ± 10% when zero (approximation until next forecast regeneration). */
    private static int backfillConfidenceBounds(Connection c) throws SQLException {
        final String sql = "UPDATE forecasts SET "
                + "lower_bound = ROUND(GREATEST(predicted_sales * 0.9, 0), 2), "
                + "upper_bound = ROUND(predicted_sales * 1.1, 2) "
                + "WHERE (lower_bound = 0 AND upper_bound = 0) AND predicted_sales > 0";
        try (Statement s = c.createStatement()) {
            return s.executeUpdate(sql);
        }
    }

    private static boolean columnExists(Connection c, String columnName) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forecasts' AND COLUMN_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void execute(Connection c, String ddl) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(ddl);
        }
    }
}
