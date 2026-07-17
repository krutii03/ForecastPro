package com.forecastpro.service;

import com.forecastpro.config.BusinessException;
import com.forecastpro.dto.ForecastAccuracyMetrics;
import com.forecastpro.dto.ForecastAccuracyRow;
import com.forecastpro.dto.ForecastDashboardKpis;
import com.forecastpro.entity.Forecast;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.UserRole;
import com.forecastpro.ml.MovingAverageForecaster;
import com.forecastpro.repository.ForecastRepository;
import com.forecastpro.repository.ProductRepository;
import com.forecastpro.repository.SaleRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

@Stateless
public class ForecastService {

    private static final Logger LOG = Logger.getLogger(ForecastService.class.getName());

    private static final int MOVING_AVG_WINDOW = 3;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Inject
    private SaleRepository saleRepository;

    @Inject
    private ProductRepository productRepository;

    @Inject
    private ForecastRepository forecastRepository;

    @Inject
    private SecurityService securityService;

    /**
     * Compute next-month forecast for a product without saving.
     * Used for analytics dashboards where forecasts may not have been generated yet.
     */
    public Forecast computeNextMonthForecast(UserRole caller, Long productId) {
        securityService.requireAdminOrSalesManager(caller);
        return buildForecastForMonth(productId, nextForecastMonth());
    }

    public Forecast generateForecast(UserRole caller, Long productId) {
        securityService.requireAdminOrSalesManager(caller);
        return saveForecastForMonth(productId, nextForecastMonth());
    }

    /**
     * On-demand: generate or refresh next-month forecasts for every product with sales history.
     */
    public int generateForecastsForAllProducts(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        return saveForecastsForAllProducts(nextForecastMonth(), false);
    }

    /**
     * Automatic monthly generation (scheduler): create forecast for every product
     * for the upcoming month if (product_id, forecast_month) does not already exist.
     */
    public int generateForecastsForAllProductsAutomatic() {
        return saveForecastsForAllProducts(nextForecastMonth(), true);
    }

    private int saveForecastsForAllProducts(LocalDate targetMonth, boolean skipExisting) {
        int saved = 0;
        for (Product product : productRepository.findAllOrdered()) {
            if (skipExisting && forecastRepository.existsByProductAndMonth(product.getId(), targetMonth)) {
                continue;
            }
            try {
                Forecast f = saveForecastForMonth(product.getId(), targetMonth);
                saved++;
                LOG.info(() -> "Forecast saved: Product: " + product.getName()
                        + " Forecast month: " + f.getForecastMonth()
                        + " Predicted units: " + f.getPredictedSales());
            } catch (BusinessException e) {
                LOG.fine(() -> "Skipped forecast for product " + product.getId() + ": " + e.getMessage());
            }
        }
        return saved;
    }

