package com.forecastpro.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.Serializable;

/**
 * Redirects legacy {@code /app/forecast/forecast.xhtml} to the demand forecast page.
 */
@Named("forecastRedirectBean")
@RequestScoped
public class ForecastRedirectBean implements Serializable {

    public void redirectToDemand() throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.redirect(ec.getRequestContextPath() + "/app/forecast/demand.xhtml");
        fc.responseComplete();
    }
}
