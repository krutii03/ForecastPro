package com.forecastpro.repository;

import com.forecastpro.entity.StockRequest;
import com.forecastpro.entity.StockRequestStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@Stateless
public class StockRequestRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public StockRequest save(StockRequest r) {
        if (r.getId() == null) {
            em.persist(r);
            return r;
        }
        return em.merge(r);
    }

    public Optional<StockRequest> findById(Long id) {
        return Optional.ofNullable(em.find(StockRequest.class, id));
    }

    public StockRequest findByIdFetched(Long id) {
        List<StockRequest> list = em.createQuery(
                        "SELECT r FROM StockRequest r JOIN FETCH r.product JOIN FETCH r.vendor v "
                                + "LEFT JOIN FETCH v.user WHERE r.id = :id",
                        StockRequest.class)
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * All stock requests, newest first. JOIN FETCH so JSF can read product/vendor after the EJB transaction ends.
     */
    public List<StockRequest> findAll() {
        return em.createQuery(
                        "SELECT s FROM StockRequest s JOIN FETCH s.product JOIN FETCH s.vendor ORDER BY s.requestDate DESC",
                        StockRequest.class)
                .getResultList();
    }

    public List<StockRequest> findByVendorUserId(Long userId) {
        return em.createQuery(
                        "SELECT s FROM StockRequest s JOIN FETCH s.product JOIN FETCH s.vendor v "
                                + "WHERE v.user.id = :uid ORDER BY s.requestDate DESC",
                        StockRequest.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    public long countByVendorUserId(Long userId) {
        Long n = em.createQuery(
                        "SELECT COUNT(r) FROM StockRequest r JOIN r.vendor v WHERE v.user.id = :uid", Long.class)
                .setParameter("uid", userId)
                .getSingleResult();
        return n != null ? n : 0L;
    }

    public long countByVendorUserIdAndStatus(Long userId, StockRequestStatus status) {
        Long n = em.createQuery(
                        "SELECT COUNT(r) FROM StockRequest r JOIN r.vendor v WHERE v.user.id = :uid AND r.status = :st",
                        Long.class)
                .setParameter("uid", userId)
                .setParameter("st", status)
                .getSingleResult();
        return n != null ? n : 0L;
    }
}
