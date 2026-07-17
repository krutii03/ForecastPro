package com.forecastpro.controller;

import com.forecastpro.config.AccessDeniedException;
import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.StockRequest;
import com.forecastpro.entity.StockRequestStatus;
import com.forecastpro.entity.UserRole;
import com.forecastpro.service.StockRequestService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJBException;
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

@Named("vendorRequestsBean")
@ViewScoped
public class VendorRequestsBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(VendorRequestsBean.class.getName());

    @Inject
    private StockRequestService stockRequestService;

    @Inject
    private UserSessionBean userSession;

    private List<StockRequest> requests = new ArrayList<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        if (userSession.getCurrentUser() == null || userSession.getRole() != UserRole.VENDOR) {
            requests = new ArrayList<>();
            return;
        }
        try {
            requests = stockRequestService.findByVendorUserId(userSession.getRole(), userSession.getCurrentUser().getId());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Failed to load vendor stock requests", e);
            requests = new ArrayList<>();
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc != null) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Could not load requests. Check that your vendor account is linked (vendors.user_id) and redeploy.",
                        null));
            }
        }
    }

    public void updateStatus(StockRequest r, String status) {
        if (r == null || r.getId() == null) {
            return;
        }
        try {
            StockRequestStatus next = StockRequestStatus.valueOf(status);
            stockRequestService.transitionStatus(userSession.getRole(), userSession.getCurrentUser().getId(),
                    r.getId(), next);
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Updated.", null));
        } catch (IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid status.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (AccessDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (EJBException e) {
            Throwable c = e.getCause();
            while (c != null) {
                if (c instanceof BusinessException be) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, be.getMessage(), null));
                    return;
                }
                if (c instanceof AccessDeniedException ad) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, ad.getMessage(), null));
                    return;
                }
                c = c.getCause();
            }
            throw e;
        }
    }

    public String statusStyleClass(StockRequestStatus s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case PENDING -> "status-pending";
            case APPROVED -> "status-approved";
            case READY_PACKED -> "status-ready";
            case OUT_FOR_DELIVERY -> "status-out-delivery";
            case DELIVERED -> "status-delivered";
            case COMPLETED -> "status-received";
            case RECEIVED -> "status-received"; // legacy
            case READY -> "status-ready"; // legacy
            case REJECTED -> "status-rejected"; // legacy
        };
    }

    public List<StockRequest> getRequests() {
        return requests;
    }
}
