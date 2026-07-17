package com.forecastpro.repository;

import com.forecastpro.entity.Sale;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Stateless
public class SaleRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public Sale save(Sale sale) {
        em.persist(sale);
        return sale;
    }

    public void delete(Sale sale) {
        em.remove(em.contains(sale) ? sale : em.merge(sale));
    }

    public BigDecimal sumTotalRevenue() {
        TypedQuery<BigDecimal> q = em.createQuery(
                "SELECT COALESCE(SUM(s.quantitySold * p.price), 0) FROM Sale s JOIN s.product p",
                BigDecimal.class);
        return q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> monthlyRevenueNative() {
        Query q = em.createNativeQuery(
                "SELECT YEAR(s.sale_date), MONTH(s.sale_date), "
                        + "SUM(s.quantity_sold * p.price) "
                        + "FROM sales s JOIN products p ON s.product_id = p.id "
                        + "GROUP BY YEAR(s.sale_date), MONTH(s.sale_date) "
                        + "ORDER BY YEAR(s.sale_date), MONTH(s.sale_date)");
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> categoryRevenueNative() {
        Query q = em.createNativeQuery(
                "SELECT c.name, SUM(s.quantity_sold * p.price) "
                        + "FROM sales s "
                        + "JOIN products p ON s.product_id = p.id "
                        + "JOIN categories c ON p.category_id = c.id "
                        + "GROUP BY c.id, c.name ORDER BY c.name");
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> productRevenueNative() {
        Query q = em.createNativeQuery(
                "SELECT p.id, p.name, SUM(s.quantity_sold * p.price), SUM(s.quantity_sold) "
                        + "FROM sales s JOIN products p ON s.product_id = p.id "
                        + "GROUP BY p.id, p.name ORDER BY p.name");
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> topProductsNative(int limit) {
        Query q = em.createNativeQuery(
                "SELECT p.id, p.name, SUM(s.quantity_sold * p.price), SUM(s.quantity_sold) "
                        + "FROM sales s JOIN products p ON s.product_id = p.id "
                        + "GROUP BY p.id, p.name ORDER BY SUM(s.quantity_sold * p.price) DESC");
        q.setMaxResults(limit);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> lowProductsNative(int limit) {
        Query q = em.createNativeQuery(
                "SELECT p.id, p.name, SUM(s.quantity_sold * p.price), SUM(s.quantity_sold) "
                        + "FROM sales s JOIN products p ON s.product_id = p.id "
                        + "GROUP BY p.id, p.name ORDER BY SUM(s.quantity_sold * p.price) ASC");
        q.setMaxResults(limit);
        return q.getResultList();
    }

    /**
     * Monthly total quantity sold per product (for forecasting), chronological order.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> monthlyQuantityByProductNative(Long productId) {
        Query q = em.createNativeQuery(
                "SELECT YEAR(s.sale_date), MONTH(s.sale_date), SUM(s.quantity_sold) "
                        + "FROM sales s WHERE s.product_id = ?1 "
                        + "GROUP BY YEAR(s.sale_date), MONTH(s.sale_date) "
                        + "ORDER BY YEAR(s.sale_date), MONTH(s.sale_date)");
        q.setParameter(1, productId);
        return q.getResultList();
    }

    public long countSales() {
        return em.createQuery("SELECT COUNT(s) FROM Sale s", Long.class).getSingleResult();
    }

    /**
     * Per product × calendar month: quantity and revenue (qty × unit price at sale time).
     * Optional category and/or product filter (null = all).
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> monthlySalesByProductFiltered(Long categoryId, Long productId,
                                                        LocalDate fromInclusive, LocalDate toInclusive) {
        Query q = em.createNativeQuery(
                "SELECT p.id, p.name, YEAR(s.sale_date), MONTH(s.sale_date), "
                        + "SUM(s.quantity_sold), SUM(s.quantity_sold * p.price) "
                        + "FROM sales s JOIN products p ON s.product_id = p.id "
                        + "WHERE s.sale_date >= ?1 AND s.sale_date <= ?2 "
                        + "AND (?3 IS NULL OR p.category_id = ?3) "
                        + "AND (?4 IS NULL OR p.id = ?4) "
                        + "GROUP BY p.id, p.name, YEAR(s.sale_date), MONTH(s.sale_date) "
                        + "ORDER BY p.name, YEAR(s.sale_date), MONTH(s.sale_date)");
        q.setParameter(1, java.sql.Date.valueOf(fromInclusive));
        q.setParameter(2, java.sql.Date.valueOf(toInclusive));
        q.setParameter(3, categoryId);
        q.setParameter(4, productId);
        return q.getResultList();
    }

    /**
     * Per product totals for a full calendar year (not broken down by month).
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> yearlySalesByProductFiltered(Long categoryId, Long productId, int year) {
        Query q = em.createNativeQuery(
                "SELECT p.id, p.name, SUM(s.quantity_sold), SUM(s.quantity_sold * p.price) "
                        + "FROM sales s JOIN products p ON s.product_id = p.id "
                        + "WHERE YEAR(s.sale_date) = ?1 "
                        + "AND (?2 IS NULL OR p.category_id = ?2) "
                        + "AND (?3 IS NULL OR p.id = ?3) "
                        + "GROUP BY p.id, p.name ORDER BY p.name");
        q.setParameter(1, year);
        q.setParameter(2, categoryId);
        q.setParameter(3, productId);
        return q.getResultList();
    }

    /** Total revenue by month for a date range (trend line). */
    @SuppressWarnings("unchecked")
    public List<Object[]> monthlyRevenueRangeNative(LocalDate fromInclusive, LocalDate toInclusive) {
        Query q = em.createNativeQuery(
                "SELECT YEAR(s.sale_date), MONTH(s.sale_date), "
                        + "SUM(s.quantity_sold * p.price) "
                        + "FROM sales s JOIN products p ON s.product_id = p.id "
                        + "WHERE s.sale_date >= ?1 AND s.sale_date <= ?2 "
                        + "GROUP BY YEAR(s.sale_date), MONTH(s.sale_date) "
                        + "ORDER BY YEAR(s.sale_date), MONTH(s.sale_date)");
        q.setParameter(1, java.sql.Date.valueOf(fromInclusive));
        q.setParameter(2, java.sql.Date.valueOf(toInclusive));
        return q.getResultList();
    }

    /** Total revenue by product for a date range, optional category/product filters. */
    @SuppressWarnings("unchecked")
    public List<Object[]> productRevenueRangeNative(Long categoryId, Long productId,
                                                    LocalDate fromInclusive, LocalDate toInclusive) {
        Query q = em.createNativeQuery(
                "SELECT p.id, p.name, SUM(s.quantity_sold * p.price), SUM(s.quantity_sold) "
                        + "FROM sales s JOIN products p ON s.product_id = p.id "
                        + "WHERE s.sale_date >= ?1 AND s.sale_date <= ?2 "
                        + "AND (?3 IS NULL OR p.category_id = ?3) "
                        + "AND (?4 IS NULL OR p.id = ?4) "
                        + "GROUP BY p.id, p.name ORDER BY p.name");
        q.setParameter(1, java.sql.Date.valueOf(fromInclusive));
        q.setParameter(2, java.sql.Date.valueOf(toInclusive));
        q.setParameter(3, categoryId);
        q.setParameter(4, productId);
        return q.getResultList();
    }

    /** Total revenue by category for a date range. */
    @SuppressWarnings("unchecked")
    public List<Object[]> categoryRevenueRangeNative(LocalDate fromInclusive, LocalDate toInclusive) {
        Query q = em.createNativeQuery(
                "SELECT c.name, SUM(s.quantity_sold * p.price) "
                        + "FROM sales s "
                        + "JOIN products p ON s.product_id = p.id "
                        + "JOIN categories c ON p.category_id = c.id "
                        + "WHERE s.sale_date >= ?1 AND s.sale_date <= ?2 "
                        + "GROUP BY c.id, c.name ORDER BY c.name");
        q.setParameter(1, java.sql.Date.valueOf(fromInclusive));
        q.setParameter(2, java.sql.Date.valueOf(toInclusive));
        return q.getResultList();
    }

    /** Total units sold for one product in a calendar month (for forecast accuracy). */
    public BigDecimal quantitySoldInMonth(Long productId, LocalDate month) {
        LocalDate start = month.withDayOfMonth(1);
        LocalDate end = month.withDayOfMonth(month.lengthOfMonth());
        Object result = em.createNativeQuery(
                        "SELECT COALESCE(SUM(s.quantity_sold), 0) FROM sales s "
                                + "WHERE s.product_id = ?1 AND s.sale_date >= ?2 AND s.sale_date <= ?3")
                .setParameter(1, productId)
                .setParameter(2, java.sql.Date.valueOf(start))
                .setParameter(3, java.sql.Date.valueOf(end))
                .getSingleResult();
        if (result == null) {
            return BigDecimal.ZERO;
        }
        if (result instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(result.toString());
    }

    /**
     * Average units sold per calendar month (1–12) across all years for seasonality analysis.
     * Returns rows: [month_number, avg_units].
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> monthlyAverageUnitsByProduct(Long productId) {
        Query q = em.createNativeQuery(
                "SELECT m, AVG(monthly_qty) AS avg_qty FROM ("
                        + "  SELECT YEAR(s.sale_date) AS y, MONTH(s.sale_date) AS m, SUM(s.quantity_sold) AS monthly_qty "
                        + "  FROM sales s WHERE s.product_id = ?1 "
                        + "  GROUP BY YEAR(s.sale_date), MONTH(s.sale_date)"
                        + ") monthly GROUP BY m ORDER BY m");
        q.setParameter(1, productId);
        return q.getResultList();
    }

    /** Overall average monthly units for one product (all history). */
    public BigDecimal averageMonthlyUnitsForProduct(Long productId) {
        Object result = em.createNativeQuery(
                        "SELECT COALESCE(AVG(monthly_qty), 0) FROM ("
                                + "SELECT SUM(s.quantity_sold) AS monthly_qty "
                                + "FROM sales s WHERE s.product_id = ?1 "
                                + "GROUP BY YEAR(s.sale_date), MONTH(s.sale_date)) t")
                .setParameter(1, productId)
                .getSingleResult();
        if (result == null) {
            return BigDecimal.ZERO;
        }
        if (result instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(result.toString());
    }
}
