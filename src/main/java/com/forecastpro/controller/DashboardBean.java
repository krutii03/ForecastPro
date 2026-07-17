package com.forecastpro.controller;

import com.forecastpro.entity.UserRole;
import com.forecastpro.service.DashboardService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    @Inject
    private DashboardService dashboardService;

    @Inject
    private UserSessionBean userSession;

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private long salesCount;
    private long forecastCount;
    private long lowStockCount;
    private int alertCount;
    private boolean showAnalytics;
    private boolean showAlerts;

    @PostConstruct
    public void init() {
        Map<String, Object> m = dashboardService.buildDashboard(userSession.getRole());
        totalRevenue = (BigDecimal) m.get("totalRevenue");
        salesCount = ((Number) m.get("salesCount")).longValue();
        forecastCount = ((Number) m.get("forecastCount")).longValue();
        lowStockCount = ((Number) m.get("lowStockCount")).longValue();
        alertCount = (Integer) m.get("alertCount");
        showAnalytics = Boolean.TRUE.equals(m.get("showAnalytics"));
        showAlerts = Boolean.TRUE.equals(m.get("showAlerts"));
    }

    public void refresh() {
        init();
    }

    public boolean isEmployee() {
        return userSession.getRole() == UserRole.EMPLOYEE;
    }

    public boolean isAdminOrManager() {
        UserRole r = userSession.getRole();
        return r == UserRole.ADMIN || r == UserRole.SALES_MANAGER;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public long getSalesCount() {
        return salesCount;
    }

    public long getForecastCount() {
        return forecastCount;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public int getAlertCount() {
        return alertCount;
    }

    public boolean isShowAnalytics() {
        return showAnalytics;
    }

    public boolean isShowAlerts() {
        return showAlerts;
    }
}
