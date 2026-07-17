package com.forecastpro.controller;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.User;
import com.forecastpro.entity.UserRole;
import com.forecastpro.service.AuthService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("loginBean")
@ViewScoped
public class LoginBean implements Serializable {

    @Inject
    private AuthService authService;

    @Inject
    private UserSessionBean userSession;

    private String username;
    private String password;

    public String login() {
        try {
            User u = authService.authenticate(username, password);
            userSession.setCurrentUser(u);
            if (u.getRole() == UserRole.VENDOR) {
                return "/vendor/dashboard.xhtml?faces-redirect=true";
            }
            return "/app/dashboard.xhtml?faces-redirect=true";
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
