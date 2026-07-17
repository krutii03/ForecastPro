package com.forecastpro.controller;

import com.forecastpro.config.AccessDeniedException;
import com.forecastpro.config.BusinessException;
import com.forecastpro.dto.ForecastDashboardKpis;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.Forecast;
import com.forecastpro.entity.Product;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demand-side forecast UI: moving average, Commons Math regression component, and predicted units.
 */
@Named("demandForecastBean")
@ViewScoped
public class DemandForecastBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(DemandForecastBean.class.getName());
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    @Inject
    private ForecastService forecastService;

    @Inject
    private ProductService productService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private UserSessionBean userSession;

    private List<Category> categories = new ArrayList<>();
    private Long selectedCategoryId;
    private List<Product> products = new ArrayList<>();
    private Long selectedProductId;
    private Forecast lastForecast;
    private List<Forecast> recentForecasts = new ArrayList<>();
    private ForecastDashboardKpis forecastKpis = new ForecastDashboardKpis();

    private LineChartModel lineModel;
    private BarChartModel barModel1;

    @PostConstruct
    public void init() {
        loadCategories();
        // Dashboard-style: load history immediately (not dependent on selection).
        loadRecentForecasts();
    }

    public void loadCategories() {
        categories = new ArrayList<>(categoryService.findAllForUi(userSession.getRole()));
    }

    public void loadRecentForecasts() {
        recentForecasts = new ArrayList<>(forecastService.recentForecasts(userSession.getRole(), 40));
        forecastKpis = forecastService.computeDashboardKpis(userSession.getRole());
        buildCharts();
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
            LOG.info(() -> "DemandForecastBean.generate called categoryId=" + selectedCategoryId
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
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Demand forecast ready.", null));
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
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Bulk demand forecast complete.", detail));
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

    public List<Forecast> getRecentForecasts() {
        return recentForecasts;
    }

    // Alias getter for dashboard-like UI naming
    public List<Forecast> getDemandForecastHistoryList() {
        return recentForecasts;
    }

    public LineChartModel getLineModel() {
        return lineModel;
    }

    public BarChartModel getBarModel1() {
        return barModel1;
    }

    public ForecastDashboardKpis getForecastKpis() {
        return forecastKpis;
    }

    private void buildCharts() {
        lineModel = buildDemandForecastVsAvgLine();
        barModel1 = buildProductWiseDemandBar();
    }

    private LineChartModel buildDemandForecastVsAvgLine() {
        LineChartModel model = new LineChartModel();
        ChartData data = new ChartData();

        // month -> average values across all forecast rows for that month
        Map<LocalDate, double[]> byMonth = new TreeMap<>(); // [sumMovingAvg, sumPredicted, count]
        for (Forecast f : recentForecasts) {
            if (f == null || f.getForecastMonth() == null) {
                continue;
            }
            double ma = f.getMovingAvgValue() != null ? f.getMovingAvgValue().doubleValue() : 0d;
            double pred = f.getPredictedSales() != null ? f.getPredictedSales().doubleValue() : 0d;
            double[] agg = byMonth.computeIfAbsent(f.getForecastMonth(), k -> new double[]{0d, 0d, 0d});
            agg[0] += ma;
            agg[1] += pred;
            agg[2] += 1d;
        }

        List<String> labels = new ArrayList<>();
        List<Object> maValues = new ArrayList<>();
        List<Object> predValues = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> e : byMonth.entrySet()) {
            labels.add(e.getKey().format(MONTH_FMT));
            double[] agg = e.getValue();
            double count = agg[2] == 0d ? 1d : agg[2];
            maValues.add(agg[0] / count);
            predValues.add(agg[1] / count);
        }
        if (labels.isEmpty()) {
            labels.add("—");
            maValues.add(0d);
            predValues.add(0d);
        }

        LineChartDataSet dsMa = new LineChartDataSet();
        dsMa.setData(maValues);
        dsMa.setLabel("Recent Average Demand");
        dsMa.setFill(false);
        dsMa.setBorderColor("rgb(107, 114, 128)");
        dsMa.setBackgroundColor("rgba(107, 114, 128, 0.15)");
        dsMa.setTension(0.25);

        LineChartDataSet dsPred = new LineChartDataSet();
        dsPred.setData(predValues);
        dsPred.setLabel("Estimated Demand (Final Prediction)");
        dsPred.setFill(false);
        dsPred.setBorderColor("rgb(30, 64, 175)");
        dsPred.setBackgroundColor("rgba(30, 64, 175, 0.15)");
        dsPred.setTension(0.25);

        data.addChartDataSet(dsMa);
        data.addChartDataSet(dsPred);
        data.setLabels(labels);
        model.setData(data);

        LineChartOptions opts = new LineChartOptions();
        Title title = new Title();
        title.setDisplay(false);
        opts.setTitle(title);
        model.setOptions(opts);
        return model;
    }

    private BarChartModel buildProductWiseDemandBar() {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        // product -> latest predicted units (latest forecastMonth; tie-break createdAt via list ordering)
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
        entries.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));

        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (Map.Entry<String, Forecast> e : entries) {
            labels.add(e.getKey());
            values.add(e.getValue().getPredictedSales() != null ? e.getValue().getPredictedSales().doubleValue() : 0d);
        }
        if (labels.isEmpty()) {
            labels.add("—");
            values.add(0d);
        }

        BarChartDataSet ds = new BarChartDataSet();
        ds.setLabel("Predicted units");
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
