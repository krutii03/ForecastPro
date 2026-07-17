package com.forecastpro.dto;

import java.math.BigDecimal;

public class CategorySalesRow {

    private String categoryName;
    private BigDecimal revenue;

    public CategorySalesRow() {
    }

    public CategorySalesRow(String categoryName, BigDecimal revenue) {
        this.categoryName = categoryName;
        this.revenue = revenue;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
