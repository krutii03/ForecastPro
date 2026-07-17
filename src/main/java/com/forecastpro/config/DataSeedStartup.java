package com.forecastpro.config;

import com.forecastpro.service.DataSeedService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs {@link DataSeedService#seedIfEmpty()} after the EJB container is up so JTA and JPA are available.
 */
@Singleton
@Startup
@DependsOn("ForecastTableMigrationStartup")
public class DataSeedStartup {

    private static final Logger LOG = Logger.getLogger(DataSeedStartup.class.getName());

    @Inject
    private DataSeedService dataSeedService;

    @PostConstruct
    public void init() {
        try {
            dataSeedService.seedIfEmpty();
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "ForecastPro: automatic data seed failed. Check JDBC jdbc/ForecastProDS and MySQL. You can load db/seed-all.sql manually.",
                    e);
        }
    }
}
