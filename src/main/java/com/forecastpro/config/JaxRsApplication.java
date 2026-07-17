package com.forecastpro.config;

import com.forecastpro.rest.ForecastResource;
import com.forecastpro.rest.ProductResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

@ApplicationPath("/api")
public class JaxRsApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(ForecastResource.class, ProductResource.class);
    }
}
