package com.forecastpro.service;

import com.forecastpro.entity.InventoryMovement;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.UserRole;
import com.forecastpro.entity.Vendor;
import com.forecastpro.repository.InventoryMovementRepository;
import com.forecastpro.repository.VendorRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateless
public class InventoryService {

    /** @deprecated use {@link #sourceLabelForVendor(Vendor)} for stock-request receipts */
    @Deprecated
    public static final String SOURCE_VENDOR_DELIVERY = "Vendor Delivery";

    /** Demo / legacy labels stored before vendor names were written to {@code inventory.source}. */
    private static final Set<String> LEGACY_GENERIC_SOURCES = Set.of(
            "Initial Stock",
            SOURCE_VENDOR_DELIVERY,
            "Warehouse Update",
            "Manual Correction",
            "Emergency Purchase"
    );

    /** Category id → specialist vendor id (seed: 1=Furniture, 2=Appliances, 3=Electronics). */
    private static final Map<Long, Long> VENDOR_ID_BY_CATEGORY = Map.of(
            1L, 1L,
            2L, 2L,
            3L, 3L
    );

    /** Inventory source label when stock is received from a vendor stock request. */
    public static String sourceLabelForVendor(Vendor vendor) {
        if (vendor == null || vendor.getName() == null || vendor.getName().isBlank()) {
            return SOURCE_VENDOR_DELIVERY;
        }
        return vendor.getName().trim();
    }

    /** Vendor name for reports and UI; resolves legacy generic source strings from product category. */
    public String inventoryVendorLabel(InventoryMovement movement) {
        if (movement == null) {
            return "—";
        }
        String stored = movement.getSource();
        if (stored != null && !stored.isBlank() && !isLegacyGenericSource(stored)) {
            return stored.trim();
        }
        String vendorName = vendorNameForProduct(movement.getProduct());
        if (vendorName != null) {
            return vendorName;
        }
        return stored != null && !stored.isBlank() ? stored.trim() : "—";
    }

    public String vendorNameForProduct(Product product) {
        if (product == null || product.getCategory() == null) {
            return null;
        }
        Long vendorId = VENDOR_ID_BY_CATEGORY.getOrDefault(product.getCategory().getId(), 4L);
        return vendorRepository.findById(vendorId).map(Vendor::getName).orElse(null);
    }

    private static boolean isLegacyGenericSource(String source) {
        return source != null && LEGACY_GENERIC_SOURCES.contains(source.trim());
    }

    @Inject
    private InventoryMovementRepository inventoryMovementRepository;

    @Inject
    private VendorRepository vendorRepository;

    @Inject
    private SecurityService securityService;

    public List<InventoryMovement> listHistory(UserRole caller) {
        securityService.requireAuthenticated(caller);
        return inventoryMovementRepository.findRecent(500);
    }
}
