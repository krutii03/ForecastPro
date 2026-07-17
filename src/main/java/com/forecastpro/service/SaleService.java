package com.forecastpro.service;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.Sale;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.ProductRepository;
import com.forecastpro.repository.SaleRepository;
import com.forecastpro.util.DisplayFormats;
import com.forecastpro.util.ValidationUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.logging.Logger;

@Stateless
public class SaleService {

    private static final Logger LOG = Logger.getLogger(SaleService.class.getName());
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Inject
    private SaleRepository saleRepository;

    @Inject
    private ProductRepository productRepository;

    @Inject
    private SecurityService securityService;

    public Sale recordSale(UserRole caller, Long productId, int quantitySold, LocalDate saleDate) {
        securityService.requireAdminOrEmployee(caller);
        ValidationUtil.requirePositive(quantitySold, "Quantity sold");
        if (saleDate == null) {
            throw new BusinessException("Sale date is required.");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (saleDate.isAfter(today)) {
            throw new BusinessException("Sale date cannot be in the future.");
        }
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));
        if (p.getStockQuantity() < quantitySold) {
            throw new BusinessException("Insufficient stock. Available: " + p.getStockQuantity());
        }
        Sale s = new Sale();
        s.setProduct(p);
        s.setQuantitySold(quantitySold);
        s.setSaleDate(saleDate);
        saleRepository.save(s);
        p.setStockQuantity(p.getStockQuantity() - quantitySold);
        productRepository.save(p);
        LOG.info(() -> "Sale saved: productId=" + productId + " qty=" + quantitySold + " date="
                + DisplayFormats.formatDate(saleDate) + " newStock=" + p.getStockQuantity());
        return s;
    }
}
