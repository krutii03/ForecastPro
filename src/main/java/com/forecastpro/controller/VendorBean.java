package com.forecastpro.controller;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Vendor;
import com.forecastpro.service.VendorService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("vendorBean")
@ViewScoped
public class VendorBean implements Serializable {

    @Inject
    private VendorService vendorService;

    @Inject
    private UserSessionBean userSession;

    private List<Vendor> vendors = new ArrayList<>();
    private Long editId;
    private String formName;
    private String formEmail;
    private String formPhone;
    private String formAddress;
    private boolean formActive = true;

    @PostConstruct
    public void init() {
        vendors = new ArrayList<>();
        if (userSession.getCurrentUser() != null) {
            vendors = vendorService.findAll(userSession.getRole());
        }
        System.out.println("Vendors size: " + vendors.size());
        if (userSession.getCurrentUser() != null && userSession.isAdmin()) {
            prepareNew();
        }
    }

    public void loadVendors() {
        if (userSession.getCurrentUser() == null) {
            vendors = new ArrayList<>();
            System.out.println("Vendors size: " + vendors.size());
            return;
        }
        vendors = vendorService.findAll(userSession.getRole());
        System.out.println("Vendors size: " + vendors.size());
    }

    public void refresh() {
        loadVendors();
    }

    public void prepareNew() {
        editId = null;
        formName = null;
        formEmail = null;
        formPhone = null;
        formAddress = null;
        formActive = true;
    }

    public void prepareEdit(Vendor v) {
        editId = v.getId();
        formName = v.getName();
        formEmail = v.getContactEmail();
        formPhone = v.getPhone();
        formAddress = v.getAddress();
        formActive = v.isActive();
    }

    public void save() {
        try {
            vendorService.save(userSession.getRole(), editId, formName, formEmail, formPhone, formAddress, formActive);
            prepareNew();
            loadVendors();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Vendor saved.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public void delete(Vendor v) {
        try {
            vendorService.delete(userSession.getRole(), v.getId());
            loadVendors();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Vendor removed.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public List<Vendor> getVendors() {
        return vendors;
    }

    public Long getEditId() {
        return editId;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getFormEmail() {
        return formEmail;
    }

    public void setFormEmail(String formEmail) {
        this.formEmail = formEmail;
    }

    public String getFormPhone() {
        return formPhone;
    }

    public void setFormPhone(String formPhone) {
        this.formPhone = formPhone;
    }

    public String getFormAddress() {
        return formAddress;
    }

    public void setFormAddress(String formAddress) {
        this.formAddress = formAddress;
    }

    public boolean isFormActive() {
        return formActive;
    }

    public void setFormActive(boolean formActive) {
        this.formActive = formActive;
    }
}
