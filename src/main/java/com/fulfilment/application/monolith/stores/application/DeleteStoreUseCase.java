
package com.fulfilment.application.monolith.stores.application;

import com.fulfilment.application.monolith.stores.domain.port.StoreRepository;
import com.fulfilment.application.monolith.stores.domain.model.Store;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class DeleteStoreUseCase {

    @Inject StoreRepository repository;

    @Transactional
    public void execute(Long id) {
        Store existing = repository.findById(id);

        if (existing == null) {
            throw new WebApplicationException("Store not found", 404);
        }

        repository.delete(id);
    }
}
