package com.forecastpro.repository;

import com.forecastpro.entity.User;
import com.forecastpro.entity.UserRole;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

@Stateless
public class UserRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public User save(User user) {
        if (user.getId() == null) {
            em.persist(user);
            return user;
        }
        return em.merge(user);
    }

    public void delete(User user) {
        em.remove(em.contains(user) ? user : em.merge(user));
    }

    public Optional<User> findByUsername(String username) {
        TypedQuery<User> q = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :u", User.class);
        q.setParameter("u", username);
        return q.getResultStream().findFirst();
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    public List<User> findAllOrdered() {
        return em.createQuery("SELECT u FROM User u ORDER BY u.username", User.class)
                .getResultList();
    }

    public long count() {
        return em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
    }

    /** Enabled users with Admin or Sales Manager role (for email alerts). */
    public List<User> findAdminAndSalesManagers() {
        return em.createQuery(
                        "SELECT u FROM User u WHERE u.enabled = true "
                                + "AND (u.role = :admin OR u.role = :manager) ORDER BY u.username",
                        User.class)
                .setParameter("admin", UserRole.ADMIN)
                .setParameter("manager", UserRole.SALES_MANAGER)
                .getResultList();
    }
}
