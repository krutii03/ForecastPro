package com.forecastpro.dto;

import java.math.BigDecimal;

public class MonthTrendRow {

    private String monthLabel;
    private BigDecimal revenue;
    private BigDecimal changePercent;

    public MonthTrendRow() {
    }

    public MonthTrendRow(String monthLabel, BigDecimal revenue, BigDecimal changePercent) {
        this.monthLabel = monthLabel;
        this.revenue = revenue;
        this.changePercent = changePercent;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }
}
