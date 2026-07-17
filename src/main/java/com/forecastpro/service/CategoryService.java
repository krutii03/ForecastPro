package com.forecastpro.service;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Category;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.CategoryRepository;
import com.forecastpro.util.ValidationUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class CategoryService {

    private static final Logger LOG = Logger.getLogger(CategoryService.class.getName());

    @Inject
    private CategoryRepository categoryRepository;

    @Inject
    private SecurityService securityService;

    public List<Category> listAll(UserRole caller) {
        securityService.requireAdminOrEmployee(caller);
        return categoryRepository.findAllOrdered();
    }

    /**
     * Read-only list for dropdowns (all authenticated roles need category → product flows).
     */
    public List<Category> listForSelection(UserRole caller) {
        if (caller == null) {
            throw new BusinessException("Not authenticated.");
        }
        return categoryRepository.findAllOrdered();
    }

    /**
     * JSF dropdowns: never null; safe if session not ready on first phase.
     */
    public List<Category> findAllForUi(UserRole caller) {
        if (caller == null) {
            LOG.fine("findAllForUi: caller null, returning empty list");
            return Collections.emptyList();
        }
        List<Category> list = categoryRepository.findAllOrdered();
        LOG.info(() -> "ForecastPro categories loaded for UI: count=" + list.size());
        return list;
    }

    public Category create(UserRole caller, String name) {
        securityService.requireAdminOrEmployee(caller);
        ValidationUtil.requireNonBlank(name, "Category name");
        if (categoryRepository.existsNameForOther(name.trim(), null)) {
            throw new BusinessException("Category name already exists.");
        }
        Category c = new Category();
        c.setName(name.trim());
        return categoryRepository.save(c);
    }

    public Category update(UserRole caller, Long id, String name) {
        securityService.requireAdminOrEmployee(caller);
        ValidationUtil.requireNonBlank(name, "Category name");
        Category c = categoryRepository.findById(id).orElseThrow(() -> new BusinessException("Category not found."));
        if (categoryRepository.existsNameForOther(name.trim(), c.getId())) {
            throw new BusinessException("Category name already exists.");
        }
        c.setName(name.trim());
        return categoryRepository.save(c);
    }

    public void delete(UserRole caller, Long id) {
        securityService.requireAdminOrEmployee(caller);
        Category c = categoryRepository.findById(id).orElseThrow(() -> new BusinessException("Category not found."));
        if (!c.getProducts().isEmpty()) {
            throw new BusinessException("Cannot delete category with products.");
        }
        categoryRepository.delete(c);
    }
}
