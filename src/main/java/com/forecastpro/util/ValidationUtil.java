package com.forecastpro.util;

import com.forecastpro.config.BusinessException;

import java.math.BigDecimal;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + " is required.");
        }
    }

    public static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new BusinessException(field + " must be positive.");
        }
    }

    public static void requireNonNegativeInt(int value, String field) {
        if (value < 0) {
            throw new BusinessException(field + " cannot be negative.");
        }
    }

    public static void requireNonNegativeMoney(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new BusinessException(field + " cannot be negative.");
        }
    }
}
