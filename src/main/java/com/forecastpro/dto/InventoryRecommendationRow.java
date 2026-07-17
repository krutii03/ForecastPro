package com.forecastpro.dto;

import java.math.BigDecimal;

/**
 * Inventory recommendation for one product: stock vs latest forecast with status badges.
 */
public class InventoryRecommendationRow {

    private Long productId;
    private Long categoryId;
    private String productName;
    private String categoryName;
    private int stockQuantity;
    private BigDecimal forecastDemand;
    private BigDecimal recommendedPurchase;
    /** Healthy | Restock Required | Critical */
    private String stockStatus;
    /** Urgent Restock | Reduce Inventory | Stock Level Healthy */
    private String smartRecommendation;
    private String statusBadgeClass;
    private String smartBadgeClass;
    private int minimumThreshold;
    private boolean alert;

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

    public BigDecimal getRecommendedPurchase() {
        return recommendedPurchase;
    }

    public void setRecommendedPurchase(BigDecimal recommendedPurchase) {
        this.recommendedPurchase = recommendedPurchase;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public String getSmartRecommendation() {
        return smartRecommendation;
    }

    public void setSmartRecommendation(String smartRecommendation) {
        this.smartRecommendation = smartRecommendation;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public void setStatusBadgeClass(String statusBadgeClass) {
        this.statusBadgeClass = statusBadgeClass;
    }

    public String getSmartBadgeClass() {
        return smartBadgeClass;
    }

    public void setSmartBadgeClass(String smartBadgeClass) {
        this.smartBadgeClass = smartBadgeClass;
    }

    public int getMinimumThreshold() {
        return minimumThreshold;
    }

    public void setMinimumThreshold(int minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

    public boolean isAlert() {
        return alert;
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }

    /** Whole units for stock-request prefill (minimum 1 when restock needed). */
    public int getRecommendedRestockUnits() {
        if (recommendedPurchase == null || recommendedPurchase.signum() <= 0) {
            return 1;
        }
        return Math.max(1, recommendedPurchase.intValue());
    }
}
