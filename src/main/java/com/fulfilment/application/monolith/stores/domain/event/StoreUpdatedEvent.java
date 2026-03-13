
package com.fulfilment.application.monolith.stores.domain.event;

import com.fulfilment.application.monolith.stores.domain.model.Store;

public class StoreUpdatedEvent {
    private final Store store;

    public StoreUpdatedEvent(Store store) {
        this.store = store;
    }

    public Store getStore() {
        return store;
    }
}
