package com.forecastpro.service;

import com.forecastpro.dto.CategorySalesRow;
import com.forecastpro.dto.MonthTrendRow;
import com.forecastpro.dto.MonthlySalesRow;
import com.forecastpro.dto.PredictedRevenueRow;
import com.forecastpro.dto.ProductSalesRow;
import com.forecastpro.entity.Forecast;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.ForecastRepository;
import com.forecastpro.repository.SaleRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class AnalyticsService {

    private static final MathContext MC = new MathContext(8, RoundingMode.HALF_UP);

    @Inject
    private SaleRepository saleRepository;

    @Inject
    private ForecastRepository forecastRepository;

    @Inject
    private SecurityService securityService;

    @Inject
    private ProductService productService;

    @Inject
    private ForecastService forecastService;

    public BigDecimal totalRevenue(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        return saleRepository.sumTotalRevenue();
    }

    /** Number of sale rows in the system (for analytics dashboards). */
    public long saleRecordCount(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        return saleRepository.countSales();
    }

    public List<MonthlySalesRow> monthlySummary(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        List<MonthlySalesRow> rows = new ArrayList<>();
        for (Object[] o : saleRepository.monthlyRevenueNative()) {
            int y = ((Number) o[0]).intValue();
            int m = ((Number) o[1]).intValue();
            BigDecimal rev = toBd(o[2]);
            rows.add(new MonthlySalesRow(y, m, rev));
        }
        return rows;
    }

    public List<CategorySalesRow> categoryWise(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        List<CategorySalesRow> rows = new ArrayList<>();
        for (Object[] o : saleRepository.categoryRevenueNative()) {
            rows.add(new CategorySalesRow(String.valueOf(o[0]), toBd(o[1])));
        }
        return rows;
    }

    public List<ProductSalesRow> productWise(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        List<ProductSalesRow> rows = new ArrayList<>();
        for (Object[] o : saleRepository.productRevenueNative()) {
            Long id = ((Number) o[0]).longValue();
            String name = String.valueOf(o[1]);
            BigDecimal rev = toBd(o[2]);
            long qty = ((Number) o[3]).longValue();
            rows.add(new ProductSalesRow(id, name, rev, qty));
        }
        return rows;
    }

    public List<ProductSalesRow> topSelling(UserRole caller, int limit) {
        securityService.requireAdminOrSalesManager(caller);
        List<ProductSalesRow> rows = new ArrayList<>();
        for (Object[] o : saleRepository.topProductsNative(limit)) {
            Long id = ((Number) o[0]).longValue();
            String name = String.valueOf(o[1]);
            BigDecimal rev = toBd(o[2]);
            long qty = ((Number) o[3]).longValue();
            rows.add(new ProductSalesRow(id, name, rev, qty));
        }
        return rows;
    }

    public List<ProductSalesRow> lowSelling(UserRole caller, int limit) {
        securityService.requireAdminOrSalesManager(caller);
        List<ProductSalesRow> rows = new ArrayList<>();
        for (Object[] o : saleRepository.lowProductsNative(limit)) {
            Long id = ((Number) o[0]).longValue();
            String name = String.valueOf(o[1]);
            BigDecimal rev = toBd(o[2]);
            long qty = ((Number) o[3]).longValue();
            rows.add(new ProductSalesRow(id, name, rev, qty));
        }
        return rows;
    }

    /** Sum of latest forecast {@code predicted_revenue} per product. */
    public BigDecimal totalLatestPredictedRevenue(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        return forecastRepository.sumPredictedRevenueLatestPerProduct();
    }

    /**
     * Total predicted revenue for next month across all products.
     * Computed from sales history on-the-fly so it works even if no forecasts were generated/saved yet.
     */
    public BigDecimal totalNextMonthPredictedRevenueAllProducts(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        BigDecimal sum = BigDecimal.ZERO;
        for (Product p : productService.listAll(caller)) {
            try {
                Forecast f = forecastService.computeNextMonthForecast(caller, p.getId());
                if (f.getPredictedRevenue() != null) {
                    sum = sum.add(f.getPredictedRevenue());
                }
            } catch (Exception ignored) {
                // No sales history (or other issues) → treat as 0 for total.
            }
        }
        return sum;
    }

    /** Latest forecast per product, highest predicted revenue first. */
    public List<PredictedRevenueRow> topPredictedRevenueProducts(UserRole caller, int limit) {
        securityService.requireAdminOrSalesManager(caller);
        List<PredictedRevenueRow> rows = new ArrayList<>();
        for (Object[] o : forecastRepository.latestForecastRevenueRows(limit)) {
            java.time.LocalDate month = null;
            Object m = o.length > 2 ? o[2] : null;
            if (m instanceof java.sql.Date d) {
                month = d.toLocalDate();
            } else if (m instanceof java.time.LocalDate ld) {
                month = ld;
            } else if (m != null) {
                month = java.sql.Date.valueOf(m.toString()).toLocalDate();
            }
            rows.add(new PredictedRevenueRow(String.valueOf(o[0]), toBd(o[1]), month));
        }
        return rows;
    }

    public List<MonthTrendRow> monthToMonthTrends(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        List<MonthlySalesRow> monthly = monthlySummary(caller);
        List<MonthTrendRow> trends = new ArrayList<>();
        BigDecimal prev = null;
        for (MonthlySalesRow r : monthly) {
            BigDecimal change = BigDecimal.ZERO;
            if (prev != null && prev.signum() != 0) {
                change = r.getRevenue().subtract(prev)
                        .divide(prev, MC)
                        .multiply(BigDecimal.valueOf(100));
            }
            trends.add(new MonthTrendRow(r.getLabel(), r.getRevenue(), change));
            prev = r.getRevenue();
        }
        return trends;
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        return new BigDecimal(v.toString());
    }
}
