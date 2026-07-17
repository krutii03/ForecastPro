package com.forecastpro.dto;

import java.math.BigDecimal;

/**
 * Aggregate forecast accuracy KPIs (MAE, MSE, RMSE, MAPE).
 */
public class ForecastAccuracyMetrics {

    private BigDecimal mae = BigDecimal.ZERO;
    private BigDecimal mse = BigDecimal.ZERO;
    private BigDecimal rmse = BigDecimal.ZERO;
    private BigDecimal mape = BigDecimal.ZERO;

    public BigDecimal getMae() {
        return mae;
    }

    public void setMae(BigDecimal mae) {
        this.mae = mae;
    }

    public BigDecimal getMse() {
        return mse;
    }

    public void setMse(BigDecimal mse) {
        this.mse = mse;
    }

    public BigDecimal getRmse() {
        return rmse;
    }

    public void setRmse(BigDecimal rmse) {
        this.rmse = rmse;
    }

    public BigDecimal getMape() {
        return mape;
    }

    public void setMape(BigDecimal mape) {
        this.mape = mape;
    }
}
