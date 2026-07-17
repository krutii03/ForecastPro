package com.forecastpro.repository;

import com.forecastpro.entity.Forecast;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Stateless
public class ForecastRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public Forecast save(Forecast forecast) {
        if (forecast.getId() == null) {
            em.persist(forecast);
            return forecast;
        }
        return em.merge(forecast);
    }

    public Optional<Forecast> findLatestForProduct(Long productId) {
        TypedQuery<Forecast> q = em.createQuery(
                "SELECT f FROM Forecast f WHERE f.product.id = :pid ORDER BY f.forecastMonth DESC, f.createdAt DESC",
                Forecast.class);
        q.setParameter("pid", productId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    public List<Forecast> findRecent(int maxResults) {
        return em.createQuery(
                "SELECT f FROM Forecast f ORDER BY f.forecastMonth DESC, f.createdAt DESC", Forecast.class)
                .setMaxResults(maxResults)
                .getResultList();
    }

    public boolean existsByProductAndMonth(Long productId, LocalDate forecastMonth) {
        Long count = em.createQuery(
                        "SELECT COUNT(f) FROM Forecast f WHERE f.product.id = :pid AND f.forecastMonth = :m",
                        Long.class)
                .setParameter("pid", productId)
                .setParameter("m", forecastMonth)
                .getSingleResult();
        return count != null && count > 0;
    }

    public Optional<Forecast> findByProductAndMonth(Long productId, LocalDate forecastMonth) {
        TypedQuery<Forecast> q = em.createQuery(
                "SELECT f FROM Forecast f WHERE f.product.id = :pid AND f.forecastMonth = :m ORDER BY f.createdAt DESC",
                Forecast.class);
        q.setParameter("pid", productId);
        q.setParameter("m", forecastMonth);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    public List<Forecast> findByFilters(LocalDate fromInclusive, LocalDate toInclusive,
                                        Long categoryId, Long productId, int maxRows) {
        String jpql = "SELECT f FROM Forecast f JOIN FETCH f.product p JOIN FETCH p.category c "
                + "WHERE f.forecastMonth >= :from AND f.forecastMonth <= :to "
                + "AND (:cid IS NULL OR c.id = :cid) "
                + "AND (:pid IS NULL OR p.id = :pid) "
                + "ORDER BY f.forecastMonth DESC, f.createdAt DESC";
        TypedQuery<Forecast> q = em.createQuery(jpql, Forecast.class)
                .setParameter("from", fromInclusive)
                .setParameter("to", toInclusive)
                .setParameter("cid", categoryId)
                .setParameter("pid", productId);
        if (maxRows > 0) {
            q.setMaxResults(maxRows);
        }
        return q.getResultList();
    }

    public void deleteByProductId(Long productId) {
        em.createQuery("DELETE FROM Forecast f WHERE f.product.id = :pid")
                .setParameter("pid", productId)
                .executeUpdate();
    }

    public long countAll() {
        return em.createQuery("SELECT COUNT(f) FROM Forecast f", Long.class).getSingleResult();
    }

    /**
     * Sum of {@code predicted_revenue} for each product's latest forecast (by max id).
     */
    @SuppressWarnings("unchecked")
    public BigDecimal sumPredictedRevenueLatestPerProduct() {
        Query q = em.createNativeQuery(
                "SELECT COALESCE(SUM(f.predicted_revenue), 0) FROM forecasts f "
                        + "INNER JOIN (SELECT product_id, MAX(id) AS mid FROM forecasts GROUP BY product_id) t "
                        + "ON f.id = t.mid");
        Object o = q.getSingleResult();
        return toBigDecimal(o);
    }

    /**
     * Latest forecast per product, ordered by predicted revenue descending.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> latestForecastRevenueRows(int limit) {
        Query q = em.createNativeQuery(
                "SELECT p.name, f.predicted_revenue, f.forecast_month FROM forecasts f "
                        + "JOIN products p ON p.id = f.product_id "
                        + "JOIN (SELECT product_id, MAX(id) AS mid FROM forecasts GROUP BY product_id) t ON f.id = t.mid "
                        + "ORDER BY f.predicted_revenue DESC");
        q.setMaxResults(limit);
        return new ArrayList<>((List<Object[]>) q.getResultList());
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        return new BigDecimal(v.toString());
    }
}
