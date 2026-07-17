package com.forecastpro.service;

import com.forecastpro.config.AccessDeniedException;
import com.forecastpro.entity.UserRole;
import jakarta.ejb.Stateless;

@Stateless
public class SecurityService {

    public void requireAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new AccessDeniedException("Administrator access required.");
        }
    }

    public void requireAdminOrEmployee(UserRole role) {
        if (role != UserRole.ADMIN && role != UserRole.EMPLOYEE) {
            throw new AccessDeniedException("This action is restricted to Admin or Employee.");
        }
    }

    public void requireAdminOrSalesManager(UserRole role) {
        if (role != UserRole.ADMIN && role != UserRole.SALES_MANAGER) {
            throw new AccessDeniedException("This page is restricted to Admin or Sales Manager.");
        }
    }

    public void requireAuthenticated(UserRole role) {
        if (role == null) {
            throw new AccessDeniedException("Sign in required.");
        }
    }

    public void requireEmployee(UserRole role) {
        if (role != UserRole.EMPLOYEE) {
            throw new AccessDeniedException("Only employees can perform this action.");
        }
    }

    /** Admin or Sales Manager (management). */
    public void requireAdminOrManager(UserRole role) {
        if (role != UserRole.ADMIN && role != UserRole.SALES_MANAGER) {
            throw new AccessDeniedException("Administrator or Sales Manager access required.");
        }
    }

    /** Sales report: Admin, Sales Manager, or Employee. */
    public void requireSalesReportAccess(UserRole role) {
        if (role != UserRole.ADMIN && role != UserRole.SALES_MANAGER && role != UserRole.EMPLOYEE) {
            throw new AccessDeniedException("Access denied.");
        }
    }
}
