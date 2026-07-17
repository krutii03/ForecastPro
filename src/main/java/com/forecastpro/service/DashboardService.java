package com.forecastpro.service;

import com.forecastpro.dto.InventoryAlertRow;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.ForecastRepository;
import com.forecastpro.repository.ProductRepository;
import com.forecastpro.repository.SaleRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class DashboardService {

    @Inject
    private ProductRepository productRepository;

    @Inject
    private SaleRepository saleRepository;

    @Inject
    private AlertService alertService;

    @Inject
    private ForecastRepository forecastRepository;

    public Map<String, Object> buildDashboard(UserRole role) {
        Map<String, Object> m = new HashMap<>();
        if (role == UserRole.VENDOR) {
            m.put("forecastCount", 0L);
            m.put("totalRevenue", BigDecimal.ZERO);
            m.put("salesCount", 0L);
            m.put("lowStockCount", 0L);
            m.put("alertCount", 0);
            m.put("showAnalytics", false);
            m.put("showAlerts", false);
            return m;
        }
        m.put("forecastCount", forecastRepository.countAll());
        m.put("totalRevenue", BigDecimal.ZERO);
        m.put("salesCount", 0L);
        m.put("lowStockCount", 0L);
        m.put("alertCount", 0);
        m.put("showAnalytics", false);
        m.put("showAlerts", false);

        if (role == UserRole.EMPLOYEE) {
            m.put("totalRevenue", BigDecimal.ZERO);
            m.put("salesCount", saleRepository.countSales());
            m.put("lowStockCount", 0L);
            return m;
        }

        m.put("totalRevenue", saleRepository.sumTotalRevenue());
        m.put("salesCount", saleRepository.countSales());
        m.put("lowStockCount", productRepository.findLowStock(10).size());

        if (role == UserRole.ADMIN || role == UserRole.SALES_MANAGER) {
            m.put("showAnalytics", true);
            m.put("showAlerts", true);
            List<InventoryAlertRow> alerts = alertService.inventoryAlerts(role);
            m.put("alertCount", alerts.size());
        }
        return m;
    }
}
