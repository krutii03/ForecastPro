package com.forecastpro.repository;

import com.forecastpro.entity.Message;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class MessageRepository {

    @PersistenceContext(unitName = "ForecastProPU")
    private EntityManager em;

    public Message save(Message m) {
        if (m.getId() == null) {
            em.persist(m);
            return m;
        }
        return em.merge(m);
    }

    public Message findById(Long id) {
        return em.find(Message.class, id);
    }

    public List<Message> findAllOrderByCreatedDesc() {
        return em.createQuery(
                        "SELECT m FROM Message m JOIN FETCH m.sender LEFT JOIN FETCH m.recipient "
                                + "ORDER BY m.createdAt DESC",
                        Message.class)
                .getResultList();
    }

    public List<Message> findForUser(Long userId) {
        return em.createQuery(
                        "SELECT m FROM Message m JOIN FETCH m.sender LEFT JOIN FETCH m.recipient "
                                + "WHERE m.sender.id = :uid OR m.recipient.id = :uid "
                                + "ORDER BY m.createdAt DESC",
                        Message.class)
                .setParameter("uid", userId)
                .getResultList();
    }
}
