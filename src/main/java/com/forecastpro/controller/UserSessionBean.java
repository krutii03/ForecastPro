package com.forecastpro.controller;

import com.forecastpro.entity.User;
import com.forecastpro.entity.UserRole;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.Serializable;

@Named("userSession")
@SessionScoped
public class UserSessionBean implements Serializable {

    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public UserRole getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    public boolean isSalesManager() {
        return currentUser != null && currentUser.getRole() == UserRole.SALES_MANAGER;
    }

    public boolean isEmployee() {
        return currentUser != null && currentUser.getRole() == UserRole.EMPLOYEE;
    }

    public boolean isVendor() {
        return currentUser != null && currentUser.getRole() == UserRole.VENDOR;
    }

    public boolean isAdminOrManager() {
        return currentUser != null
                && (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.SALES_MANAGER);
    }

    public void logout() {
        currentUser = null;
    }

    /**
     * Explicit redirect after invalidation — more reliable than faces-redirect alone with some containers
     * and PrimeFaces menu items.
     */
    public String logoutAction() {
        FacesContext fc = FacesContext.getCurrentInstance();
        currentUser = null;
        if (fc == null) {
            return "/login.xhtml?faces-redirect=true";
        }
        ExternalContext ec = fc.getExternalContext();
        try {
            ec.invalidateSession();
            ec.redirect(ec.getRequestContextPath() + "/login.xhtml");
            fc.responseComplete();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
