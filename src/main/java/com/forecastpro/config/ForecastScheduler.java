package com.forecastpro.config;

import com.forecastpro.service.ForecastService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * Automatic monthly forecast generation on the 1st day of each month.
 * Uses (product_id, forecast_month) uniqueness — no duplicates are created.
 */
@Singleton
@Startup
public class ForecastScheduler {

    private static final Logger LOG = Logger.getLogger(ForecastScheduler.class.getName());

    @Inject
    private ForecastService forecastService;

    @PostConstruct
    public void init() {
        LOG.info("ForecastScheduler: automatic monthly forecast generation enabled (1st of month).");
    }

    @Schedule(dayOfMonth = "1", hour = "0", minute = "5", second = "0", persistent = false)
    public void generateMonthlyForecasts() {
        LOG.info("ForecastScheduler: running automatic monthly forecast generation.");
        int created = forecastService.generateForecastsForAllProductsAutomatic();
        LOG.info(() -> "ForecastScheduler: created " + created + " new forecast(s).");
    }
}
