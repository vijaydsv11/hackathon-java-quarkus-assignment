
package com.fulfilment.application.monolith.stores.infrastructure.persistence;

import com.fulfilment.application.monolith.stores.domain.model.Store;
import com.fulfilment.application.monolith.stores.domain.port.StoreRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class StoreRepositoryImpl implements StoreRepository {

    @Inject EntityManager em;

    @Override
    public Store save(Store store) {

        DbStore entity;

        if (store.getId() != null) {
            entity = DbStore.findById(store.getId());
            if (entity == null) {
                throw new IllegalArgumentException("Store not found");
            }
        } else {
            entity = new DbStore();
        }

        entity.name = store.getName();
        entity.quantityProductsInStock = store.getQuantityProductsInStock();

        entity.persist();

        return new Store(entity.id, entity.name, entity.quantityProductsInStock);
    }
    

    @Override
    public Store findById(Long id) {
        DbStore db = em.find(DbStore.class, id);
        if (db == null) return null;
        return new Store(db.id, db.name, db.quantityProductsInStock);
    }

    @Override
    public List<Store> findAll() {
        return em.createQuery("from DbStore", DbStore.class)
                .getResultList()
                .stream()
                .map(db -> new Store(db.id, db.name, db.quantityProductsInStock))
                .toList();
    }

    @Override
    public void delete(Long id) {
        DbStore db = em.find(DbStore.class, id);
        if (db != null) em.remove(db);
    }
}
