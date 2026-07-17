package com.forecastpro.dto;

import com.forecastpro.util.DisplayFormats;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MonthlySalesRow {

    private int year;
    private int month;
    private BigDecimal revenue;

    public MonthlySalesRow() {
    }

    public MonthlySalesRow(int year, int month, BigDecimal revenue) {
        this.year = year;
        this.month = month;
        this.revenue = revenue;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public String getLabel() {
        return DisplayFormats.formatDate(LocalDate.of(year, month, 1));
    }
}
