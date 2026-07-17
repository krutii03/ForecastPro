package com.forecastpro.controller;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Category;
import com.forecastpro.service.CategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("categoryBean")
@ViewScoped
public class CategoryBean implements Serializable {

    @Inject
    private CategoryService categoryService;

    @Inject
    private UserSessionBean userSession;

    private List<Category> categories;
    private String newName;
    private Category editing;
    private String editName;

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        categories = categoryService.listAll(userSession.getRole());
    }

    public void create() {
        try {
            categoryService.create(userSession.getRole(), newName);
            newName = null;
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Category saved.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public void prepareEdit(Category c) {
        editing = c;
        editName = c.getName();
    }

    public void saveEdit() {
        try {
            categoryService.update(userSession.getRole(), editing.getId(), editName);
            editing = null;
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Category updated.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public void delete(Category c) {
        try {
            categoryService.delete(userSession.getRole(), c.getId());
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Category deleted.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public List<Category> getCategories() {
        return categories;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public Category getEditing() {
        return editing;
    }

    public void setEditing(Category editing) {
        this.editing = editing;
    }

    public String getEditName() {
        return editName;
    }

    public void setEditName(String editName) {
        this.editName = editName;
    }
}
