package com.forecastpro.service;

import com.forecastpro.entity.Product;

import java.util.Map;

/** Minimum stock thresholds for low-stock alerts (per product name or category). */
public final class InventoryThresholds {

    private static final Map<String, Integer> BY_PRODUCT = Map.of(
            "Laptop", 10,
            "Smartphone", 20,
            "Bluetooth Speaker", 15,
            "Microwave", 10,
            "Gaming Console", 5,
            "Smart Watch", 15
    );

    private static final int FURNITURE_DEFAULT = 20;
    private static final int DEFAULT = 10;

    private InventoryThresholds() {
    }

    public static int minimumFor(Product product) {
        if (product == null) {
            return DEFAULT;
        }
        Integer named = BY_PRODUCT.get(product.getName());
        if (named != null) {
            return named;
        }
        if (product.getCategory() != null && "Furniture".equalsIgnoreCase(product.getCategory().getName())) {
            return FURNITURE_DEFAULT;
        }
        return DEFAULT;
    }
}
