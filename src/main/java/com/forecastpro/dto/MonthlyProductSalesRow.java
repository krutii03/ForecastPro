package com.forecastpro.dto;

import com.forecastpro.util.DisplayFormats;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One product × calendar month aggregate. */
public class MonthlyProductSalesRow {

    private final Long productId;
    private final String productName;
    private final int year;
    private final int month;
    private final long quantitySold;
    private final BigDecimal revenue;

    public MonthlyProductSalesRow(Long productId, String productName, int year, int month,
                                  long quantitySold, BigDecimal revenue) {
        this.productId = productId;
        this.productName = productName;
        this.year = year;
        this.month = month;
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

    public int getMonth() {
        return month;
    }

    public String getMonthLabel() {
        return DisplayFormats.formatMonthBucket(year, month);
    }

    public long getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
