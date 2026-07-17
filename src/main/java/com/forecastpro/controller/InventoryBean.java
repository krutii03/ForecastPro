package com.forecastpro.controller;

import com.forecastpro.dto.InventoryRecommendationRow;
import com.forecastpro.service.AlertService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("inventoryBean")
@ViewScoped
public class InventoryBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(InventoryBean.class.getName());

    @Inject
    private AlertService alertService;

    @Inject
    private UserSessionBean userSession;

    private List<InventoryRecommendationRow> inventoryRecommendations = new ArrayList<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        inventoryRecommendations = new ArrayList<>();
        if (userSession.isAdminOrManager()) {
            try {
                inventoryRecommendations = alertService.inventoryRecommendations(userSession.getRole());
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Inventory alerts load failed", e);
                FacesContext fc = FacesContext.getCurrentInstance();
                if (fc != null) {
                    fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Could not load inventory alerts. See server logs.", null));
                }
            }
        }
    }

    public List<InventoryRecommendationRow> getInventoryRecommendations() {
        return inventoryRecommendations;
    }
}
