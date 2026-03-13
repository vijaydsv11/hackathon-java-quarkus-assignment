
package com.fulfilment.application.monolith.stores.application;

import com.fulfilment.application.monolith.stores.domain.model.Store;
import com.fulfilment.application.monolith.stores.domain.port.StoreRepository;
import com.fulfilment.application.monolith.stores.domain.event.StoreUpdatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class UpdateStoreUseCase {

    @Inject StoreRepository repository;
    @Inject Event<StoreUpdatedEvent> event;

    @Transactional
    public Store execute(Long id, Store updated) {
        Store existing = repository.findById(id);

        if (existing == null) {
            throw new WebApplicationException("Store not found", 404);
        }

        existing.update(updated.getName(), updated.getQuantityProductsInStock());

        Store saved = repository.save(existing);

        event.fire(new StoreUpdatedEvent(saved));

        return saved;
    }
}
