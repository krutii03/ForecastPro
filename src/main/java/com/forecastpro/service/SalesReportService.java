package com.forecastpro.service;

import com.forecastpro.dto.MonthlyProductSalesRow;
import com.forecastpro.dto.YearlyProductSalesRow;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.SaleRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class SalesReportService {

    @Inject
    private SaleRepository saleRepository;

    @Inject
    private SecurityService securityService;

    /**
     * Rolling window: from first day of month (now − 11 months) through end of current month.
     */
    public List<MonthlyProductSalesRow> monthlySales(UserRole caller, Long categoryId, Long productId) {
        LocalDate to = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        LocalDate from = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        return monthlySales(caller, categoryId, productId, from, to);
    }

    public List<MonthlyProductSalesRow> monthlySales(UserRole caller, Long categoryId, Long productId,
                                                     LocalDate fromInclusive, LocalDate toInclusive) {
        securityService.requireSalesReportAccess(caller);
        LocalDate from = fromInclusive != null ? fromInclusive : LocalDate.now().minusMonths(11).withDayOfMonth(1);
        LocalDate to = toInclusive != null ? toInclusive : LocalDate.now();
        if (from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        return mapMonthly(saleRepository.monthlySalesByProductFiltered(categoryId, productId, from, to));
    }

    public List<YearlyProductSalesRow> yearlyTotals(UserRole caller, Long categoryId, Long productId, int year) {
        securityService.requireSalesReportAccess(caller);
        List<YearlyProductSalesRow> rows = new ArrayList<>();
        for (Object[] o : saleRepository.yearlySalesByProductFiltered(categoryId, productId, year)) {
            Long pid = ((Number) o[0]).longValue();
            String name = String.valueOf(o[1]);
            long qty = ((Number) o[2]).longValue();
            BigDecimal rev = toBd(o[3]);
            rows.add(new YearlyProductSalesRow(pid, name, year, qty, rev));
        }
        return rows;
    }

    private static List<MonthlyProductSalesRow> mapMonthly(List<Object[]> raw) {
        List<MonthlyProductSalesRow> rows = new ArrayList<>();
        for (Object[] o : raw) {
            Long pid = ((Number) o[0]).longValue();
            String name = String.valueOf(o[1]);
            int y = ((Number) o[2]).intValue();
            int m = ((Number) o[3]).intValue();
            long qty = ((Number) o[4]).longValue();
            BigDecimal rev = toBd(o[5]);
            rows.add(new MonthlyProductSalesRow(pid, name, y, m, qty, rev));
        }
        return rows;
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
