package com.forecastpro.controller;

import com.forecastpro.dto.InventoryAlertRow;
import com.forecastpro.service.AlertService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("alertBean")
@ViewScoped
public class AlertBean implements Serializable {

    @Inject
    private AlertService alertService;

    @Inject
    private UserSessionBean userSession;

    private List<InventoryAlertRow> alerts = new ArrayList<>();

    @PostConstruct
    public void init() {
        alerts = alertService.inventoryAlerts(userSession.getRole());
    }

    public void refresh() {
        init();
    }

    /** Legacy /bookmarks to /app/alerts/ — send users to Inventory (alerts live there). */
    public String goToInventory() {
        return "/app/inventory/inventory.xhtml?faces-redirect=true";
    }

    public List<InventoryAlertRow> getAlerts() {
        return alerts;
    }
}
