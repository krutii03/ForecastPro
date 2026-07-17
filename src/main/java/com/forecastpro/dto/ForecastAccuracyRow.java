package com.forecastpro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row in the Forecast Accuracy Report: predicted vs actual units for a completed month.
 */
public class ForecastAccuracyRow {

    private String productName;
    private LocalDate forecastMonth;
    private BigDecimal predictedUnits;
    private BigDecimal actualUnits;
    private BigDecimal absoluteError;
    private BigDecimal accuracyPercent;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public LocalDate getForecastMonth() {
        return forecastMonth;
    }

    public void setForecastMonth(LocalDate forecastMonth) {
        this.forecastMonth = forecastMonth;
    }

    public BigDecimal getPredictedUnits() {
        return predictedUnits;
    }

    public void setPredictedUnits(BigDecimal predictedUnits) {
        this.predictedUnits = predictedUnits;
    }

    public BigDecimal getActualUnits() {
        return actualUnits;
    }

    public void setActualUnits(BigDecimal actualUnits) {
        this.actualUnits = actualUnits;
    }

    public BigDecimal getAbsoluteError() {
        return absoluteError;
    }

    public void setAbsoluteError(BigDecimal absoluteError) {
        this.absoluteError = absoluteError;
    }

    public BigDecimal getAccuracyPercent() {
        return accuracyPercent;
    }

    public void setAccuracyPercent(BigDecimal accuracyPercent) {
        this.accuracyPercent = accuracyPercent;
    }
}
