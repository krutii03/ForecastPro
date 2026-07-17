package com.forecastpro.service;

import com.forecastpro.dto.InventoryAlertRow;
import com.forecastpro.dto.InventoryRecommendationRow;
import com.forecastpro.entity.Forecast;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.ForecastRepository;
import com.forecastpro.repository.ProductRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Stateless
public class AlertService {

    @Inject
    private ProductRepository productRepository;

    @Inject
    private ForecastRepository forecastRepository;

    @Inject
    private SecurityService securityService;

    @Inject
    private EmailService emailService;

    public List<InventoryAlertRow> inventoryAlerts(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        List<InventoryAlertRow> alerts = new ArrayList<>();
        for (InventoryRecommendationRow rec : inventoryRecommendations(caller)) {
            if (!rec.isAlert()) {
                continue;
            }
            InventoryAlertRow row = new InventoryAlertRow();
            row.setProductId(rec.getProductId());
            row.setCategoryId(rec.getCategoryId());
            row.setProductName(rec.getProductName());
            row.setCategoryName(rec.getCategoryName());
            row.setStockQuantity(rec.getStockQuantity());
            row.setForecastDemand(rec.getForecastDemand());
            row.setRecommendedRestock(rec.getRecommendedPurchase());
            alerts.add(row);
        }
        alerts.sort(Comparator.comparing(InventoryAlertRow::getRecommendedRestock).reversed());
        return alerts;
    }

    public List<InventoryRecommendationRow> inventoryRecommendations(UserRole caller) {
        securityService.requireAdminOrSalesManager(caller);
        List<InventoryRecommendationRow> rows = new ArrayList<>();
        for (Product p : productRepository.findAllOrdered()) {
            Optional<Forecast> opt = forecastRepository.findLatestForProduct(p.getId());
            BigDecimal forecast = opt.map(f -> f.getPredictedSales() != null ? f.getPredictedSales() : BigDecimal.ZERO)
                    .orElse(BigDecimal.ZERO);
            int stock = p.getStockQuantity();
            int minThreshold = InventoryThresholds.minimumFor(p);

            boolean belowForecast = forecast.signum() > 0 && stock < forecast.intValue();
            boolean belowMin = stock < minThreshold;
            boolean needsAlert = belowForecast || belowMin;

            InventoryRecommendationRow row = new InventoryRecommendationRow();
            row.setProductId(p.getId());
            row.setCategoryId(p.getCategory().getId());
            row.setProductName(p.getName());
            row.setCategoryName(p.getCategory().getName());
            row.setStockQuantity(stock);
            row.setForecastDemand(forecast.setScale(2, RoundingMode.HALF_UP));
            row.setMinimumThreshold(minThreshold);
            row.setAlert(needsAlert);

            BigDecimal criticalThreshold = forecast.multiply(BigDecimal.valueOf(0.3));
            BigDecimal doubleForecast = forecast.multiply(BigDecimal.valueOf(2));

            if (!needsAlert) {
                row.setStockStatus("Healthy");
                row.setRecommendedPurchase(BigDecimal.ZERO);
                row.setStatusBadgeClass("inv-badge-healthy");
            } else {
                int target = Math.max(forecast.intValue(), minThreshold);
                BigDecimal purchase = BigDecimal.valueOf(Math.max(1, target - stock));
                row.setRecommendedPurchase(purchase);
                if (belowMin && (belowForecast && stock <= criticalThreshold.intValue())) {
                    row.setStockStatus("Critical");
                    row.setStatusBadgeClass("inv-badge-critical");
                } else if (belowForecast && stock <= criticalThreshold.intValue()) {
                    row.setStockStatus("Critical");
                    row.setStatusBadgeClass("inv-badge-critical");
                } else if (belowMin || belowForecast) {
                    row.setStockStatus("Restock Required");
                    row.setStatusBadgeClass("inv-badge-restock");
                }
            }

            if (needsAlert) {
                row.setSmartRecommendation("Urgent Restock");
                row.setSmartBadgeClass("inv-badge-urgent");
            } else if (forecast.signum() > 0 && stock > doubleForecast.intValue()) {
                row.setSmartRecommendation("Reduce Inventory");
                row.setSmartBadgeClass("inv-badge-reduce");
            } else {
                row.setSmartRecommendation("Stock Level Healthy");
                row.setSmartBadgeClass("inv-badge-healthy");
            }

            rows.add(row);

            if (needsAlert) {
                emailService.sendInventoryAlert(
                        p.getName(),
                        stock,
                        forecast.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        row.getRecommendedPurchase().toPlainString(),
                        row.getStockStatus());
            }
        }
        rows.sort(Comparator.comparing(InventoryRecommendationRow::isAlert).reversed()
                .thenComparing(InventoryRecommendationRow::getRecommendedPurchase, Comparator.reverseOrder()));
        return rows;
    }
}
