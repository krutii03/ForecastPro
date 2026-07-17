package com.forecastpro.dto;

import java.math.BigDecimal;

public class ProductSalesRow {

    private Long productId;
    private String productName;
    private BigDecimal revenue;
    private long quantitySold;

    public ProductSalesRow() {
    }

    public ProductSalesRow(Long productId, String productName, BigDecimal revenue, long quantitySold) {
        this.productId = productId;
        this.productName = productName;
        this.revenue = revenue;
        this.quantitySold = quantitySold;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public long getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(long quantitySold) {
        this.quantitySold = quantitySold;
    }
}
