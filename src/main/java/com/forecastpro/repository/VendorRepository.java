package com.forecastpro.repository;

import com.forecastpro.entity.Vendor;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@Stateless
public class VendorRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public Vendor save(Vendor v) {
        if (v.getId() == null) {
            em.persist(v);
            return v;
        }
        return em.merge(v);
    }

    public void delete(Vendor v) {
        em.remove(em.contains(v) ? v : em.merge(v));
    }

    public Optional<Vendor> findById(Long id) {
        return Optional.ofNullable(em.find(Vendor.class, id));
    }

    /**
     * Vendor row linked to a login user ({@code vendors.user_id}).
     */
    public Optional<Vendor> findByUserId(Long userId) {
        List<Vendor> list = em.createQuery(
                        "SELECT v FROM Vendor v WHERE v.user.id = :uid", Vendor.class)
                .setParameter("uid", userId)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Vendor> findAllActiveOrdered() {
        return em.createQuery(
                        "SELECT v FROM Vendor v WHERE UPPER(v.status) = 'ACTIVE' ORDER BY v.name",
                        Vendor.class)
                .getResultList();
    }

    /** {@code SELECT v FROM Vendor v} (see VendorService.findAll). */
    public List<Vendor> findAll() {
        return em.createQuery("SELECT v FROM Vendor v", Vendor.class).getResultList();
    }

    public List<Vendor> findAllOrdered() {
        return em.createQuery("SELECT v FROM Vendor v ORDER BY v.name", Vendor.class)
                .getResultList();
    }
}
