package com.forecastpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "forecasts")
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "moving_avg_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal movingAvgValue;

    @Column(name = "ml_regression_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal mlRegressionValue;

    /** Ensemble sales forecast (units): average of moving average and Commons Math regression prediction. */
    @Column(name = "predicted_sales", nullable = false, precision = 18, scale = 4)
    private BigDecimal predictedSales;

    /** predictedSales × product unit price. */
    @Column(name = "predicted_revenue", nullable = false, precision = 18, scale = 4)
    private BigDecimal predictedRevenue;

    /**
     * Lower confidence bound (units): predictedSales − σ where σ is the standard deviation
     * of monthly sales quantities for this product.
     */
    @Column(name = "lower_bound", nullable = false, precision = 18, scale = 4)
    private BigDecimal lowerBound;

    /**
     * Upper confidence bound (units): predictedSales + σ (same σ as lower bound).
     */
    @Column(name = "upper_bound", nullable = false, precision = 18, scale = 4)
    private BigDecimal upperBound;

    @Column(name = "forecast_month", nullable = false)
    private LocalDate forecastMonth;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(BigDecimal lowerBound) {
        this.lowerBound = lowerBound;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(BigDecimal upperBound) {
        this.upperBound = upperBound;
    }

    public LocalDate getForecastMonth() {
        return forecastMonth;
    }

    public void setForecastMonth(LocalDate forecastMonth) {
        this.forecastMonth = forecastMonth;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
