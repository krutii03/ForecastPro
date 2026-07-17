package com.forecastpro.repository;

import com.forecastpro.entity.InventoryMovement;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;

@Stateless
public class InventoryMovementRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public InventoryMovement save(InventoryMovement row) {
        em.persist(row);
        return row;
    }

    public List<InventoryMovement> findRecent(int maxRows) {
        return em.createQuery(
                        "SELECT i FROM InventoryMovement i JOIN FETCH i.product p JOIN FETCH p.category "
                                + "ORDER BY i.createdAt DESC",
                        InventoryMovement.class)
                .setMaxResults(maxRows)
                .getResultList();
    }

    public List<InventoryMovement> findByFilters(LocalDate fromInclusive, LocalDate toInclusive,
                                                 Long categoryId, Long productId, int maxRows) {
        String jpql = "SELECT i FROM InventoryMovement i JOIN FETCH i.product p JOIN FETCH p.category c "
                + "WHERE i.dateAdded >= :from AND i.dateAdded <= :to "
                + "AND (:cid IS NULL OR c.id = :cid) "
                + "AND (:pid IS NULL OR p.id = :pid) "
                + "ORDER BY i.dateAdded DESC, i.createdAt DESC";
        TypedQuery<InventoryMovement> q = em.createQuery(jpql, InventoryMovement.class)
                .setParameter("from", fromInclusive)
                .setParameter("to", toInclusive)
                .setParameter("cid", categoryId)
                .setParameter("pid", productId);
        if (maxRows > 0) {
            q.setMaxResults(maxRows);
        }
        return q.getResultList();
    }
}
