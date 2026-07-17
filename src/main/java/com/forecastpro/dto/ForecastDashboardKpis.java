package com.forecastpro.dto;

import java.math.BigDecimal;

/**
 * KPI cards shared across Demand Forecast, Sales Forecast, and Reports pages.
 */
public class ForecastDashboardKpis {

    private BigDecimal forecastedRevenue = BigDecimal.ZERO;
    private String highestPredictedProduct = "—";
    private String lowestPredictedProduct = "—";
    private BigDecimal forecastGrowthPercent = BigDecimal.ZERO;
    private long totalPredictedUnits;

    public BigDecimal getForecastedRevenue() {
        return forecastedRevenue;
    }

    public void setForecastedRevenue(BigDecimal forecastedRevenue) {
        this.forecastedRevenue = forecastedRevenue;
    }

    public String getHighestPredictedProduct() {
        return highestPredictedProduct;
    }

    public void setHighestPredictedProduct(String highestPredictedProduct) {
        this.highestPredictedProduct = highestPredictedProduct;
    }

    public String getLowestPredictedProduct() {
        return lowestPredictedProduct;
    }

    public void setLowestPredictedProduct(String lowestPredictedProduct) {
        this.lowestPredictedProduct = lowestPredictedProduct;
    }

    public BigDecimal getForecastGrowthPercent() {
        return forecastGrowthPercent;
    }

    public void setForecastGrowthPercent(BigDecimal forecastGrowthPercent) {
        this.forecastGrowthPercent = forecastGrowthPercent;
    }

    public long getTotalPredictedUnits() {
        return totalPredictedUnits;
    }

    public void setTotalPredictedUnits(long totalPredictedUnits) {
        this.totalPredictedUnits = totalPredictedUnits;
    }
}
