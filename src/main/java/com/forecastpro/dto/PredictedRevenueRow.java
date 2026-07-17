package com.forecastpro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Latest forecast revenue for a product (analytics). */
public class PredictedRevenueRow {

    private final String productName;
    private final BigDecimal predictedRevenue;
    private final LocalDate predictionMonth;

    public PredictedRevenueRow(String productName, BigDecimal predictedRevenue, LocalDate predictionMonth) {
        this.productName = productName;
        this.predictedRevenue = predictedRevenue != null ? predictedRevenue : BigDecimal.ZERO;
        this.predictionMonth = predictionMonth;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getPredictedRevenue() {
        return predictedRevenue;
    }

    public LocalDate getPredictionMonth() {
        return predictionMonth;
    }
}
