package com.forecastpro.repository;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Product;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

@Stateless
public class ProductRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public Product save(Product product) {
        if (product.getId() == null) {
            em.persist(product);
            return product;
        }
        return em.merge(product);
    }

    public void delete(Product product) {
        em.remove(em.contains(product) ? product : em.merge(product));
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(em.find(Product.class, id));
    }

    public List<Product> findByCategoryId(Long categoryId) {
        TypedQuery<Product> q = em.createQuery(
                "SELECT p FROM Product p WHERE p.category.id = :cid ORDER BY p.name", Product.class);
        q.setParameter("cid", categoryId);
        return q.getResultList();
    }

    public List<Product> findAllOrdered() {
        return em.createQuery(
                "SELECT p FROM Product p JOIN FETCH p.category c ORDER BY c.name, p.name", Product.class)
                .getResultList();
    }

    public long count() {
        return em.createQuery("SELECT COUNT(p) FROM Product p", Long.class).getSingleResult();
    }

    public List<Product> findLowStock(int threshold) {
        return em.createQuery(
                "SELECT p FROM Product p JOIN FETCH p.category WHERE p.stockQuantity <= :t ORDER BY p.stockQuantity",
                Product.class)
                .setParameter("t", threshold)
                .getResultList();
    }

    /** {@code delta} may be positive (receipt) or negative (adjustment). */
    public void adjustStock(Long productId, int delta) {
        Product p = em.find(Product.class, productId);
        if (p == null) {
            throw new BusinessException("Product not found.");
        }
        int next = p.getStockQuantity() + delta;
        if (next < 0) {
            throw new BusinessException("Stock cannot be negative.");
        }
        p.setStockQuantity(next);
    }
}
