package vn.learnhub.shared.persistence;

import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.*;

@Repository
public class Store {
    @PersistenceContext
    private EntityManager em;

    public EntityManager entityManager() {
        return em;
    }

    public <T> T get(Class<T> type, Object id) {
        T value = em.find(type, id);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return value;
    }

    public <T> T lock(Class<T> type, Object id) {
        T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return value;
    }

    public <T> TypedQuery<T> query(Class<T> type, String jpql, Object... pairs) {
        var q = em.createQuery(jpql, type);
        for (int i = 0; i < pairs.length; i += 2) q.setParameter((String) pairs[i], pairs[i + 1]);
        return q;
    }

    public <T> List<T> list(Class<T> type, String jpql, Object... pairs) {
        return query(type, jpql, pairs).getResultList();
    }

    public <T> Optional<T> first(Class<T> type, String jpql, Object... pairs) {
        return query(type, jpql, pairs).setMaxResults(1).getResultList().stream().findFirst();
    }

    public long count(String jpql, Object... pairs) {
        return query(Long.class, jpql, pairs).getSingleResult();
    }

    public <T> T add(T entity) {
        em.persist(entity);
        return entity;
    }
}
