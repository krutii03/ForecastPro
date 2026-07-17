package com.forecastpro.repository;

import com.forecastpro.entity.Category;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

@Stateless
public class CategoryRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public Category save(Category category) {
        if (category.getId() == null) {
            em.persist(category);
            return category;
        }
        return em.merge(category);
    }

    public void delete(Category category) {
        em.remove(em.contains(category) ? category : em.merge(category));
    }

    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(em.find(Category.class, id));
    }

    public List<Category> findAllOrdered() {
        return em.createQuery("SELECT c FROM Category c ORDER BY c.name", Category.class)
                .getResultList();
    }

    public boolean existsNameForOther(String name, Long excludeId) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(c) FROM Category c WHERE c.name = :n AND (:id IS NULL OR c.id <> :id)",
                Long.class);
        q.setParameter("n", name);
        q.setParameter("id", excludeId);
        return q.getSingleResult() > 0;
    }
}
