
package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.stores.domain.event.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

@ApplicationScoped
public class StoreEventObserver {

    @Inject LegacyStoreManagerGateway legacyGateway;

    public void onStoreCreated(
        @Observes(during = TransactionPhase.AFTER_SUCCESS)
        StoreCreatedEvent event) {

        legacyGateway.createStoreOnLegacySystem(event.getStore());
    }

    public void onStoreUpdated(
        @Observes(during = TransactionPhase.AFTER_SUCCESS)
        StoreUpdatedEvent event) {

        legacyGateway.updateStoreOnLegacySystem(event.getStore());
    }
}
