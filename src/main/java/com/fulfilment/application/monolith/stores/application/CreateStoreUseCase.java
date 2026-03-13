
package com.fulfilment.application.monolith.stores.application;

import com.fulfilment.application.monolith.stores.domain.model.Store;
import com.fulfilment.application.monolith.stores.domain.port.StoreRepository;
import com.fulfilment.application.monolith.stores.domain.event.StoreCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CreateStoreUseCase {

    @Inject StoreRepository repository;
    @Inject Event<StoreCreatedEvent> event;

    @Transactional
    public Store execute(Store store) {

        Store saved = repository.save(store);

        if (saved == null) {
            throw new IllegalStateException("Store creation failed");
        }

        event.fire(new StoreCreatedEvent(saved));

        return saved;
    }
}
