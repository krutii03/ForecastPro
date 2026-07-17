package com.forecastpro.controller;

import com.forecastpro.dto.MonthlyProductSalesRow;
import com.forecastpro.dto.YearlyProductSalesRow;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.Product;
import com.forecastpro.service.CategoryService;
import com.forecastpro.service.ProductService;
import com.forecastpro.service.SalesReportService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Named("salesReportBean")
@ViewScoped
public class SalesReportBean implements Serializable {

    @Inject
    private SalesReportService salesReportService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private ProductService productService;

    @Inject
    private UserSessionBean userSession;

    private List<Category> categories = new ArrayList<>();
    private Long selectedCategoryId;
    private List<Product> products = new ArrayList<>();
    private Long selectedProductId;

    private List<MonthlyProductSalesRow> monthlyRows = new ArrayList<>();
    private List<YearlyProductSalesRow> yearlyRows = new ArrayList<>();

    /** Calendar year for the "Annual" tab (default: previous year). */
    private int yearForAnnual;

    @PostConstruct
    public void init() {
        yearForAnnual = LocalDate.now().getYear() - 1;
        categories = new ArrayList<>(categoryService.findAllForUi(userSession.getRole()));
        products = new ArrayList<>();
        refresh();
    }

    public void onCategoryChange() {
        selectedProductId = null;
        products = new ArrayList<>();
        if (selectedCategoryId != null) {
            products = new ArrayList<>(productService.listByCategory(userSession.getRole(), selectedCategoryId));
        }
        refresh();
    }

    public void refresh() {
        monthlyRows = salesReportService.monthlySales(userSession.getRole(), selectedCategoryId, selectedProductId);
        yearlyRows = salesReportService.yearlyTotals(userSession.getRole(), selectedCategoryId, selectedProductId,
                yearForAnnual);
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

    public List<MonthlyProductSalesRow> getMonthlyRows() {
        return monthlyRows;
    }

    public List<YearlyProductSalesRow> getYearlyRows() {
        return yearlyRows;
    }

    public int getYearForAnnual() {
        return yearForAnnual;
    }

    public void setYearForAnnual(int yearForAnnual) {
        this.yearForAnnual = yearForAnnual;
    }

    /** Shown above the monthly table (rolling 12 months). */
    public String getMonthlyRangeDescription() {
        LocalDate to = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        LocalDate from = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return from.format(f) + " — " + to.format(f);
    }
}
