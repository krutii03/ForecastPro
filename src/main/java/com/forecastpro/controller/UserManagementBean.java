package com.forecastpro.controller;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.User;
import com.forecastpro.entity.UserRole;
import com.forecastpro.service.SecurityService;
import com.forecastpro.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Named("userManagementBean")
@ViewScoped
public class UserManagementBean implements Serializable {

    @Inject
    private UserService userService;

    @Inject
    private UserSessionBean userSession;

    private List<User> users;
    private String newUsername;
    private String newPassword;
    private UserRole newRole = UserRole.EMPLOYEE;
    private boolean newEnabled = true;

    private User editingUser;
    private UserRole editRole;
    private boolean editEnabled;

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        users = userService.listUsers(userSession.getRole());
    }

    public List<UserRole> getAllRoles() {
        return Arrays.asList(UserRole.values());
    }

    public void createUser() {
        try {
            userService.createUser(userSession.getRole(), newUsername, newPassword, newRole, newEnabled);
            newUsername = null;
            newPassword = null;
            newRole = UserRole.EMPLOYEE;
            newEnabled = true;
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "User created.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public void prepareEdit(User u) {
        editingUser = u;
        editRole = u.getRole();
        editEnabled = u.isEnabled();
    }

    public void saveEdit() {
        try {
            userService.updateUser(userSession.getRole(), editingUser.getId(), editRole, editEnabled);
            editingUser = null;
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "User updated.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public List<User> getUsers() {
        return users;
    }

    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public UserRole getNewRole() {
        return newRole;
    }

    public void setNewRole(UserRole newRole) {
        this.newRole = newRole;
    }

    public boolean isNewEnabled() {
        return newEnabled;
    }

    public void setNewEnabled(boolean newEnabled) {
        this.newEnabled = newEnabled;
    }

    public User getEditingUser() {
        return editingUser;
    }

    public void setEditingUser(User editingUser) {
        this.editingUser = editingUser;
    }

    public UserRole getEditRole() {
        return editRole;
    }

    public void setEditRole(UserRole editRole) {
        this.editRole = editRole;
    }

    public boolean isEditEnabled() {
        return editEnabled;
    }

    public void setEditEnabled(boolean editEnabled) {
        this.editEnabled = editEnabled;
    }
}
