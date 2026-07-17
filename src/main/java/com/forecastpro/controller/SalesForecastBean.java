package com.forecastpro.controller;

import com.forecastpro.config.AccessDeniedException;
import com.forecastpro.config.BusinessException;
import com.forecastpro.dto.ForecastDashboardKpis;
import com.forecastpro.dto.PredictedRevenueRow;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.Forecast;
import com.forecastpro.entity.Product;
import com.forecastpro.service.AnalyticsService;
import com.forecastpro.service.CategoryService;
import com.forecastpro.service.ForecastService;
import com.forecastpro.service.ProductService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sales / revenue forecast UI: unit prediction × product price → predicted revenue.
 */
@Named("salesForecastBean")
@ViewScoped
public class SalesForecastBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(SalesForecastBean.class.getName());
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    @Inject
    private ForecastService forecastService;

    @Inject
    private ProductService productService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private AnalyticsService analyticsService;

    @Inject
    private UserSessionBean userSession;

    private List<Category> categories = new ArrayList<>();
    private Long selectedCategoryId;
    private List<Product> products = new ArrayList<>();
    private Long selectedProductId;
    private Forecast lastForecast;
    private List<Forecast> recentForecasts = new ArrayList<>();
    private List<PredictedRevenueRow> topPredictedRevenue = new ArrayList<>();
    private ForecastDashboardKpis forecastKpis = new ForecastDashboardKpis();

    private LineChartModel lineModel;
    private BarChartModel barModel1;
    private BarChartModel barModel2;

    @PostConstruct
    public void init() {
        // Dashboard-style: load tables immediately (not dependent on selected product).
        loadCategories();
        loadRecentForecasts();
    }

    public void loadCategories() {
        categories = new ArrayList<>(categoryService.findAllForUi(userSession.getRole()));
    }

    public void loadRecentForecasts() {
        recentForecasts = new ArrayList<>(forecastService.recentForecasts(userSession.getRole(), 40));
        forecastKpis = forecastService.computeDashboardKpis(userSession.getRole());
        loadTopPredictedRevenue();
        buildCharts();
    }

    private void loadTopPredictedRevenue() {
        topPredictedRevenue = new ArrayList<>(analyticsService.topPredictedRevenueProducts(userSession.getRole(), 8));
    }

    public void loadProducts() {
        products = new ArrayList<>();
        selectedProductId = null;
        lastForecast = null;
        if (selectedCategoryId == null) {
            return;
        }
        products = productService.listByCategory(userSession.getRole(), selectedCategoryId);
    }

    public void generate() {
        try {
            LOG.info(() -> "SalesForecastBean.generate called categoryId=" + selectedCategoryId
                    + " productId=" + selectedProductId + " role=" + userSession.getRole());
            if (selectedCategoryId == null) {
                throw new BusinessException("Select a category first.");
            }
            if (selectedProductId == null) {
                throw new BusinessException("Select a product.");
            }
            lastForecast = forecastService.generateForecast(userSession.getRole(), selectedProductId);
            loadRecentForecasts();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Sales forecast ready.", null));
        } catch (BusinessException e) {
            FacesMessage.Severity sev = FacesMessage.SEVERITY_ERROR;
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("already exists")) {
                sev = FacesMessage.SEVERITY_WARN;
            }
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(sev, e.getMessage(), null));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, userMessage(e), null));
        }
    }

    public void generateAll() {
        try {
            int saved = forecastService.generateForecastsForAllProducts(userSession.getRole());
            loadRecentForecasts();
            String detail = saved > 0
                    ? "Updated next-month forecasts for " + saved + " product(s)."
                    : "No forecasts saved. Ensure products have sales history.";
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Bulk sales forecast complete.", detail));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, userMessage(e), null));
        }
    }

    private static String userMessage(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof BusinessException || t instanceof AccessDeniedException) {
                return t.getMessage();
            }
            if (t.getCause() != null && t.getCause() != t) {
                t = t.getCause();
            } else {
                break;
            }
        }
        String m = e.getMessage();
        return m != null && !m.isBlank() ? m : "Unexpected error while generating forecast.";
    }

    /** Unit price for the currently selected product (from the loaded category list). */
    public BigDecimal getSelectedProductUnitPrice() {
        if (selectedProductId == null) {
            return null;
        }
        return products.stream()
                .filter(p -> selectedProductId.equals(p.getId()))
                .map(Product::getPrice)
                .findFirst()
                .orElse(null);
    }

    /** Prefer price from the latest forecast row when present. */
    public BigDecimal getDisplayUnitPrice() {
        if (lastForecast != null && lastForecast.getProduct() != null) {
            return lastForecast.getProduct().getPrice();
        }
        return getSelectedProductUnitPrice();
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Long selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public List<Product> getProducts() {
        return products;
    }

    public Long getSelectedProductId() {
        return selectedProductId;
    }

    public void setSelectedProductId(Long selectedProductId) {
        this.selectedProductId = selectedProductId;
    }

    public Forecast getLastForecast() {
        return lastForecast;
    }

    // Alias getters for UI readability (dashboard-like tables)
    public List<Forecast> getSalesForecastHistoryList() {
        return recentForecasts;
    }

    public List<PredictedRevenueRow> getTopPredictedRevenueList() {
        return topPredictedRevenue;
    }

    public List<Forecast> getRecentForecasts() {
        return recentForecasts;
    }

    public List<PredictedRevenueRow> getTopPredictedRevenue() {
        return topPredictedRevenue;
    }

    public LineChartModel getLineModel() {
        return lineModel;
    }

    public BarChartModel getBarModel1() {
        return barModel1;
    }

    public BarChartModel getBarModel2() {
        return barModel2;
    }

    public ForecastDashboardKpis getForecastKpis() {
        return forecastKpis;
    }

    private void buildCharts() {
        lineModel = buildRevenueTrendLine();
        barModel1 = buildProductWiseRevenueBar(false);
        barModel2 = buildProductWiseRevenueBar(true);
    }

    private LineChartModel buildRevenueTrendLine() {
        LineChartModel model = new LineChartModel();
        ChartData data = new ChartData();

        // month -> total predicted revenue (sum across products)
        Map<LocalDate, Double> byMonth = new TreeMap<>();
        for (Forecast f : recentForecasts) {
            if (f == null || f.getForecastMonth() == null || f.getPredictedRevenue() == null) {
                continue;
            }
            byMonth.merge(f.getForecastMonth(), f.getPredictedRevenue().doubleValue(), Double::sum);
        }

        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> e : byMonth.entrySet()) {
            labels.add(e.getKey().format(MONTH_FMT));
            values.add(e.getValue());
        }
        if (labels.isEmpty()) {
            labels.add("—");
            values.add(0d);
        }

        LineChartDataSet ds = new LineChartDataSet();
        ds.setData(values);
        ds.setLabel("Predicted revenue (₹)");
        ds.setFill(false);
        ds.setBorderColor("rgb(30, 64, 175)");
        ds.setBackgroundColor("rgba(30, 64, 175, 0.15)");
        ds.setTension(0.25);

        data.addChartDataSet(ds);
        data.setLabels(labels);
        model.setData(data);

        LineChartOptions opts = new LineChartOptions();
        Title title = new Title();
        title.setDisplay(false);
        opts.setTitle(title);
        model.setOptions(opts);
        return model;
    }

    private BarChartModel buildProductWiseRevenueBar(boolean sortDesc) {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        // product -> latest predicted revenue (latest forecastMonth)
        Map<String, Forecast> latestByProduct = new LinkedHashMap<>();
        for (Forecast f : recentForecasts) {
            if (f == null || f.getProduct() == null || f.getProduct().getName() == null || f.getForecastMonth() == null) {
                continue;
            }
            String name = f.getProduct().getName();
            Forecast prev = latestByProduct.get(name);
            if (prev == null || f.getForecastMonth().isAfter(prev.getForecastMonth())) {
                latestByProduct.put(name, f);
            }
        }

        List<Map.Entry<String, Forecast>> entries = new ArrayList<>(latestByProduct.entrySet());
        if (sortDesc) {
            entries.sort(Comparator.comparingDouble((Map.Entry<String, Forecast> e) ->
                    e.getValue().getPredictedRevenue() != null ? e.getValue().getPredictedRevenue().doubleValue() : 0d).reversed());
        } else {
            entries.sort(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER));
        }

        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (Map.Entry<String, Forecast> e : entries) {
            labels.add(e.getKey());
            values.add(e.getValue().getPredictedRevenue() != null ? e.getValue().getPredictedRevenue().doubleValue() : 0d);
        }
        if (labels.isEmpty()) {
            labels.add("—");
            values.add(0d);
        }

        BarChartDataSet ds = new BarChartDataSet();
        ds.setLabel("Predicted revenue (₹)");
        ds.setData(values);
        ds.setBackgroundColor("rgba(30, 64, 175, 0.45)");
        ds.setBorderColor("rgb(30, 64, 175)");
        ds.setBorderWidth(1);

        data.addChartDataSet(ds);
        data.setLabels(labels);
        model.setData(data);
        return model;
    }
}
