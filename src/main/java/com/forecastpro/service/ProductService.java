package com.forecastpro.service;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.CategoryRepository;
import com.forecastpro.repository.ForecastRepository;
import com.forecastpro.repository.ProductRepository;
import com.forecastpro.util.ValidationUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Stateless
public class ProductService {

    @Inject
    private ProductRepository productRepository;

    @Inject
    private CategoryRepository categoryRepository;

    @Inject
    private ForecastRepository forecastRepository;

    @Inject
    private SecurityService securityService;

    /**
     * Read-only listing by category (inventory, forecast, sales flows).
     */
    public List<Product> findByCategory(UserRole caller, Long categoryId) {
        requireAuthenticated(caller);
        if (categoryId == null) {
            return Collections.emptyList();
        }
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> listByCategory(UserRole caller, Long categoryId) {
        return findByCategory(caller, categoryId);
    }

    public List<Product> listAll(UserRole caller) {
        requireAuthenticated(caller);
        return productRepository.findAllOrdered();
    }

    private void requireAuthenticated(UserRole caller) {
        if (caller == null) {
            throw new BusinessException("Not authenticated.");
        }
    }

    public Product saveProduct(UserRole caller, Long id, Long categoryId, String name, BigDecimal price, int stock) {
        securityService.requireAdminOrEmployee(caller);
        ValidationUtil.requireNonBlank(name, "Product name");
        ValidationUtil.requireNonNegativeMoney(price, "Price");
        ValidationUtil.requireNonNegativeInt(stock, "Stock");
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("Category not found."));
        Product p;
        if (id == null) {
            p = new Product();
        } else {
            p = productRepository.findById(id).orElseThrow(() -> new BusinessException("Product not found."));
        }
        p.setCategory(cat);
        p.setName(name.trim());
        p.setPrice(price);
        p.setStockQuantity(stock);
        return productRepository.save(p);
    }

    public void delete(UserRole caller, Long id) {
        securityService.requireAdminOrEmployee(caller);
        Product p = productRepository.findById(id).orElseThrow(() -> new BusinessException("Product not found."));
        if (!p.getSales().isEmpty()) {
            throw new BusinessException("Cannot delete product with sales history.");
        }
        forecastRepository.deleteByProductId(p.getId());
        productRepository.delete(p);
    }
}
