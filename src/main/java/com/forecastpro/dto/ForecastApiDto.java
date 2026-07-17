package com.forecastpro.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class ForecastApiDto {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal movingAvgValue;
    private BigDecimal mlRegressionValue;
    private BigDecimal predictedSales;
    private BigDecimal predictedRevenue;
    private String forecastMonth;
    private Instant createdAt;
    /** Same as {@link #createdAt}, formatted as dd/MM/yyyy in the server default time zone (for display). */
    private String createdAtDisplay;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getMovingAvgValue() {
        return movingAvgValue;
    }

    public void setMovingAvgValue(BigDecimal movingAvgValue) {
        this.movingAvgValue = movingAvgValue;
    }

    public BigDecimal getMlRegressionValue() {
        return mlRegressionValue;
    }

    public void setMlRegressionValue(BigDecimal mlRegressionValue) {
        this.mlRegressionValue = mlRegressionValue;
    }

    public BigDecimal getPredictedSales() {
        return predictedSales;
    }

    public void setPredictedSales(BigDecimal predictedSales) {
        this.predictedSales = predictedSales;
    }

    public BigDecimal getPredictedRevenue() {
        return predictedRevenue;
    }

    public void setPredictedRevenue(BigDecimal predictedRevenue) {
        this.predictedRevenue = predictedRevenue;
    }

    public String getForecastMonth() {
        return forecastMonth;
    }

    public void setForecastMonth(String forecastMonth) {
        this.forecastMonth = forecastMonth;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtDisplay() {
        return createdAtDisplay;
    }

    public void setCreatedAtDisplay(String createdAtDisplay) {
        this.createdAtDisplay = createdAtDisplay;
    }
}
