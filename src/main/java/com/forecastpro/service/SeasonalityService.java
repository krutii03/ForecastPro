package com.forecastpro.service;

import com.forecastpro.dto.SeasonalityRow;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.ProductRepository;
import com.forecastpro.repository.SaleRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Stateless
public class SeasonalityService {

    @Inject
    private ProductRepository productRepository;

    @Inject
    private SaleRepository saleRepository;

    @Inject
    private SecurityService securityService;

    /**
     * Identifies strongest and weakest calendar months per product by average units sold.
     */
    public List<SeasonalityRow> seasonalityReport(UserRole caller, Long categoryId, Long productId) {
        securityService.requireAdminOrSalesManager(caller);
        List<SeasonalityRow> rows = new ArrayList<>();
        for (Product p : productRepository.findAllOrdered()) {
            if (categoryId != null && !categoryId.equals(p.getCategory().getId())) {
                continue;
            }
            if (productId != null && !productId.equals(p.getId())) {
                continue;
            }
            List<Object[]> monthly = saleRepository.monthlyAverageUnitsByProduct(p.getId());
            if (monthly.isEmpty()) {
                continue;
            }
            int bestMonthNum = 1;
            int worstMonthNum = 1;
            double bestAvg = -1d;
            double worstAvg = Double.MAX_VALUE;
            for (Object[] o : monthly) {
                int m = ((Number) o[0]).intValue();
                double avg = o[1] instanceof BigDecimal bd ? bd.doubleValue() : ((Number) o[1]).doubleValue();
                if (avg > bestAvg) {
                    bestAvg = avg;
                    bestMonthNum = m;
                }
                if (avg < worstAvg) {
                    worstAvg = avg;
                    worstMonthNum = m;
                }
            }
            BigDecimal overallAvg = saleRepository.averageMonthlyUnitsForProduct(p.getId())
                    .setScale(2, RoundingMode.HALF_UP);
            SeasonalityRow row = new SeasonalityRow();
            row.setProductName(p.getName());
            row.setBestMonth(monthName(bestMonthNum));
            row.setWorstMonth(monthName(worstMonthNum));
            row.setAverageMonthlySales(overallAvg);
            rows.add(row);
        }
        rows.sort(Comparator.comparing(SeasonalityRow::getProductName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    /** Chart data: calendar month label → average units (aggregated across filtered products). */
    public List<Object[]> seasonalityChartData(UserRole caller, Long categoryId, Long productId) {
        securityService.requireAdminOrSalesManager(caller);
        double[] monthTotals = new double[13];
        int[] monthCounts = new int[13];
        for (Product p : productRepository.findAllOrdered()) {
            if (categoryId != null && !categoryId.equals(p.getCategory().getId())) {
                continue;
            }
            if (productId != null && !productId.equals(p.getId())) {
                continue;
            }
            for (Object[] o : saleRepository.monthlyAverageUnitsByProduct(p.getId())) {
                int m = ((Number) o[0]).intValue();
                double avg = o[1] instanceof BigDecimal bd ? bd.doubleValue() : ((Number) o[1]).doubleValue();
                if (m >= 1 && m <= 12) {
                    monthTotals[m] += avg;
                    monthCounts[m]++;
                }
            }
        }
        List<Object[]> chart = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            double val = monthCounts[m] > 0 ? monthTotals[m] / monthCounts[m] : 0d;
            chart.add(new Object[]{monthName(m), val});
        }
        return chart;
    }

    private static String monthName(int monthNum) {
        return Month.of(Math.max(1, Math.min(12, monthNum)))
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
