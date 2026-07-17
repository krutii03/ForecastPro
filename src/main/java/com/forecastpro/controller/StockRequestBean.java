package com.forecastpro.controller;

import com.forecastpro.config.AccessDeniedException;
import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.StockRequest;
import com.forecastpro.entity.StockRequestStatus;
import com.forecastpro.entity.UserRole;
import com.forecastpro.entity.Vendor;
import com.forecastpro.service.CategoryService;
import com.forecastpro.service.ProductService;
import com.forecastpro.service.StockRequestService;
import com.forecastpro.service.VendorService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Named("stockRequestBean")
@ViewScoped
public class StockRequestBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(StockRequestBean.class.getName());

    /** Category id → specialist vendor id (seed data: 1=Furniture, 2=Appliances, 3=Electronics). */
    private static final Map<Long, Long> VENDOR_ID_BY_CATEGORY = Map.of(
            1L, 1L,
            2L, 2L,
            3L, 3L
    );

    @Inject
    private StockRequestService stockRequestService;

    @EJB
    private ProductService productService;

    @EJB
    private CategoryService categoryService;

    @Inject
    private VendorService vendorService;

    @Inject
    private UserSessionBean userSession;

    private List<StockRequest> requests = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<Vendor> vendors = new ArrayList<>();

    private Long selectedCategoryId;
    private Long formProductId;
    private Long formVendorId;
    private int formQuantity = 1;

    @PostConstruct
    public void init() {
        categories = loadCategoriesSafe();
        products = new ArrayList<>();
        vendors = loadVendorsSafe();
        loadRequestsSafe();
        applyPrefillFromQueryParameters();
    }

    /**
     * Deep link from inventory alerts: {@code ?categoryId=&productId=&quantity=}.
     */
    private void applyPrefillFromQueryParameters() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return;
        }
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        String categoryIdStr = params.get("categoryId");
        if (categoryIdStr == null || categoryIdStr.isBlank()) {
            return;
        }
        try {
            selectedCategoryId = Long.parseLong(categoryIdStr);
            products = loadProductsSafe();
            String productIdStr = params.get("productId");
            if (productIdStr != null && !productIdStr.isBlank()) {
                formProductId = Long.parseLong(productIdStr);
            }
            String quantityStr = params.get("quantity");
            if (quantityStr != null && !quantityStr.isBlank()) {
                formQuantity = Math.max(1, Integer.parseInt(quantityStr));
            }
            applyVendorForCategory();
        } catch (NumberFormatException e) {
            LOG.log(Level.FINE, "Ignoring invalid stock request prefill query params", e);
        }
    }

    public void refresh() {
        categories = loadCategoriesSafe();
        vendors = loadVendorsSafe();
        if (selectedCategoryId != null) {
            products = loadProductsSafe();
        } else {
            products = new ArrayList<>();
            formProductId = null;
        }
        loadRequestsSafe();
    }

    private void loadRequestsSafe() {
        if (userSession.getCurrentUser() == null) {
            requests = new ArrayList<>();
            return;
        }
        UserRole role = userSession.getRole();
        try {
            if (role == UserRole.VENDOR) {
                requests = stockRequestService.findByVendorUserId(role, userSession.getCurrentUser().getId());
            } else {
                requests = stockRequestService.findAll(role);
            }
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Stock requests list failed", e);
            requests = new ArrayList<>();
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc != null) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Could not load stock requests. See server logs.", null));
            }
        }
    }

    private List<Category> loadCategoriesSafe() {
        try {
            if (userSession.getCurrentUser() == null) {
                return new ArrayList<>();
            }
            return categoryService.findAllForUi(userSession.getRole());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Categories load failed", e);
            return new ArrayList<>();
        }
    }

    private List<Vendor> loadVendorsSafe() {
        try {
            if (userSession.getCurrentUser() == null) {
                return new ArrayList<>();
            }
            return vendorService.listForUi(userSession.getRole());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Vendors load failed", e);
            return new ArrayList<>();
        }
    }

    private List<Product> loadProductsSafe() {
        try {
            if (userSession.getCurrentUser() == null || selectedCategoryId == null) {
                return new ArrayList<>();
            }
            return productService.findByCategory(userSession.getRole(), selectedCategoryId);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Products load failed", e);
            return new ArrayList<>();
        }
    }

    public void onCategoryChange() {
        formProductId = null;
        if (selectedCategoryId == null) {
            products = new ArrayList<>();
            formVendorId = null;
        } else {
            products = loadProductsSafe();
            applyVendorForCategory();
        }
    }

    private void applyVendorForCategory() {
        if (selectedCategoryId == null) {
            formVendorId = null;
            return;
        }
        Long vendorId = VENDOR_ID_BY_CATEGORY.get(selectedCategoryId);
        if (vendorId != null && vendors.stream().anyMatch(v -> vendorId.equals(v.getId()))) {
            formVendorId = vendorId;
        }
    }

    /** Vendors that supply the selected category (specialist + general fallback). */
    public List<Vendor> getVendorsForForm() {
        if (selectedCategoryId == null) {
            return vendors;
        }
        Long specialistId = VENDOR_ID_BY_CATEGORY.get(selectedCategoryId);
        if (specialistId == null) {
            return vendors;
        }
        return vendors.stream()
                .filter(v -> specialistId.equals(v.getId()) || Long.valueOf(4L).equals(v.getId()))
                .collect(Collectors.toList());
    }

    public void create() {
        try {
            stockRequestService.create(userSession.getRole(), userSession.getCurrentUser().getId(),
                    formProductId, formVendorId, formQuantity);
            formProductId = null;
            formVendorId = null;
            formQuantity = 1;
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Request created.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    /**
     * Vendor status transitions (enforced in service). {@code status} must match enum name.
     */
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

    /** Admin: DELIVERED → COMPLETED (add delivered quantity into stock). */
    public void addToStock(StockRequest r) {
        if (r == null || r.getId() == null) {
            return;
        }
        try {
            stockRequestService.addToInventory(userSession.getRole(), userSession.getCurrentUser().getId(), r.getId());
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Stock successfully updated.", null));
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

    public List<Category> getCategories() {
        return categories;
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<Vendor> getVendors() {
        return vendors;
    }

    public Long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Long selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public Long getFormProductId() {
        return formProductId;
    }

    public void setFormProductId(Long formProductId) {
        this.formProductId = formProductId;
    }

    public Long getFormVendorId() {
        return formVendorId;
    }

    public void setFormVendorId(Long formVendorId) {
        this.formVendorId = formVendorId;
    }

    public int getFormQuantity() {
        return formQuantity;
    }

    public void setFormQuantity(int formQuantity) {
        this.formQuantity = formQuantity;
    }
}
