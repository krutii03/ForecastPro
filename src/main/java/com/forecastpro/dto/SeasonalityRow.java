package com.forecastpro.dto;

import java.math.BigDecimal;

/**
 * Seasonality analysis per product: strongest/weakest calendar months by average units sold.
 */
public class SeasonalityRow {

    private String productName;
    private String bestMonth;
    private String worstMonth;
    private BigDecimal averageMonthlySales;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBestMonth() {
        return bestMonth;
    }

    public void setBestMonth(String bestMonth) {
        this.bestMonth = bestMonth;
    }

    public String getWorstMonth() {
        return worstMonth;
    }

    public void setWorstMonth(String worstMonth) {
        this.worstMonth = worstMonth;
    }

    public BigDecimal getAverageMonthlySales() {
        return averageMonthlySales;
    }

    public void setAverageMonthlySales(BigDecimal averageMonthlySales) {
        this.averageMonthlySales = averageMonthlySales;
    }
}
