package com.forecastpro.ml;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public final class MovingAverageForecaster {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    private MovingAverageForecaster() {
    }

    /**
     * @param monthlyTotals 
     * @param window        
     */
    public static BigDecimal forecast(List<BigDecimal> monthlyTotals, int window) {
        if (monthlyTotals == null || monthlyTotals.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int from = Math.max(0, monthlyTotals.size() - window);
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int i = from; i < monthlyTotals.size(); i++) {
            sum = sum.add(monthlyTotals.get(i));
            count++;
        }
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return sum.divide(BigDecimal.valueOf(count), MC);
    }
}
