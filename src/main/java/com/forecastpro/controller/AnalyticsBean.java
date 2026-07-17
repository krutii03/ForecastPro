package com.forecastpro.controller;

import com.forecastpro.dto.CategorySalesRow;
import com.forecastpro.dto.MonthlySalesRow;
import com.forecastpro.dto.ProductSalesRow;
import com.forecastpro.service.AnalyticsService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.title.Title;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Named("analyticsBean")
@ViewScoped
public class AnalyticsBean implements Serializable {

    @Inject
    private AnalyticsService analyticsService;

    @Inject
    private UserSessionBean userSession;

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private long totalSaleRecords;
    private long totalUnitsSold;
    private int productCount;

    private List<MonthlySalesRow> monthly = new ArrayList<>();
    private List<CategorySalesRow> categoryWise = new ArrayList<>();
    private List<ProductSalesRow> productWise = new ArrayList<>();
    private List<ProductSalesRow> topSelling = new ArrayList<>();
    private List<ProductSalesRow> lowSelling = new ArrayList<>();
    private BigDecimal totalPredictedRevenue = BigDecimal.ZERO;
    private LocalDate nextForecastMonth;

    private LineChartModel monthlyLineModel;
    private BarChartModel categoryBarModel;
    private BarChartModel topProductsBarModel;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @PostConstruct
    public void init() {
        nextForecastMonth = YearMonth.from(LocalDate.now(BUSINESS_ZONE)).plusMonths(1).atDay(1);
        totalRevenue = analyticsService.totalRevenue(userSession.getRole());
        totalSaleRecords = analyticsService.saleRecordCount(userSession.getRole());
        monthly = new ArrayList<>(analyticsService.monthlySummary(userSession.getRole()));
        categoryWise = new ArrayList<>(analyticsService.categoryWise(userSession.getRole()));
        productWise = new ArrayList<>(analyticsService.productWise(userSession.getRole()));
        topSelling = new ArrayList<>(analyticsService.topSelling(userSession.getRole(), 100));
        lowSelling = new ArrayList<>(analyticsService.lowSelling(userSession.getRole(), 100));
        totalPredictedRevenue = analyticsService.totalNextMonthPredictedRevenueAllProducts(userSession.getRole());

        totalUnitsSold = productWise.stream().mapToLong(ProductSalesRow::getQuantitySold).sum();
        productCount = productWise.size();

        buildCharts();
    }

    public LocalDate getNextForecastMonth() {
        return nextForecastMonth;
    }

    private void buildCharts() {
        monthlyLineModel = buildMonthlyLineChart();
        categoryBarModel = buildCategoryBarChart();
        topProductsBarModel = buildTopProductsBarChart();
    }

    private LineChartModel buildMonthlyLineChart() {
        LineChartModel model = new LineChartModel();
        ChartData data = new ChartData();

        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (MonthlySalesRow row : monthly) {
            labels.add(row.getLabel());
            values.add(row.getRevenue() != null ? row.getRevenue().doubleValue() : 0d);
        }
        if (labels.isEmpty()) {
            labels.add("—");
            values.add(0d);
        }

        LineChartDataSet dataSet = new LineChartDataSet();
        dataSet.setData(values);
        dataSet.setLabel("Revenue (₹)");
        dataSet.setFill(false);
        dataSet.setBorderColor("rgb(30, 64, 175)");
        dataSet.setBackgroundColor("rgba(30, 64, 175, 0.15)");
        dataSet.setTension(0.25);

        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        model.setData(data);

        LineChartOptions opts = new LineChartOptions();
        Title title = new Title();
        title.setDisplay(true);
        title.setText("Monthly revenue");
        opts.setTitle(title);
        model.setOptions(opts);
        return model;
    }

    private BarChartModel buildCategoryBarChart() {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (CategorySalesRow row : categoryWise) {
            labels.add(row.getCategoryName());
            values.add(row.getRevenue() != null ? row.getRevenue().doubleValue() : 0d);
        }
        if (labels.isEmpty()) {
            labels.add("—");
            values.add(0d);
        }

        BarChartDataSet dataSet = new BarChartDataSet();
        dataSet.setData(values);
        dataSet.setLabel("Revenue (₹)");
        dataSet.setBackgroundColor("rgba(30, 64, 175, 0.6)");
        dataSet.setBorderColor("rgb(30, 64, 175)");
        dataSet.setBorderWidth(1);

        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        model.setData(data);

        org.primefaces.model.charts.bar.BarChartOptions opts = new org.primefaces.model.charts.bar.BarChartOptions();
        Title title = new Title();
        title.setDisplay(true);
        title.setText("Revenue by category");
        opts.setTitle(title);
        model.setOptions(opts);

        return model;
    }

    private BarChartModel buildTopProductsBarChart() {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (ProductSalesRow row : topSelling) {
            String name = row.getProductName();
            if (name != null && name.length() > 24) {
                name = name.substring(0, 21) + "…";
            }
            labels.add(name != null ? name : "—");
            values.add(row.getRevenue() != null ? row.getRevenue().doubleValue() : 0d);
        }
        if (labels.isEmpty()) {
            labels.add("—");
            values.add(0d);
        }

        BarChartDataSet dataSet = new BarChartDataSet();
        dataSet.setData(values);
        dataSet.setLabel("Revenue (₹)");
        dataSet.setBackgroundColor("rgba(5, 150, 105, 0.65)");
        dataSet.setBorderColor("rgb(5, 120, 85)");
        dataSet.setBorderWidth(1);

        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        model.setData(data);

        org.primefaces.model.charts.bar.BarChartOptions opts = new org.primefaces.model.charts.bar.BarChartOptions();
        Title title = new Title();
        title.setDisplay(true);
        title.setText("Top products (by revenue)");
        opts.setTitle(title);
        model.setOptions(opts);

        return model;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public long getTotalSaleRecords() {
        return totalSaleRecords;
    }

    public long getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public int getProductCount() {
        return productCount;
    }

    public List<MonthlySalesRow> getMonthly() {
        return monthly;
    }

    public List<CategorySalesRow> getCategoryWise() {
        return categoryWise;
    }

    public List<ProductSalesRow> getProductWise() {
        return productWise;
    }

    public List<ProductSalesRow> getTopSelling() {
        return topSelling;
    }

    public List<ProductSalesRow> getLowSelling() {
        return lowSelling;
    }

    public BigDecimal getTotalPredictedRevenue() {
        return totalPredictedRevenue;
    }

    public LineChartModel getMonthlyLineModel() {
        return monthlyLineModel;
    }

    public BarChartModel getCategoryBarModel() {
        return categoryBarModel;
    }

    public BarChartModel getTopProductsBarModel() {
        return topProductsBarModel;
    }

    public void refresh() {
        init();
    }
}