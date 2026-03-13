package com.fulfilment.application.monolith.stores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fulfilment.application.monolith.stores.domain.event.StoreCreatedEvent;
import com.fulfilment.application.monolith.stores.domain.event.StoreUpdatedEvent;
import com.fulfilment.application.monolith.stores.domain.model.Store;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class StoreEventObserverTest {

  @Inject
  StoreEventObserver storeEventObserver;

  @InjectMock
  LegacyStoreManagerGateway legacyGateway;

  private Store testStore;

  @BeforeEach
  public void setup() {
    testStore = new Store(1L, "Test Store", 100);
  }

  @Test
  public void testStoreCreatedEventCallsLegacyGateway() {
    Mockito.reset(legacyGateway);

    StoreCreatedEvent event = new StoreCreatedEvent(testStore);
    storeEventObserver.onStoreCreated(event);

    verify(legacyGateway, times(1))
        .createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testStoreUpdatedEventCallsLegacyGateway() {
    Mockito.reset(legacyGateway);

    StoreUpdatedEvent event = new StoreUpdatedEvent(testStore);
    storeEventObserver.onStoreUpdated(event);

    verify(legacyGateway, times(1))
        .updateStoreOnLegacySystem(any(Store.class));
  }
}
