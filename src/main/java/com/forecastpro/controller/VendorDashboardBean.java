package com.forecastpro.controller;

import com.forecastpro.entity.UserRole;
import com.forecastpro.service.StockRequestService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("vendorDashboardBean")
@ViewScoped
public class VendorDashboardBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(VendorDashboardBean.class.getName());

    @Inject
    private StockRequestService stockRequestService;

    @Inject
    private UserSessionBean userSession;

    private long totalRequests;
    private long pendingRequests;
    private long activeFulfillment;
    private long awaitingWarehouseReceipt;
    private long receivedRequests;

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        if (userSession.getCurrentUser() == null || userSession.getRole() != UserRole.VENDOR) {
            totalRequests = pendingRequests = activeFulfillment = awaitingWarehouseReceipt = receivedRequests = 0;
            return;
        }
        try {
            StockRequestService.VendorStockRequestSummary s = stockRequestService.summarizeVendorRequests(
                    userSession.getRole(), userSession.getCurrentUser().getId());
            totalRequests = s.total();
            pendingRequests = s.pending();
            activeFulfillment = s.activeFulfillment();
            awaitingWarehouseReceipt = s.awaitingWarehouseReceipt();
            receivedRequests = s.received();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Vendor dashboard stats failed", e);
            totalRequests = pendingRequests = activeFulfillment = awaitingWarehouseReceipt = receivedRequests = 0;
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc != null) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Could not load dashboard stats.", null));
            }
        }
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public long getPendingRequests() {
        return pendingRequests;
    }

    public long getActiveFulfillment() {
        return activeFulfillment;
    }

    public long getAwaitingWarehouseReceipt() {
        return awaitingWarehouseReceipt;
    }

    public long getReceivedRequests() {
        return receivedRequests;
    }
}