    private Forecast saveForecastForMonth(Long productId, LocalDate forecastMonth) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));
        List<BigDecimal> monthlyQuantities = loadMonthlyQuantitySeries(productId);
        if (monthlyQuantities.isEmpty()) {
            throw new BusinessException("No sales history for this product. Enter sales data first.");
        }
        LOG.info(() -> "Forecast for product " + productId + ": months of history=" + monthlyQuantities.size());

        ForecastComponents c = computeComponents(monthlyQuantities, product);
        LocalDate monthKey = forecastMonth.withDayOfMonth(1);
        Forecast f = forecastRepository.findByProductAndMonth(productId, monthKey).orElseGet(Forecast::new);
        f.setProduct(product);
        f.setMovingAvgValue(c.maScaled());
        f.setMlRegressionValue(c.regressionScaled());
        f.setPredictedSales(c.predictedSales());
        f.setPredictedRevenue(c.predictedRevenue());
        f.setLowerBound(c.lowerBound());
        f.setUpperBound(c.upperBound());
        f.setForecastMonth(monthKey);
        f.setCreatedAt(Instant.now());
        Forecast saved = forecastRepository.save(f);
        LOG.info(() -> "Forecast saved id=" + saved.getId() + " product=" + productId + " month=" + saved.getForecastMonth());
        return saved;
    }

    private Forecast buildForecastForMonth(Long productId, LocalDate forecastMonth) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));
        List<BigDecimal> monthlyQuantities = loadMonthlyQuantitySeries(productId);
        if (monthlyQuantities.isEmpty()) {
            throw new BusinessException("No sales history for this product.");
        }
        ForecastComponents c = computeComponents(monthlyQuantities, product);
        Forecast f = new Forecast();
        f.setProduct(product);
        f.setMovingAvgValue(c.maScaled());
        f.setMlRegressionValue(c.regressionScaled());
        f.setPredictedSales(c.predictedSales());
        f.setPredictedRevenue(c.predictedRevenue());
        f.setLowerBound(c.lowerBound());
        f.setUpperBound(c.upperBound());
        f.setForecastMonth(forecastMonth.withDayOfMonth(1));
        f.setCreatedAt(Instant.now());
        return f;
    }

    /**
     * Forecast accuracy: compare stored predictions against actual sales for months that have ended.
     * Accuracy % = (1 − |predicted − actual| / actual) × 100
     */
    public List<ForecastAccuracyRow> forecastAccuracyRows(UserRole caller, LocalDate from, LocalDate to,
                                                          Long categoryId, Long productId) {
        securityService.requireAdminOrSalesManager(caller);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        List<ForecastAccuracyRow> rows = new ArrayList<>();
        for (Forecast f : forecastRepository.findByFilters(from.withDayOfMonth(1), to.withDayOfMonth(1),
                categoryId, productId, 1000)) {
            if (f.getForecastMonth() == null || !f.getForecastMonth().isBefore(today.withDayOfMonth(1))) {
                continue;
            }
            BigDecimal predicted = f.getPredictedSales() != null ? f.getPredictedSales() : BigDecimal.ZERO;
            BigDecimal actual = saleRepository.quantitySoldInMonth(f.getProduct().getId(), f.getForecastMonth());
            BigDecimal absError = predicted.subtract(actual).abs();
            BigDecimal accuracyPct = BigDecimal.ZERO;
            if (actual.signum() > 0) {
                // Accuracy % = (1 - |predicted - actual| / actual) * 100
                accuracyPct = BigDecimal.ONE.subtract(absError.divide(actual, 6, RoundingMode.HALF_UP))
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            ForecastAccuracyRow row = new ForecastAccuracyRow();
            row.setProductName(f.getProduct().getName());
            row.setForecastMonth(f.getForecastMonth());
            row.setPredictedUnits(predicted.setScale(2, RoundingMode.HALF_UP));
            row.setActualUnits(actual.setScale(2, RoundingMode.HALF_UP));
            row.setAbsoluteError(absError.setScale(2, RoundingMode.HALF_UP));
            row.setAccuracyPercent(accuracyPct);
            rows.add(row);
        }
        rows.sort(Comparator.comparing(ForecastAccuracyRow::getForecastMonth).reversed()
                .thenComparing(ForecastAccuracyRow::getProductName));
        return rows;
    }

    /**
     * MAE = mean(|predicted − actual|), MSE = mean((predicted − actual)²),
     * RMSE = √MSE, MAPE = mean(|predicted − actual| / actual) × 100 (actual > 0 only).
     */
    public ForecastAccuracyMetrics computeAccuracyMetrics(List<ForecastAccuracyRow> rows) {
        ForecastAccuracyMetrics m = new ForecastAccuracyMetrics();
        if (rows == null || rows.isEmpty()) {
            return m;
        }
        double sumAbs = 0d;
        double sumSq = 0d;
        double sumMape = 0d;
        int mapeCount = 0;
        int n = rows.size();
        for (ForecastAccuracyRow r : rows) {
            double pred = r.getPredictedUnits() != null ? r.getPredictedUnits().doubleValue() : 0d;
            double act = r.getActualUnits() != null ? r.getActualUnits().doubleValue() : 0d;
            double err = pred - act;
            sumAbs += Math.abs(err);
            sumSq += err * err;
            if (act > 0d) {
                sumMape += Math.abs(err) / act;
                mapeCount++;
            }
        }
        double mae = sumAbs / n;
        double mse = sumSq / n;
        double rmse = Math.sqrt(mse);
        double mape = mapeCount > 0 ? (sumMape / mapeCount) * 100d : 0d;
        m.setMae(BigDecimal.valueOf(mae).setScale(2, RoundingMode.HALF_UP));
        m.setMse(BigDecimal.valueOf(mse).setScale(2, RoundingMode.HALF_UP));
        m.setRmse(BigDecimal.valueOf(rmse).setScale(2, RoundingMode.HALF_UP));
        m.setMape(BigDecimal.valueOf(mape).setScale(2, RoundingMode.HALF_UP));
        return m;
    }

    /** KPI cards from latest forecasts per product. */
    public ForecastDashboardKpis computeDashboardKpis(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        List<Forecast> recent = forecastRepository.findRecent(500);
        java.util.Map<Long, Forecast> latestByProduct = new java.util.LinkedHashMap<>();
        for (Forecast f : recent) {
            if (f.getProduct() == null) {
                continue;
            }
            Long pid = f.getProduct().getId();
            Forecast prev = latestByProduct.get(pid);
            if (prev == null || (f.getForecastMonth() != null && prev.getForecastMonth() != null
                    && f.getForecastMonth().isAfter(prev.getForecastMonth()))) {
                latestByProduct.put(pid, f);
            }
        }
        ForecastDashboardKpis kpis = new ForecastDashboardKpis();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalUnits = 0;
        Forecast highest = null;
        Forecast lowest = null;
        for (Forecast f : latestByProduct.values()) {
            if (f.getPredictedRevenue() != null) {
                totalRevenue = totalRevenue.add(f.getPredictedRevenue());
            }
            if (f.getPredictedSales() != null) {
                totalUnits += f.getPredictedSales().longValue();
            }
            if (highest == null || f.getPredictedSales().compareTo(highest.getPredictedSales()) > 0) {
                highest = f;
            }
            if (lowest == null || f.getPredictedSales().compareTo(lowest.getPredictedSales()) < 0) {
                lowest = f;
            }
        }
        kpis.setForecastedRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP));
        kpis.setTotalPredictedUnits(totalUnits);
        if (highest != null && highest.getProduct() != null) {
            kpis.setHighestPredictedProduct(highest.getProduct().getName());
        }
        if (lowest != null && lowest.getProduct() != null) {
            kpis.setLowestPredictedProduct(lowest.getProduct().getName());
        }
        kpis.setForecastGrowthPercent(computeForecastGrowthPercent(recent));
        return kpis;
    }

    /**
     * Forecast growth %: latest month vs previous month, only when every forecastable product
     * has a stored forecast for both months (avoids misleading % from partial manual runs).
     */
    private BigDecimal computeForecastGrowthPercent(List<Forecast> forecasts) {
        Set<Long> forecastableIds = forecastableProductIds();
        if (forecastableIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        TreeMap<YearMonth, Map<Long, Forecast>> byMonth = groupLatestForecastPerProductMonth(forecasts);
        if (byMonth.isEmpty()) {
            return BigDecimal.ZERO;
        }

        YearMonth latestComplete = null;
        for (YearMonth ym : byMonth.descendingKeySet()) {
            if (monthHasCompleteForecasts(byMonth.get(ym), forecastableIds)) {
                latestComplete = ym;
                break;
            }
        }
        if (latestComplete == null) {
            return BigDecimal.ZERO;
        }

        YearMonth previous = latestComplete.minusMonths(1);
        Map<Long, Forecast> currentByProduct = byMonth.get(latestComplete);
        Map<Long, Forecast> previousByProduct = byMonth.get(previous);
        if (!monthHasCompleteForecasts(previousByProduct, forecastableIds)) {
            return BigDecimal.ZERO;
        }

        double currentSum = sumPredictedUnits(currentByProduct, forecastableIds);
        double previousSum = sumPredictedUnits(previousByProduct, forecastableIds);
        if (previousSum == 0d) {
            return BigDecimal.ZERO;
        }
        double growth = ((currentSum - previousSum) / previousSum) * 100d;
        return BigDecimal.valueOf(growth).setScale(2, RoundingMode.HALF_UP);
    }

    private Set<Long> forecastableProductIds() {
        Set<Long> ids = new HashSet<>();
        for (Product product : productRepository.findAllOrdered()) {
            if (!loadMonthlyQuantitySeries(product.getId()).isEmpty()) {
                ids.add(product.getId());
            }
        }
        return ids;
    }

    private static TreeMap<YearMonth, Map<Long, Forecast>> groupLatestForecastPerProductMonth(List<Forecast> forecasts) {
        TreeMap<YearMonth, Map<Long, Forecast>> byMonth = new TreeMap<>();
        for (Forecast f : forecasts) {
            if (f.getForecastMonth() == null || f.getProduct() == null || f.getProduct().getId() == null) {
                continue;
            }
            YearMonth ym = YearMonth.from(f.getForecastMonth());
            Long productId = f.getProduct().getId();
            byMonth.computeIfAbsent(ym, k -> new HashMap<>()).merge(productId, f, (existing, incoming) -> {
                if (existing.getCreatedAt() == null) {
                    return incoming;
                }
                if (incoming.getCreatedAt() == null) {
                    return existing;
                }
                return incoming.getCreatedAt().isAfter(existing.getCreatedAt()) ? incoming : existing;
            });
        }
        return byMonth;
    }

    private static boolean monthHasCompleteForecasts(Map<Long, Forecast> monthForecasts, Set<Long> requiredProductIds) {
        return monthForecasts != null && monthForecasts.keySet().containsAll(requiredProductIds);
    }

    private static double sumPredictedUnits(Map<Long, Forecast> monthForecasts, Set<Long> productIds) {
        double sum = 0d;
        for (Long productId : productIds) {
            Forecast f = monthForecasts.get(productId);
            if (f != null && f.getPredictedSales() != null) {
                sum += f.getPredictedSales().doubleValue();
            }
        }
        return sum;
    }

    private record ForecastComponents(
            BigDecimal maScaled,
            BigDecimal regressionScaled,
            BigDecimal predictedSales,
            BigDecimal predictedRevenue,
            BigDecimal lowerBound,
            BigDecimal upperBound) {
    }

    private ForecastComponents computeComponents(List<BigDecimal> monthlyQuantities, Product product) {
        BigDecimal movingAvg = MovingAverageForecaster.forecast(monthlyQuantities, MOVING_AVG_WINDOW);
        BigDecimal regressionRaw = regressionPredictNext(monthlyQuantities);

        BigDecimal maScaled = movingAvg.setScale(4, RoundingMode.HALF_UP);
        double ma = maScaled.doubleValue();
        double reg = regressionRaw.doubleValue();
        // Clamp regression to ±20% of moving average
        double clampedReg = Math.max(ma * 0.8, Math.min(ma * 1.2, reg));
        BigDecimal regressionScaled = BigDecimal.valueOf(clampedReg).setScale(4, RoundingMode.HALF_UP);
        // Weighted: 70% MA + 30% clamped regression
        BigDecimal predictedSales = maScaled.multiply(BigDecimal.valueOf(0.7))
                .add(regressionScaled.multiply(BigDecimal.valueOf(0.3)))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal predictedRevenue = predictedSales.multiply(product.getPrice()).setScale(2, RoundingMode.HALF_UP);

        double sigma = monthlySalesStdDev(monthlyQuantities);
        BigDecimal lower = predictedSales.subtract(BigDecimal.valueOf(sigma))
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal upper = predictedSales.add(BigDecimal.valueOf(sigma))
                .setScale(2, RoundingMode.HALF_UP);

        return new ForecastComponents(maScaled, regressionScaled, predictedSales, predictedRevenue, lower, upper);
    }

    /** σ = standard deviation of monthly sales quantities (Apache Commons Math). */
    private static double monthlySalesStdDev(List<BigDecimal> monthlyQuantities) {
        if (monthlyQuantities == null || monthlyQuantities.size() < 2) {
            return 0d;
        }
        double[] values = monthlyQuantities.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        return new StandardDeviation().evaluate(values);
    }

    private static BigDecimal regressionPredictNext(List<BigDecimal> monthlyTotals) {
        if (monthlyTotals == null || monthlyTotals.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int n = monthlyTotals.size();
        SimpleRegression reg = new SimpleRegression();
        for (int i = 0; i < n; i++) {
            reg.addData(i, monthlyTotals.get(i).doubleValue());
        }
        double pred = reg.predict(n);
        if (Double.isNaN(pred) || Double.isInfinite(pred)) {
            double sum = 0d;
            for (BigDecimal b : monthlyTotals) {
                sum += b.doubleValue();
            }
            pred = sum / n;
        }
        return BigDecimal.valueOf(pred);
    }

    private LocalDate nextForecastMonth() {
        YearMonth ym = YearMonth.from(LocalDate.now(BUSINESS_ZONE)).plusMonths(1);
        return ym.atDay(1);
    }

    private List<BigDecimal> loadMonthlyQuantitySeries(Long productId) {
        List<BigDecimal> series = new ArrayList<>();
        for (Object[] row : saleRepository.monthlyQuantityByProductNative(productId)) {
            BigDecimal qty = new BigDecimal(row[2].toString());
            series.add(qty);
        }
        return series;
    }

    public List<Forecast> recentForecasts(UserRole caller, int max) {
        securityService.requireAdminOrSalesManager(caller);
        return forecastRepository.findRecent(max);
    }
}
