package com.forecastpro.controller;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.Product;
import com.forecastpro.service.CategoryService;
import com.forecastpro.service.ProductService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Named("productBean")
@ViewScoped
public class ProductBean implements Serializable {

    @Inject
    private ProductService productService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private UserSessionBean userSession;

    private List<Category> categories = new ArrayList<>();
    private Long selectedCategoryId;
    private List<Product> products = new ArrayList<>();

    private Long editProductId;
    private Long formCategoryId;
    private String formName;
    private BigDecimal formPrice;
    private int formStock;

    @PostConstruct
    public void init() {
        loadCategories();
    }

    /** Refresh category dropdown (call from f:viewAction after navigation). */
    public void loadCategories() {
        categories = new ArrayList<>(categoryService.findAllForUi(userSession.getRole()));
    }

    public void loadProducts() {
        products = new ArrayList<>();
        if (selectedCategoryId == null) {
            return;
        }
        products = productService.listByCategory(userSession.getRole(), selectedCategoryId);
    }

    public void prepareNew() {
        editProductId = null;
        formCategoryId = selectedCategoryId;
        formName = null;
        formPrice = BigDecimal.ZERO;
        formStock = 0;
    }

    public void prepareEdit(Product prod) {
        editProductId = prod.getId();
        formCategoryId = prod.getCategory().getId();
        selectedCategoryId = prod.getCategory().getId();
        formName = prod.getName();
        formPrice = prod.getPrice();
        formStock = prod.getStockQuantity();
    }

    public void save() {
        try {
            if (selectedCategoryId == null && editProductId == null) {
                throw new BusinessException("Select a category first.");
            }
            Long catId = formCategoryId != null ? formCategoryId : selectedCategoryId;
            productService.saveProduct(userSession.getRole(), editProductId, catId, formName, formPrice, formStock);
            loadProducts();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Product saved.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public void delete(Product p) {
        try {
            productService.delete(userSession.getRole(), p.getId());
            loadProducts();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Product deleted.", null));
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

    public Long getEditProductId() {
        return editProductId;
    }

    public void setEditProductId(Long editProductId) {
        this.editProductId = editProductId;
    }

    public Long getFormCategoryId() {
        return formCategoryId;
    }

    public void setFormCategoryId(Long formCategoryId) {
        this.formCategoryId = formCategoryId;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public BigDecimal getFormPrice() {
        return formPrice;
    }

    public void setFormPrice(BigDecimal formPrice) {
        this.formPrice = formPrice;
    }

    public int getFormStock() {
        return formStock;
    }

    public void setFormStock(int formStock) {
        this.formStock = formStock;
    }
}
