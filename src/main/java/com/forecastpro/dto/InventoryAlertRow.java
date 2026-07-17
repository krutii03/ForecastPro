package com.forecastpro.dto;

import java.math.BigDecimal;

public class InventoryAlertRow {

    private Long productId;
    private Long categoryId;
    private String productName;
    private String categoryName;
    private int stockQuantity;
    private BigDecimal forecastDemand;
    private BigDecimal recommendedRestock;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /** Whole units for URLs / stock request quantity (minimum 1). */
    public int getRecommendedRestockUnits() {
        if (recommendedRestock == null) {
            return 1;
        }
        return Math.max(1, recommendedRestock.intValue());
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getForecastDemand() {
        return forecastDemand;
    }

    public void setForecastDemand(BigDecimal forecastDemand) {
        this.forecastDemand = forecastDemand;
    }

    public BigDecimal getRecommendedRestock() {
        return recommendedRestock;
    }

    public void setRecommendedRestock(BigDecimal recommendedRestock) {
        this.recommendedRestock = recommendedRestock;
    }
}
