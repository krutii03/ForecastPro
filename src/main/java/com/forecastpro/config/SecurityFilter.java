package com.forecastpro.config;

import com.forecastpro.controller.UserSessionBean;
import com.forecastpro.entity.UserRole;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(urlPatterns = {"/app/*", "/vendor/*"})
public class SecurityFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String ctx = req.getContextPath();
        UserSessionBean userSession = CDI.current().select(UserSessionBean.class).get();
        if (!userSession.isLoggedIn()) {
            res.sendRedirect(ctx + "/login.xhtml");
            return;
        }
        UserRole role = userSession.getRole();
        String path = req.getRequestURI().substring(ctx.length());

        if (path.startsWith("/vendor/")) {
            if (role != UserRole.VENDOR) {
                res.sendRedirect(ctx + "/app/dashboard.xhtml");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        if (role == UserRole.VENDOR) {
            res.sendRedirect(ctx + "/vendor/dashboard.xhtml");
            return;
        }

        if (path.startsWith("/app/admin/categories.xhtml")) {
            if (role != UserRole.ADMIN && role != UserRole.EMPLOYEE) {
                res.sendRedirect(ctx + "/app/dashboard.xhtml");
                return;
            }
        } else if (path.startsWith("/app/admin/")) {
            if (role != UserRole.ADMIN) {
                res.sendRedirect(ctx + "/app/dashboard.xhtml");
                return;
            }
        } else if (path.startsWith("/app/analytics/") || path.startsWith("/app/forecast/")
                || path.startsWith("/app/alerts/")) {
            if (role != UserRole.ADMIN && role != UserRole.SALES_MANAGER) {
                res.sendRedirect(ctx + "/app/dashboard.xhtml");
                return;
            }
        } else if (path.startsWith("/app/products/") || path.startsWith("/app/sales/")) {
            if (role != UserRole.ADMIN && role != UserRole.EMPLOYEE && role != UserRole.SALES_MANAGER) {
                res.sendRedirect(ctx + "/app/dashboard.xhtml");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
