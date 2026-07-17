package com.forecastpro.controller;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.Product;
import com.forecastpro.service.CategoryService;
import com.forecastpro.service.ProductService;
import com.forecastpro.service.SaleService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Named("saleBean")
@ViewScoped
public class SaleBean implements Serializable {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Inject
    private SaleService saleService;

    @Inject
    private ProductService productService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private UserSessionBean userSession;

    private List<Category> categories = new ArrayList<>();
    private Long selectedCategoryId;
    private List<Product> products = new ArrayList<>();
    private Long selectedProductId;
    private int quantitySold = 1;
    private LocalDate saleDate = LocalDate.now(BUSINESS_ZONE);

    @PostConstruct
    public void init() {
        loadCategories();
    }

    public void loadCategories() {
        categories = new ArrayList<>(categoryService.findAllForUi(userSession.getRole()));
    }

    public void loadProducts() {
        products = new ArrayList<>();
        selectedProductId = null;
        if (selectedCategoryId == null) {
            return;
        }
        products = productService.listByCategory(userSession.getRole(), selectedCategoryId);
    }

    public void recordSale() {
        try {
            if (selectedCategoryId == null) {
                throw new BusinessException("Select a category first.");
            }
            if (selectedProductId == null) {
                throw new BusinessException("Select a product.");
            }
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            if (saleDate != null && saleDate.isAfter(today)) {
                throw new BusinessException("Sale date cannot be in the future.");
            }
            saleService.recordSale(userSession.getRole(), selectedProductId, quantitySold, saleDate);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sale recorded.", null));
            quantitySold = 1;
            saleDate = LocalDate.now(BUSINESS_ZONE);
            loadProducts();
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Long selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public List<Product> getProducts() {
        return products;
    }

    public Long getSelectedProductId() {
        return selectedProductId;
    }

    public void setSelectedProductId(Long selectedProductId) {
        this.selectedProductId = selectedProductId;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    /** Latest selectable sale date (today); used by datePicker maxDate. */
    public LocalDate getMaxSaleDate() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
