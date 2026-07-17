package com.forecastpro.dto;

import java.math.BigDecimal;

/** One product × calendar year total (not monthly). */
public class YearlyProductSalesRow {

    private final Long productId;
    private final String productName;
    private final int year;
    private final long quantitySold;
    private final BigDecimal revenue;

    public YearlyProductSalesRow(Long productId, String productName, int year,
                                 long quantitySold, BigDecimal revenue) {
        this.productId = productId;
        this.productName = productName;
        this.year = year;
        this.quantitySold = quantitySold;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getYear() {
        return year;
    }

    public long getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
